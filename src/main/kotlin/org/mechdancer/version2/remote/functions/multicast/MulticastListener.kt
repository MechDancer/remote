package org.mechdancer.version2.remote.functions.multicast

import org.mechdancer.remote.core.protocol.RemotePacket
import org.mechdancer.version2.dependency.FunctionModule
import org.mechdancer.version2.remote.resources.UdpCmd

/**
 * 组播监听者
 */
interface MulticastListener : FunctionModule {
    val interest: Iterable<UdpCmd>
    infix fun process(remotePacket: RemotePacket)
}
