package org.mechdancer.remote.core.plugin

import org.mechdancer.remote.core.RemoteHub
import org.mechdancer.remote.core.protocol.readWithLength
import org.mechdancer.remote.core.protocol.writeWithLength
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

class ResourcePlugin : BroadcastPlugin {

    override val key: RemotePlugin.Key<*> = ResourcePlugin

    val resource = RemoteResource()

    private val resourceToAsk = ConcurrentSkipListSet<String>()

    override fun invoke(host: RemoteHub, guest: String, payload: ByteArray) {
        val (cmd, data) = decodePayload(payload)
        when (cmd) {
            Cmd.ResourceAsk -> onResourceAsk(host, String(data))
            Cmd.ResourceAck -> onResourceAck(payload)
        }
        resourceToAsk.forEach {
            host.ask(it)
            resourceToAsk.remove(it)
        }
    }

    private fun RemoteHub.ask(resourceId: String) {
        broadcast(Cmd.ResourceAsk join resourceId.toByteArray())
    }

    private fun RemoteHub.ack(resourceId: String, data: ByteArray) {
        broadcast(Cmd.ResourceAck join encodeAck(resourceId, data))
    }


    private fun askResource(resourceId: String) {
        resourceToAsk += resourceId
    }

    internal val onResourceAsk = { hub: RemoteHub, resourceId: String ->
        if (resourceId in resource.memory)
            hub.ack(resourceId, resource.memory[resourceId]!!)
    }
    internal val onResourceAck = { payload: ByteArray ->
        val (resourceId, data) = decodeAck(payload)
        if (resourceId in resource.queue) {
            resource.memory[resourceId] = data
            resource.queue -= resourceId
        }
    }

    private fun decodePayload(payload: ByteArray) =
        Cmd.fromId(payload[0]) to payload.copyOfRange(1, payload.size - 1)

    private fun encodeAck(resourceId: String, data: ByteArray) =
        ByteArrayOutputStream().also {
            DataOutputStream(it).apply {
                writeWithLength(resourceId.toByteArray())
                write(data)
            }
        }.toByteArray()

    private fun decodeAck(data: ByteArray) =
        DataInputStream(ByteArrayInputStream(data)).run {
            String(readWithLength()) to readBytes()
        }


    inner class RemoteResource {

        internal val memory = ConcurrentHashMap<String, ByteArray>()
        internal val queue = ConcurrentSkipListSet<String>()

        operator fun get(resourceId: String): ByteArray {
            if (resourceId in memory)
                return memory[resourceId]!!
            else queue += resourceId
            askResource(resourceId)
            while (resourceId in queue);
            return memory[resourceId]!!
        }
    }

    private enum class Cmd(private val id: Byte) {
        ResourceAsk(10),
        ResourceAck(11);

        infix fun join(payload: ByteArray) = byteArrayOf(id) + payload

        companion object {
            fun fromId(id: Byte) = when (id) {
                10.toByte() -> ResourceAsk
                11.toByte() -> ResourceAck
                else        -> throw IllegalArgumentException()
            }
        }
    }

    companion object : RemotePlugin.Key<ResourcePlugin> {
        override val id: Char = '0'
    }
}