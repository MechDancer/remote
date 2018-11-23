package org.mechdancer.version2.remote.functions

import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.dependency.get
import org.mechdancer.version2.dependency.hashOf
import org.mechdancer.version2.dependency.must
import org.mechdancer.version2.remote.functions.multicast.MulticastBroadcaster
import org.mechdancer.version2.remote.functions.multicast.MulticastListener
import org.mechdancer.version2.remote.protocol.RemotePacket
import org.mechdancer.version2.remote.protocol.zigzag
import org.mechdancer.version2.remote.resources.Command
import org.mechdancer.version2.remote.resources.UdpCmd
import org.mechdancer.version2.remote.resources.UdpCmd.PACKET_SLICE
import org.mechdancer.version2.remote.streams.SimpleInputStream
import org.mechdancer.version2.remote.streams.SimpleOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 数据包分片协议
 */
class PacketSlicer(
    private val size: Int = 65536,
    private val timeout: Int = 10000
) : AbstractModule(), MulticastListener {

    init {
        assert(size in 16..65536)
    }

    // 发送

    private val broadcaster by must<MulticastBroadcaster> { host }
    private val sequence = AtomicLong(1) // 必须从 1 开始！0 用于指示最后一包！

    // 接收

    private val buffers = ConcurrentHashMap<PackInfo, Buffer>()
    private val callbacks = mutableSetOf<MulticastListener>()

    override fun sync() {
        callbacks.addAll(host.get())
        callbacks.remove(this)
    }

    override val interest = INTEREST

    /**
     * 使用拆包协议广播一包
     */
    fun broadcast(cmd: Command, payload: ByteArray) {
        val stream = RemotePacket(cmd.id, "", 0, payload).bytes.let(::SimpleInputStream)
        val s = sequence.getAndIncrement() zigzag false
        var index = 0L   // 包序号
        while (true) {
            val i = index++ zigzag false
            // 如果是最后一包，应该多长?
            val last = stream.available() + s.size + 1 + i.size
            // 确实是最后一包！
            if (last <= size) {
                SimpleOutputStream(last)
                    .apply {
                        write(0) // 空一位作为停止位
                        write(s)
                        write(i)
                        writeFrom(stream, stream.available())
                    }
                    .core
                    .let { broadcaster.broadcast(PACKET_SLICE, it) }
                return
            } else {
                val length = size - s.size - i.size
                SimpleOutputStream(size)
                    .apply {
                        write(s)
                        write(i)
                        writeFrom(stream, length)
                    }
                    .core
                    .let { broadcaster.broadcast(PACKET_SLICE, it) }
            }
        }
    }

    override fun process(remotePacket: RemotePacket) {
        val (id, name, _, payload) = remotePacket
        if (id != PACKET_SLICE.id) return

        val stream = SimpleInputStream(payload)   // 构造流
        val last = stream.look() == 0.toByte()    // 判断停止位
        if (last) stream.skip(1)                  // 跳过停止位
        val subSeq = stream zigzag false          // 解子包序列号
        val index = (stream zigzag false).toInt() // 解子包序号
        val rest = stream.lookRest()              // 解子包负载

        when {
            index == 0 && last -> rest // 这是第一包也是最后一包 => 只有一包 => 不进缓存
            else               ->      // 否则
                PackInfo(name, subSeq).let { key ->
                    buffers
                        .computeIfAbsent(key) { Buffer() }
                        .put(last, index, rest)
                        ?.also { buffers.remove(key) }
                }
        }
            ?.let(::SimpleInputStream)
            ?.let { RemotePacket(it.look(), name, subSeq, it.skip(3).lookRest()) }
            ?.let { pack ->
                callbacks
                    .filter { UdpCmd[pack.command] in it.interest }
                    .forEach { it process pack }
            }
    }

    /**
     * 清理缓冲区
     */
    fun refresh() {
        val now = System.currentTimeMillis()
        buffers // 删除超时包
            .filterValues { it by now > timeout }
            .keys.forEach { buffers.remove(it) }
    }

    override fun equals(other: Any?) = other is PacketSlicer
    override fun hashCode() = TYPE_HASH

    /**
     * 关键信息
     */
    private data class PackInfo(val name: String, val seq: Long)

    /**
     * 子包缓存
     */
    private class Buffer {
        private var time = System.currentTimeMillis()
        private val list = mutableListOf<ByteArray?>()
        private val mark = hashSetOf<Int>()
        private var done = false

        /**
         * @return 最后活跃时间到当前的延时
         */
        infix fun by(now: Long) = now - time

        /**
         * 置入一个小包
         * @param last    末包标记
         * @param index   序号
         * @param payload 负载
         *
         * @return 已完结则返回完整包
         */
        fun put(
            last: Boolean,
            index: Int,
            payload: ByteArray
        ): ByteArray? {
            fun update() {
                list[index] = payload
                mark.remove(index)
            }

            // 修改状态，加锁保护
            synchronized(this) {
                if (done) update() else {
                    done = last
                    for (i in list.size until index) {
                        mark.add(i)
                        list.add(null)
                    }
                    if (list.size != index) update()
                    else list.add(payload)
                }
            }

            // 已经保存最后一包并且不缺包
            if (done && mark.isEmpty())
                return SimpleOutputStream(list.sumBy { it!!.size })
                    .apply { for (sub in list) write(sub!!) }
                    .core

            // 更新最后活跃时间
            time = System.currentTimeMillis()
            return null
        }
    }

    private companion object {
        val TYPE_HASH = hashOf<PacketSlicer>()
        val INTEREST = setOf(PACKET_SLICE)

//      @JvmStatic
//      fun main(args: Array<String>) {
//          val length = 2_0000_0000
//          val buffer0 = Random.nextBytes(length)
//          val buffer1 = ByteArray(length)
//          val buffer2 = ByteArray(length)
//          val buffer3 = ByteArrayOutputStream(length)
//          val buffer4 = SimpleOutputStream(length)
//          measureTimeMillis { buffer0.copyInto(buffer1) }.let(::println)
//          measureTimeMillis { System.arraycopy(buffer0, 0, buffer2, 0, length) }.let(::println)
//          measureTimeMillis { buffer3.write(buffer0) }.let(::println)
//          measureTimeMillis { buffer3.writeBytes(buffer0) }.let(::println) // Java 11 <- 智障！
//          measureTimeMillis { buffer3.toByteArray() }.let(::println)
//          measureTimeMillis { buffer4.write(buffer0) }.let(::println) // <- 引起舒适
//          measureTimeMillis { buffer4.core }.let(::println)           // <- 引起舒适
//      }
    }
}
