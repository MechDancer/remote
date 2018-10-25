package org.mechdancer.remote.builder

import org.mechdancer.remote.core.RemoteHub
import java.net.NetworkInterface

/**
 * 消息回调 Dsl 建造者
 */
class RemoteCallbackBuilder {
    var netFilter: (NetworkInterface) -> Boolean = { true }
    var newProcessDetected: String.() -> Unit = {}
    var broadcastReceived: RemoteHub.(String, ByteArray) -> Unit = { _, _ -> }
    var remoteProcess: RemoteHub.(String, ByteArray) -> ByteArray = { _, _ -> ByteArray(0) }
}
