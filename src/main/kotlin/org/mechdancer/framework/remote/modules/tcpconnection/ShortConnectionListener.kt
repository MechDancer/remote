package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.FunctionModule
import java.net.Socket

/**
 * 短连接监听者
 */
interface ShortConnectionListener : FunctionModule {
    val interest: Collection<Byte>
    infix fun process(socket: Socket)
}
