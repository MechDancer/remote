package org.mechdancer.framework.remote.modules.multicast

import org.mechdancer.framework.dependency.FunctionModule
import org.mechdancer.framework.remote.protocol.RemotePacket

/**
 * 组播监听者
 */
interface MulticastListener : FunctionModule {
    val interest: Iterable<Byte>
    infix fun process(remotePacket: RemotePacket)
}
