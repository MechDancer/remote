package org.mechdancer.remote.core.plugin

import org.mechdancer.remote.core.RemoteHub
import org.mechdancer.remote.core.protocol.readWithLength
import org.mechdancer.remote.core.protocol.writeWithLength
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.*


class ResourcePlugin(vararg resources: Pair<String, ByteArray>) : RemotePlugin(ResourcePlugin) {

    private val resource = RemoteResource(resources)

    private lateinit var master: RemoteHub

    private val resourceToAsk = LinkedTransferQueue<String>()

    private val worker = Executors.newFixedThreadPool(3)

    override fun onSetup(host: RemoteHub) {
        master = host
        worker.submit {
            while (true) {
                ask(resourceToAsk.take())
                println("Yeah")
            }
        }
    }

    override fun invoke(host: RemoteHub, sender: String, payload: ByteArray) {
        println("on broadcast")
        val (cmd, data) = decodePayload(payload)
        when (cmd) {
            Cmd.ResourceAsk -> onResourceAsk(String(data))
            Cmd.ResourceAck -> onResourceAck(payload)
            null            -> throw IllegalArgumentException()
        }
    }

    override fun onTeardown() {
        worker.shutdown()
    }

    /**
     * 获取资源
     * 未能获取则阻塞 3s ，之后会返回 `null`
     */
    operator fun get(resourceId: String) = getResource(resourceId, 3000)

    /**
     * 获取资源
     * 未能获取则阻塞 [timeout] ，之后会返回 `null`
     */
    fun getResource(resourceId: String, timeout: Long): ByteArray? =
        resource.get(resourceId, timeout)

    private fun ask(resourceId: String) =
        master.broadcast(key, Cmd.ResourceAsk join resourceId.toByteArray())

    private fun ack(resourceId: String, data: ByteArray) =
        master.broadcast(key, Cmd.ResourceAck join encodeAck(resourceId, data))


    private fun askResource(resourceId: String) =
        resourceToAsk.add(resourceId)

    internal val onResourceAsk = { resourceId: String ->
        println("resource ask: $resourceId")
        if (resourceId in resource.memory)
            ack(resourceId, resource.memory[resourceId]!!)
    }

    internal val onResourceAck = { payload: ByteArray ->
        val (resourceId, data) = decodeAck(payload)
        println("resource ack: $resourceId")
        if (resourceId in resource.queue) {
            resource.memory[resourceId] = data
            resource.queue -= resourceId
        }
    }

    private fun decodePayload(payload: ByteArray) =
        Cmd(payload[0]) to payload.copyOfRange(1, payload.lastIndex)

    private fun encodeAck(resourceId: String, data: ByteArray) =
        ByteArrayOutputStream().apply {
            writeWithLength(resourceId.toByteArray())
            write(data)
        }.toByteArray()

    private fun decodeAck(data: ByteArray) =
        ByteArrayInputStream(data).run {
            String(readWithLength()) to readBytes()
        }

    internal inner class RemoteResource(resources: Array<out Pair<String, ByteArray>>) {

        inner class RequestTask(val resourceId: String, timeOutMilliseconds: Long) : Delayed {

            private val time = timeOutMilliseconds + System.currentTimeMillis()

            override fun compareTo(other: Delayed): Int =
                (this.getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS)).toInt()


            override fun getDelay(unit: TimeUnit): Long =
                unit.convert(time - System.currentTimeMillis(), TimeUnit.MILLISECONDS)

        }

        internal val memory = ConcurrentHashMap<String, ByteArray>(resources.toMap())
        internal val queue = LinkedTransferQueue<String>()

        init {
            worker.submit {
                while (true)
                    askResource(queue.take())
            }
        }

        fun get(resourceId: String, timeout: Long): ByteArray? {
            if (resourceId in memory)
                return memory[resourceId]!!
            else queue += resourceId
            val start = System.currentTimeMillis()
            while (resourceId in queue) {
                if (System.currentTimeMillis() - start > timeout)
                    return null
            }
            return memory[resourceId]!!
        }
    }

    private enum class Cmd(private val id: Byte) {
        ResourceAsk(10),
        ResourceAck(11);

        infix fun join(payload: ByteArray) =
            ByteArray(payload.size + 1)
                .apply {
                    set(0, id)
                    payload.copyInto(this, 1)
                }

        companion object {
            operator fun invoke(id: Byte) =
                values().firstOrNull { it.id == id }
        }
    }

    companion object : RemotePlugin.Key<ResourcePlugin> {
        override val id: Char = '0'
    }
}