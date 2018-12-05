package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.Component
import java.net.Socket

/**
 * 短连接监听者
 */
interface ShortConnectionListener : Component {
    val interest: Byte
    fun process(client: String, socket: Socket)
}
