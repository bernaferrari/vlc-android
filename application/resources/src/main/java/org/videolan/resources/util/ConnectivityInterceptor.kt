package org.videolan.resources.util

import java.io.IOException

/** Raised by network-backed resource clients when Android reports no connectivity. */
class NoConnectivityException : IOException() {
    override val message: String = "No connectivity exception"
}
