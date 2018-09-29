package org.mechdancer.remote.builder

data class InnerCmdCallbacksDsl(
    var newProcessDetected: String.() -> Unit = {},
    var broadcastReceived: String.(ByteArray) -> Unit = {}
)
