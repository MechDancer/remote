package org.mechdancer.framework.remote.modules.multicast

import org.mechdancer.framework.dependency.Dependent
import org.mechdancer.framework.remote.protocol.RemotePacket

/**
 * 组播监听者
 */
interface MulticastListener : Dependent {
    val interest: Collection<Byte>
    infix fun process(remotePacket: RemotePacket)
}
