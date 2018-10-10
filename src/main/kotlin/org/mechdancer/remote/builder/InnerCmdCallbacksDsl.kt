package org.mechdancer.remote.builder

import org.mechdancer.remote.core.BroadcastHub

data class InnerCmdCallbacksDsl(
    var newProcessDetected: String.() -> Unit = {},
    var broadcastReceived: BroadcastHub.(String, ByteArray) -> Unit = { _, _ -> }
)
