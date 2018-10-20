package org.mechdancer.remote.builder

import org.mechdancer.remote.core.BroadcastHub
import java.net.NetworkInterface

/**
 * 消息回调 Dsl 建造者
 */
data class InnerCmdCallbacksBuilder(
    var netFilter: (NetworkInterface) -> Boolean = { true },
    var newProcessDetected: String.() -> Unit = {},
    var broadcastReceived: BroadcastHub.(String, ByteArray) -> Unit = { _, _ -> },
    var remoteProcess: (ByteArray) -> ByteArray = { ByteArray(0) }
)
