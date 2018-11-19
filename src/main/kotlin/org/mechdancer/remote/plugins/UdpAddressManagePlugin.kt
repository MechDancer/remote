package org.mechdancer.remote.plugins

import org.mechdancer.remote.core.AddressManager
import org.mechdancer.remote.core.RemoteHub
import org.mechdancer.remote.core.RemotePlugin
import org.mechdancer.remote.core.internal.Command
import org.mechdancer.remote.core.internal.Command.Companion.memoryOf
import org.mechdancer.remote.core.protocol.bytes

class UdpAddressManagePlugin : RemotePlugin('A') {
    private var host: RemoteHub? = null

    override fun onSetup(host: RemoteHub) {
        this.host = host
    }

    override fun onTeardown() {
        host = null
    }

    val addressManager = AddressManager {
        host?.broadcast(id, Cmd.Ask.lead(it.toByteArray()))
    }

    override fun onBroadcast(sender: String, payload: ByteArray) {
        val temp = host ?: return
        if (Cmd[payload[0]] == Cmd.Ask && String(payload, 1, payload.size - 1) == temp.name)
            temp.broadcast(id, Cmd.Ack.lead(temp.address.bytes))
    }

    private enum class Cmd(override val id: Byte) : Command {
        Ask(0), Ack(1);

        companion object {
            private val memory = memoryOf<Cmd>()
            operator fun get(id: Byte) = memory[id]
        }
    }
}