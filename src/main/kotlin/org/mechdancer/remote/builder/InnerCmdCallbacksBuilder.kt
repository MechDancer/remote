package org.mechdancer.remote.builder

import org.mechdancer.remote.core.BroadcastHub

/**
 * 消息回调 Dsl 建造者
 */
data class InnerCmdCallbacksBuilder(
    var newProcessDetected: String.() -> Unit = {},
    var broadcastReceived: BroadcastHub.(String, ByteArray) -> Unit = { _, _ -> }
)
