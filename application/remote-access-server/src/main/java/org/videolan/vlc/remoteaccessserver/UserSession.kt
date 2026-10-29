package org.videolan.vlc.remoteaccessserver

data class UserSession(
    val id: String,
    // Preserve the stored JSON field name; the value is an epoch timestamp in milliseconds.
    var maxAge: Long,
)
