package org.mechdancer.version2.remote.functions

import org.mechdancer.remote.core.protocol.RemotePacket
import org.mechdancer.remote.core.protocol.zigzag
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.get
import org.mechdancer.version2.hashOf
import org.mechdancer.version2.must
import org.mechdancer.version2.remote.resources.UdpCmd.PACKET_SLICE
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 数据包分片协议
 */
class PacketSlicer(
    private val size: Int = 65536,
    private val timeout: Int = 10000
) :
    AbstractModule(), MulticastListener {

    init {
        assert(size in 16..65536)
    }

    // 发送

    private val broadcaster by lazy { host.must<MulticastBroadcaster>() }
    private val sequence = AtomicLong(1) // 必须从 1 开始！0 用于指示最后一包！

    // 接收

    private val buffers = ConcurrentHashMap<PackInfo, Buffer>()
    private val callbacks = mutableSetOf<MulticastListener>()

    override fun sync() {
        callbacks.addAll(host.get())
        callbacks.remove(this)
    }

    /**
     * 使用拆包协议广播一包
     * @param remotePacket 数据包
     */
    infix fun broadcast(remotePacket: RemotePacket) {
        val stream = SimpleInputStream(remotePacket.bytes)
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
        }?.let { whole ->
            callbacks.forEach { it process RemotePacket(whole) }
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
        private var time = 0L
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

    private class SimpleOutputStream(size: Int) : OutputStream() {
        val core = ByteArray(size)
        var ptr = 0

        override infix fun write(b: Int) {
            core[ptr++] = b.toByte()
        }

        override infix fun write(byteArray: ByteArray) {
            byteArray.copyInto(core, ptr)
            ptr += byteArray.size
        }

        fun writeLength(byteArray: ByteArray, begin: Int, length: Int) {
            byteArray.copyInto(core, ptr, begin, begin + length)
            ptr += length
        }

        fun writeRange(byteArray: ByteArray, begin: Int, end: Int) {
            byteArray.copyInto(core, ptr, begin, end)
            ptr += end - begin
        }

        fun writeFrom(stream: SimpleInputStream, length: Int) {
            stream.readInto(this, length)
        }

        override fun close() {
            ptr = core.size
        }
    }

    private class SimpleInputStream(val core: ByteArray) : InputStream() {
        private var ptr = 0

        override fun available() = core.size - ptr

        override fun read() =
            if (ptr < core.size)
                core[ptr++].let { if (it >= 0) it.toInt() else it + 256 }
            else -1

        fun look() = core[ptr]

        fun skip(length: Int) = also { ptr += length }

        fun lookRest(): ByteArray {
            val result = ByteArray(core.size - ptr)
            core.copyInto(result, 0, ptr, core.size)
            return result
        }

        fun readInto(stream: SimpleOutputStream, length: Int) {
            stream.writeLength(core, ptr, length)
            ptr += length
        }

        override fun close() {
            ptr = core.size
        }
    }

    private companion object {
        val TYPE_HASH = hashOf<PacketSlicer>()

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