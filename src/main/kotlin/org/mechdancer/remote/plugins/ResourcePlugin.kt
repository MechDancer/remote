package org.mechdancer.remote.plugins

import org.mechdancer.remote.core.RemoteHub
import org.mechdancer.remote.core.RemotePlugin
import org.mechdancer.remote.core.internal.Command
import org.mechdancer.remote.core.internal.Command.Companion.memoryOf
import org.mechdancer.remote.core.protocol.readWithLength
import org.mechdancer.remote.core.protocol.writeWithLength
import org.mechdancer.remote.plugins.ResourcePlugin.Cmd.ResourceAck
import org.mechdancer.remote.plugins.ResourcePlugin.Cmd.ResourceAsk
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.*

class ResourcePlugin(
    private val retryPeriod: Long,
    vararg resources: Pair<String, ByteArray>
) : RemotePlugin('R') {

    //工作线程池
    private val worker = Executors.newFixedThreadPool(3)

    private val resource = RemoteResource(resources)

    //主机引用
    private lateinit var host: RemoteHub

    //需要请求的资源
    private val resourceToAsk = LinkedTransferQueue<String>()

    //已经请求的资源
    private val asked = ConcurrentSkipListMap<String, Long>()

    //资源已经请求的次数
    //暂未使用
    private val askedTimes = ConcurrentSkipListMap<String, Int>()

    override fun onSetup(host: RemoteHub) {
        this.host = host
        worker.submit {
            while (true) {
                //有没有需要发的请求？
                ask(resourceToAsk.take())
            }
        }
        worker.submit {
            while (true)
            //发过了，但 retryPeriod 毫秒之后没人回，再发一次
                asked.forEach { id, stamp ->
                    if (System.currentTimeMillis() - stamp > retryPeriod)
                        ask(id)
                }
        }
    }

    override fun onBroadcast(sender: String, payload: ByteArray) {
        val (cmd, data) = decodePayload(payload)
        when (cmd) {
            ResourceAsk -> onResourceAsk(String(data))
            ResourceAck -> onResourceAck(data)
            null        -> throw IllegalArgumentException()
        }
    }

    override fun onTeardown() {
        worker.shutdown()
    }

    /**
     * 获取资源
     * 未能获取则阻塞 10s ，之后会返回 `null`
     */
    operator fun get(resourceId: String) = getResource(resourceId, 10000)

    /**
     * 获取资源
     * 未能获取则阻塞 [timeout] ，之后会返回 `null`
     */
    fun getResource(resourceId: String, timeout: Long): ByteArray? =
        resource.get(resourceId, timeout)

    private fun ask(resourceId: String) {
        host.broadcast(id, ResourceAsk.lead(resourceId.toByteArray()))
        asked[resourceId] = System.currentTimeMillis()
        askedTimes[resourceId] = askedTimes[resourceId]?.let { it + 1 } ?: 1
    }

    private fun ack(resourceId: String, data: ByteArray) =
        host.broadcast(id, ResourceAck.lead(encodeAck(resourceId, data)))

    private fun askResource(resourceId: String) =
        resourceToAsk.add(resourceId)

    internal val onResourceAsk = { resourceId: String ->
        //有人请求，如果我有就回复
        if (resource.memory.containsKey(resourceId))
            ack(resourceId, resource.memory[resourceId]!!)
    }

    internal val onResourceAck = { payload: ByteArray ->
        val (resourceId, data) = decodeAck(payload)
        //如果内部队列需要这个请求，就存下结果
        if (resourceId in resource.queue) {
            resource.memory[resourceId] = data
            resource.queue.remove(resourceId)
            asked.remove(resourceId)
            askedTimes.remove(resourceId)
        }
    }

    private fun decodePayload(payload: ByteArray) =
        Cmd[payload[0]] to payload.copyOfRange(1, payload.size)

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
        internal val queue = ConcurrentSkipListSet<String>()

        fun get(resourceId: String, timeout: Long): ByteArray? {

            //本地有直接返回
            memory[resourceId]?.let {
                return it
            }
            //加到内部请求队列
            queue.add(resourceId)
            //添加外部请求队列
            askResource(resourceId)
            val start = System.currentTimeMillis()
            //超时返回 null
            while (resourceId in queue) {
                if (System.currentTimeMillis() - start > timeout) {
                    asked.remove(resourceId)
                    askedTimes.remove(resourceId)
                    queue.remove(resourceId)
                    return null
                }
            }
            return memory[resourceId]!!
        }
    }

    private enum class Cmd(override val id: Byte) : Command {
        ResourceAsk(10),
        ResourceAck(11);


        companion object {
            private val memory = memoryOf<Cmd>()
            operator fun get(id: Byte) = memory[id]
        }
    }
}

