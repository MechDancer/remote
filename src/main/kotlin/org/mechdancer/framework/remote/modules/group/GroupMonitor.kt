package org.mechdancer.framework.remote.modules.group

import org.mechdancer.framework.dependency.AbstractDependent
import org.mechdancer.framework.remote.modules.multicast.MulticastBroadcaster
import org.mechdancer.framework.remote.modules.multicast.MulticastListener
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.Group
import org.mechdancer.framework.remote.resources.UdpCmd

/**
 * 组成员的管理器
 *   发现新成员时会自动调用函数 [detected]
 *   从未出现过的成员或离线时间超过超时时间 [timeout] 的成员出现时视作新成员
 */
class GroupMonitor(
    private val detected: (String) -> Unit = {},
    private val timeout: Int = Int.MAX_VALUE
) : AbstractDependent<GroupMonitor>(GroupMonitor::class),
    MulticastListener {
    private val update by must { it: Group -> it::detect }
    private val broadcaster by maybe<MulticastBroadcaster>()

    /** 请求组中的成员响应，以证实存在性，要使用此功能必须依赖组播发送 */
    fun yell() = broadcaster!!.broadcast(UdpCmd.YELL_ASK)

    override val interest = INTEREST

    override fun process(remotePacket: RemotePacket) {
        val (name, cmd) = remotePacket

        if (name.isNotBlank()) // 只保存非匿名对象
            update(name)
                ?.takeUnless { System.currentTimeMillis() - it > timeout }
                ?: detected(name)

        if (cmd == UdpCmd.YELL_ASK.id) // 回应询问
            broadcaster?.broadcast(UdpCmd.YELL_ACK)
    }

    private companion object {
        val INTEREST = setOf<Byte>()
    }
}
