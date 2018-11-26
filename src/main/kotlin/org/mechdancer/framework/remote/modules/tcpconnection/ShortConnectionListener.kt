package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.FunctionModule
import org.mechdancer.framework.remote.resources.TcpCmd
import java.net.Socket

/**
 * 短连接监听者
 */
interface ShortConnectionListener : FunctionModule {
    val interest: Collection<TcpCmd>
    infix fun process(socket: Socket)
}
