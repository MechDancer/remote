package org.mechdancer.version2.remote.functions

import org.mechdancer.remote.core.protocol.RemotePackage
import org.mechdancer.remote.core.protocol.zigzag
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.get
import org.mechdancer.version2.hashOf
import org.mechdancer.version2.must
import org.mechdancer.version2.remote.resources.UdpCmd.PACKET_SLICE
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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

    private val buffer = ConcurrentHashMap<PackInfo, Buffer>()
    private val callbacks = mutableSetOf<MulticastListener>()

    override fun sync() {
        callbacks.addAll(host.get())
        callbacks.remove(this)
    }

    /**
     * 使用拆包协议广播一包
     * @param remotePackage 数据包
     */
    infix fun broadcast(remotePackage: RemotePackage) {
        val payload = remotePackage.bytes
        val s = sequence.getAndIncrement().zigzag(false)
        var index = 0L   // 包序号
        var position = 0 // 流位置
        while (true) {
            val i = index++.zigzag(false)
            // 如果是最后一包，应该多长
            val last = payload.size - position + s.size + 1 + i.size
            // 最后一包
            if (last <= size) {
                val pack = ByteArray(last)
                // 空一位作为停止位
                s.copyInto(destination = pack, destinationOffset = 1)
                i.copyInto(destination = pack, destinationOffset = 1 + s.size)
                payload.copyInto(
                    destination = pack,
                    destinationOffset = 1 + s.size + i.size,
                    startIndex = position,
                    endIndex = payload.size
                )
                broadcaster.broadcast(PACKET_SLICE, pack)
                return
            }
            // 没到最后一包
            else {
                val `this` = size - s.size - i.size
                val pack = ByteArray(size)
                s.copyInto(destination = pack, destinationOffset = 0)
                i.copyInto(destination = pack, destinationOffset = s.size)
                payload.copyInto(
                    destination = pack,
                    destinationOffset = s.size + i.size,
                    startIndex = position,
                    endIndex = position + `this`
                )
                position += `this`
                broadcaster.broadcast(PACKET_SLICE, pack)
            }
        }
    }

    override fun process(remotePackage: RemotePackage) {
        val (id, name, _, payload) = remotePackage
        if (id != PACKET_SLICE.id) return

        val last = payload[0] == 0.toByte()         // 判断停止位
        val stream = ByteArrayInputStream(payload)  // 构造流
        if (last) stream.skip(1)                    // 跳过停止位
        val subSeq = stream.zigzag(false)           // 解子包序列号
        val index = stream.zigzag(false).toInt()    // 解子包序号
        val rest = stream.readBytes()               // 解子包负载
        val actual =                                // 尝试构造完整包
            if (index == 0 && last) rest            // 这是第一包也是最后一包 => 只有一包 => 不进缓存
            else PackInfo(name, subSeq)             // 完整包用名字和序列号标识
                .let { key ->
                    val memory = buffer[key]        // 检查缓存
                    if (memory != null)
                        memory(last, index, rest)?.also { buffer.remove(key) }
                    else
                        null.also { buffer[key] = Buffer(last, index, rest) }
                }

        buffer
            .filterValues { it.delay > timeout }
            .keys.forEach { buffer.remove(it) }

        if (actual != null)
            callbacks.forEach { it process RemotePackage(actual) }
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
    private class Buffer(
        last: Boolean,
        index: Int,
        payload: ByteArray
    ) {
        private var time = 0L
        private val list = mutableListOf<ByteArray?>()
        private val mark = hashSetOf<Int>()
        private var done = false

        init {
            invoke(last, index, payload)
        }

        /**
         * @return 最后活跃时间到当前的延时
         */
        val delay get() = System.currentTimeMillis() - time

        /**
         * 置入一个小包
         * @param last    末包标记
         * @param index   序号
         * @param payload 负载
         *
         * @return 已完结则返回完整包
         */
        @Synchronized
        operator fun invoke(
            last: Boolean,
            index: Int,
            payload: ByteArray
        ): ByteArray? {
            // 尚未保存最后一包
            if (!done) {
                done = done || last
                while (list.size < index) {
                    mark.add(list.size)
                    list.add(null)
                }
                if (list.size == index) {
                    list.add(payload)
                } else {
                    list[index] = payload
                    mark.remove(index)
                }
            }

            // 已经保存最后一包并且不缺包
            if (done && mark.isEmpty())
                return ByteArrayOutputStream(list.sumBy { it!!.size })
                    .apply { for (sub in list) write(sub) }
                    .toByteArray()

            // 更新最后活跃时间
            time = System.currentTimeMillis()
            return null
        }
    }

    private companion object {
        val TYPE_HASH = hashOf<PacketSlicer>()
    }
}
