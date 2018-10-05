package org.mechdancer.remote.builder

import org.mechdancer.remote.core.BroadcastServer

data class InnerCmdCallbacksDsl(
    var newProcessDetected: String.() -> Unit = {},
    var broadcastReceived: BroadcastServer.(String, ByteArray) -> Unit = { _, _ -> }
)
