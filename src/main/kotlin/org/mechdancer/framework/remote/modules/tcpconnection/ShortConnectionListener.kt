package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.Dependent
import java.net.Socket

/**
 * 短连接监听者
 */
interface ShortConnectionListener : Dependent {
    val interest: Byte
    fun process(client: String, socket: Socket)
}
