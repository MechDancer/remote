package org.mechdancer.framework.remote.modules.multicast

import org.mechdancer.framework.dependency.FunctionModule
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.UdpCmd

/**
 * 组播监听者
 */
interface MulticastListener : FunctionModule {
    val interest: Iterable<UdpCmd>
    infix fun process(remotePacket: RemotePacket)
}
