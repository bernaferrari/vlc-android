//
//  IosWifiSharingBridge.swift
//  VLC-iOS
//
//  Authenticated, local-network file intake. The shared Compose settings page
//  owns the preference and status; this file owns sockets, network permission,
//  and Files/Documents lifecycle on iOS.
//

import Darwin
import Foundation
import Network
import VLCShared

final class IosWifiSharingBridge: NSObject, IosRemoteAccessHandler {
    static let shared = IosWifiSharingBridge()

    func setEnabled(enabled: Bool) {
        Task { @MainActor in
            if enabled {
                IosWifiSharingServer.shared.start()
            } else {
                IosWifiSharingServer.shared.stop()
            }
        }
    }
}

/**
 * Local-only HTTP upload endpoint. There is intentionally no iOS-specific VLC
 * web UI: clients upload raw bytes to the displayed, bearer-token URL and the
 * result goes through the same durable Documents import path as Files/Photos.
 */
@MainActor
private final class IosWifiSharingServer {
    static let shared = IosWifiSharingServer()

    private let queue = DispatchQueue(label: "org.videolan.vlc.wifi-sharing", qos: .utility)
    private var listener: NWListener?
    private var token: String?
    private var sessions: [UUID: WifiUploadSession] = [:]

    func start() {
        guard listener == nil else { return }
        let token = UUID().uuidString.lowercased()
        do {
            let listener = try NWListener(using: .tcp)
            listener.stateUpdateHandler = { [weak self] state in
                Task { @MainActor in self?.handle(state) }
            }
            listener.newConnectionHandler = { [weak self] connection in
                Task { @MainActor in self?.accept(connection) }
            }
            self.token = token
            self.listener = listener
            listener.start(queue: queue)
        } catch {
            publishFailure("Could not start the local transfer server: \(error.localizedDescription)")
        }
    }

    func stop() {
        listener?.cancel()
        listener = nil
        token = nil
        sessions.values.forEach { $0.cancel() }
        sessions.removeAll()
    }

    private func handle(_ state: NWListener.State) {
        switch state {
        case .ready:
            guard let listener, let port = listener.port else {
                publishFailure("The local transfer server did not receive a port.")
                return
            }
            guard let address = LocalWifiAddress.current else {
                publishFailure("Connect to Wi-Fi before enabling local transfer.")
                stop()
                return
            }
            // The UUID is a 128-bit bearer secret. Do not accept an unauthenticated
            // request merely because it originates from the LAN.
            let url = "http://\(address):\(port)/upload?token=\(token ?? "")"
            IosRemoteAccessServer.shared.publishRunning(address: url)
        case .failed(let error):
            listener = nil
            publishFailure("Local transfer stopped: \(error.localizedDescription)")
        case .waiting(let error):
            publishFailure("Local transfer is waiting for the network: \(error.localizedDescription)")
        case .cancelled:
            break
        default:
            break
        }
    }

    private func accept(_ connection: NWConnection) {
        guard let token else {
            connection.cancel()
            return
        }
        let id = UUID()
        let session = WifiUploadSession(connection: connection, token: token) { [weak self] fileURL in
            DispatchQueue.main.async {
                guard let self else { return }
                self.sessions.removeValue(forKey: id)
                guard let fileURL else { return }
                // The uploader writes inside Documents, so this goes through the
                // identical durable KMP catalog + AVFoundation enrichment path.
                _ = MediaImporter.shared.importIncomingURL(fileURL)
            }
        }
        sessions[id] = session
        session.start(on: queue)
    }

    private func publishFailure(_ message: String) {
        IosRemoteAccessServer.shared.publishFailure(message: message)
    }
}

private enum LocalWifiAddress {
    static var current: String? {
        var head: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&head) == 0, let first = head else { return nil }
        defer { freeifaddrs(first) }

        var cursor: UnsafeMutablePointer<ifaddrs>? = first
        while let interface = cursor?.pointee {
            defer { cursor = interface.ifa_next }
            guard let address = interface.ifa_addr,
                  address.pointee.sa_family == UInt8(AF_INET),
                  String(cString: interface.ifa_name).hasPrefix("en") else { continue }
            var host = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            let result = getnameinfo(
                address,
                socklen_t(address.pointee.sa_len),
                &host,
                socklen_t(host.count),
                nil,
                0,
                NI_NUMERICHOST
            )
            if result == 0 {
                return String(cString: host)
            }
        }
        return nil
    }
}

