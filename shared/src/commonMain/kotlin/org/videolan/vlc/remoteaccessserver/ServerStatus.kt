package org.videolan.vlc.remoteaccessserver

/**
 * Describe the sharing server status.
 *
 * Shared across all platforms so that the remote access server and any
 * remote control client (iOS, web) speak the same protocol.
 */
enum class ServerStatus {
    NOT_INIT, CONNECTING, STOPPING, STARTED, STOPPED, ERROR
}
