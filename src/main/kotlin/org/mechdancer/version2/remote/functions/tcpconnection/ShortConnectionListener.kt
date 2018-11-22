package org.mechdancer.version2.remote.functions.tcpconnection

import org.mechdancer.version2.dependency.FunctionModule
import org.mechdancer.version2.remote.resources.TcpCmd
import java.net.Socket

/**
 * 短连接监听者
 */
interface ShortConnectionListener : FunctionModule {
    val interest: Collection<TcpCmd>
    infix fun process(socket: Socket)
}