/** One raw HTTP PUT/POST request, written straight to disk instead of memory. */
private final class WifiUploadSession {
    private static let maxHeaderBytes = 16 * 1024
    private static let maxUploadBytes: Int64 = 20 * 1024 * 1024 * 1024

    private let connection: NWConnection
    private let token: String
    private let finish: (URL?) -> Void
    private var headers = Data()
    private var output: FileHandle?
    private var destination: URL?
    private var remainingBytes: Int64?
    private var didFinish = false

    init(connection: NWConnection, token: String, finish: @escaping (URL?) -> Void) {
        self.connection = connection
        self.token = token
        self.finish = finish
    }

    func start(on queue: DispatchQueue) {
        connection.start(queue: queue)
        receiveNext()
    }

    func cancel() {
        output?.closeFile()
        connection.cancel()
        finishOnce(nil)
    }

    private func receiveNext() {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 64 * 1024) { [weak self] data, _, isComplete, error in
            guard let self else { return }
            if let error {
                self.fail(status: 400, message: "Network error: \(error.localizedDescription)")
                return
            }
            if let data, !data.isEmpty {
                self.consume(data)
            }
            if isComplete, !self.didFinish, self.remainingBytes != 0 {
                self.fail(status: 400, message: "Upload ended before its declared length.")
                return
            }
            if !self.didFinish {
                self.receiveNext()
            }
        }
    }

    private func consume(_ data: Data) {
        if remainingBytes == nil {
            headers.append(data)
            guard let end = headers.range(of: Data([13, 10, 13, 10])) else {
                if headers.count > Self.maxHeaderBytes {
                    fail(status: 431, message: "Request headers are too large.")
                }
                return
            }
            let headerData = headers.subdata(in: 0..<end.lowerBound)
            let body = headers.subdata(in: end.upperBound..<headers.count)
            headers.removeAll(keepingCapacity: false)
            guard beginUpload(headerData) else { return }
            consumeBody(body)
        } else {
            consumeBody(data)
        }
    }

    private func beginUpload(_ data: Data) -> Bool {
        let lines = String(decoding: data, as: UTF8.self).components(separatedBy: "\r\n")
        guard let request = lines.first?.split(separator: " "), request.count >= 2 else {
            fail(status: 400, message: "Malformed HTTP request.")
            return false
        }
        let method = String(request[0])
        let requestTarget = String(request[1])
        let parts = requestTarget.split(separator: "?", maxSplits: 1, omittingEmptySubsequences: false)
        let path = String(parts[0])
        guard path == "/" || path == "/upload" || path.hasPrefix("/upload/") else {
            fail(status: 404, message: "Unknown endpoint.")
            return false
        }

        var fields: [String: String] = [:]
        for line in lines.dropFirst() {
            guard let colon = line.firstIndex(of: ":") else { continue }
            fields[String(line[..<colon]).lowercased()] = String(line[line.index(after: colon)...]).trimmingCharacters(in: .whitespaces)
        }
        let queryToken = parts.count == 2 ? URLComponents(string: "http://localhost/?\(parts[1])")?.queryItems?.first(where: { $0.name == "token" })?.value : nil
        let bearerToken = fields["authorization"]?.replacingOccurrences(of: "Bearer ", with: "")
        guard queryToken == token || bearerToken == token || fields["x-vlc-token"] == token else {
            fail(status: 401, message: "A valid transfer token is required.")
            return false
        }
        if method == "GET", path == "/" || path == "/upload" {
            serveUploadPage()
            return false
        }
        guard method == "PUT" || method == "POST", path == "/upload" || path.hasPrefix("/upload/") else {
            fail(status: 405, message: "Use the upload page, PUT, or POST to upload media.")
            return false
        }
        guard let lengthText = fields["content-length"], let length = Int64(lengthText), length > 0, length <= Self.maxUploadBytes else {
            fail(status: 411, message: "A Content-Length up to 20 GiB is required.")
            return false
        }

        let pathName = path.dropFirst("/upload/".count).removingPercentEncoding
        let name = safeFilename(fields["x-vlc-filename"] ?? pathName ?? "upload.bin")
        do {
            let root = try incomingDirectory()
            let destination = uniqueDestination(named: name, in: root)
            guard FileManager.default.createFile(atPath: destination.path, contents: nil),
                  let output = FileHandle(forWritingAtPath: destination.path) else {
                throw CocoaError(.fileWriteUnknown)
            }
            self.destination = destination
            self.output = output
            self.remainingBytes = length
            return true
        } catch {
            fail(status: 507, message: "Could not create the upload file.")
            return false
        }
    }

    private func consumeBody(_ data: Data) {
        guard let remaining = remainingBytes, let output else { return }
        let count = min(data.count, Int(remaining))
        guard count > 0 else { return }
        do {
            try output.write(contentsOf: data.prefix(count))
        } catch {
            fail(status: 507, message: "Could not save the upload.")
            return
        }
        remainingBytes = remaining - Int64(count)
        if remainingBytes == 0 {
            output.closeFile()
            self.output = nil
            respond(status: 201, message: "Imported into VLC.", importedFile: destination)
        }
    }

    private func fail(status: Int, message: String) {
        output?.closeFile()
        output = nil
        destination.flatMap { try? FileManager.default.removeItem(at: $0) }
        destination = nil
        respond(status: status, message: message, importedFile: nil)
    }

    private func serveUploadPage() {
        // This is deliberately a one-purpose transfer form, not a duplicate VLC
        // product surface. Its only route uploads a selected local file straight
        // into the shared Documents/media-library flow.
        let page = """
        <!doctype html><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>VLC local transfer</title>
        <style>body{font:16px -apple-system,system-ui,sans-serif;max-width:34rem;margin:3rem auto;padding:0 1.25rem;color:#202124}button{margin-top:1rem;padding:.7rem 1rem;background:#ff8800;color:#111;border:0;border-radius:.6rem;font-weight:700}#status{margin-top:1rem;white-space:pre-wrap}</style>
        <h1>Send media to VLC</h1><p>This phone accepts files only while local transfer is enabled in VLC.</p>
        <input id="file" type="file" multiple><br><button id="send">Upload</button><p id="status"></p>
        <script>
        const file=document.querySelector('#file'), status=document.querySelector('#status');
        document.querySelector('#send').onclick=async()=>{for(const item of file.files){status.textContent=`Uploading ${item.name}…`;const r=await fetch('/upload'+location.search,{method:'POST',headers:{'X-VLC-Filename':item.name},body:item});if(!r.ok){status.textContent=`${item.name}: ${await r.text()}`;return}}status.textContent='Imported into VLC.'}
        </script>
        """
        respond(status: 200, message: page, importedFile: nil, contentType: "text/html; charset=utf-8")
    }

    private func respond(
        status: Int,
        message: String,
        importedFile: URL?,
        contentType: String = "text/plain; charset=utf-8",
    ) {
        guard !didFinish else { return }
        let reason = status == 200 ? "OK" : status == 201 ? "Created" : status == 401 ? "Unauthorized" : "Bad Request"
        let response = "HTTP/1.1 \(status) \(reason)\r\nContent-Type: \(contentType)\r\nContent-Length: \(message.utf8.count)\r\nConnection: close\r\n\r\n\(message)"
        connection.send(content: Data(response.utf8), contentContext: .defaultMessage, isComplete: true, completion: .contentProcessed { [weak self] _ in
            self?.connection.cancel()
            self?.finishOnce(importedFile)
        })
    }

    private func finishOnce(_ file: URL?) {
        guard !didFinish else { return }
        didFinish = true
        finish(file)
    }

    private func incomingDirectory() throws -> URL {
        let documents = try FileManager.default.url(
            for: .documentDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )
        let incoming = documents.appendingPathComponent("Incoming", isDirectory: true)
        try FileManager.default.createDirectory(at: incoming, withIntermediateDirectories: true)
        return incoming
    }

    private func safeFilename(_ candidate: String) -> String {
        let basename = URL(fileURLWithPath: candidate).lastPathComponent
        let filtered = basename.unicodeScalars.filter { $0.value >= 32 && $0.value != 127 }.map(String.init).joined()
        return filtered.isEmpty ? "upload.bin" : String(filtered.prefix(180))
    }

    private func uniqueDestination(named name: String, in directory: URL) -> URL {
        let base = URL(fileURLWithPath: name).deletingPathExtension().lastPathComponent
        let ext = URL(fileURLWithPath: name).pathExtension
        var suffix = 1
        var candidate = directory.appendingPathComponent(name)
        while FileManager.default.fileExists(atPath: candidate.path) {
            suffix += 1
            candidate = directory.appendingPathComponent(ext.isEmpty ? "\(base) (\(suffix))" : "\(base) (\(suffix)).\(ext)")
        }
        return candidate
    }
}
