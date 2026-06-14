package org.videolan.vlc.platform

actual object PlatformInfoProvider {
    actual val current: PlatformInfo = PlatformInfo(
        platform = Platform.JVM,
        osVersion = System.getProperty("os.name") + " " + System.getProperty("os.version"),
        deviceModel = System.getProperty("java.vm.name") ?: "JVM"
    )
}

actual object VlcLogger {
    actual fun d(tag: String, message: String) {
        println("D/$tag: $message")
    }

    actual fun i(tag: String, message: String) {
        println("I/$tag: $message")
    }

    actual fun w(tag: String, message: String, throwable: Throwable?) {
        println("W/$tag: $message ${throwable?.stackTraceToString() ?: ""}")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        println("E/$tag: $message ${throwable?.stackTraceToString() ?: ""}")
    }
}
