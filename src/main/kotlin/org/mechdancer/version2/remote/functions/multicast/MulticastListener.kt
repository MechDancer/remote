package org.mechdancer.version2.remote.functions.multicast

import org.mechdancer.remote.core.protocol.RemotePacket
import org.mechdancer.version2.dependency.FunctionModule
import org.mechdancer.version2.remote.resources.UdpCmd

interface MulticastListener : FunctionModule {
    val interest: Iterable<UdpCmd>
    infix fun process(remotePacket: RemotePacket)
}
