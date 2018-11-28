package org.mechdancer.framework.remote.modules.multicast

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.get
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.protocol.SimpleInputStream
import org.mechdancer.framework.remote.protocol.SimpleOutputStream
import org.mechdancer.framework.remote.protocol.zigzag
import org.mechdancer.framework.remote.resources.Command
import org.mechdancer.framework.remote.resources.UdpCmd
import org.mechdancer.framework.remote.resources.UdpCmd.PACKET_SLICE
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 数据包分片协议
 */
class PacketSlicer(
    private val size: Int = 0x4000 // 16kB
) : AbstractModule(), MulticastListener {

    init {
        assert(size in 16..65536)
    }

    // 发送

    private val broadcaster by must<MulticastBroadcaster>(host)
    private val sequence = AtomicLong(1) // 必须从 1 开始！0 用于指示最后一包！

    // 接收

    private val buffers = ConcurrentHashMap<PackInfo, Buffer>()
    private val callbacks = mutableSetOf<MulticastListener>()

    override fun sync() {
        synchronized(callbacks) {
            callbacks.clear()
            callbacks.addAll(host().get())
            callbacks.remove(this)
        }
    }

    override val interest = INTEREST

    /**
     * 使用拆包协议广播一包
     */
    fun broadcast(cmd: Command, payload: ByteArray) {
        val stream = SimpleInputStream(payload)
        val s = sequence.getAndIncrement().zigzag(false)
        var index = 0L // 包序号

        while (stream.available() > 0) {
            // 编码子包序号
            val i = index++.zigzag(false)
            // 如果是最后一包，应该多长?
            val last = stream.available() + 2 + s.size + i.size
            // 打包
            val pack =
                if (last <= size) {
                    SimpleOutputStream(last)
                        .apply {
                            write(0)      // 空一位作为停止位
                            write(cmd.id) // 保存实际指令
                            write(s)
                            write(i)
                            writeFrom(stream, stream.available())
                        }
                } else {
                    val length = size - s.size - i.size
                    SimpleOutputStream(size)
                        .apply {
                            write(s)
                            write(i)
                            writeFrom(stream, length)
                        }
                }

            broadcaster.broadcast(PACKET_SLICE.id, pack.core)
        }
    }

    override fun process(remotePacket: RemotePacket) {
        val (name, _, payload) = remotePacket

        val stream = SimpleInputStream(payload) // 构造流
        val cmd =
            stream.takeIf { it.look() == LAST } // 判断停止位
                ?.skip(1)                       // 跳过停止位和指令位
                ?.read()
                ?.toByte()
        val subSeq = stream.zigzag(false) // 解子包序列号
        val index = stream.zigzag(false)  // 解子包序号
        val rest = stream.lookRest()      // 解子包负载

        when { // 这是第一包也是最后一包 => 只有一包 => 不进缓存
            index == 0L && cmd != null -> cmd to rest
            else                       ->
                PackInfo(name, subSeq).let { key ->
                    buffers
                        .computeIfAbsent(key) { Buffer() }
                        .put(cmd, index.toInt(), rest)
                        ?.also { buffers.remove(key) }
                }
        }
            ?.let { (cmd, payload) -> RemotePacket(name, cmd, payload) }
            ?.let { pack ->
                callbacks
                    .filter { UdpCmd[pack.command] in it.interest }
                    .forEach { it process pack }
            }
    }

    /**
     * 清理缓冲中最后活跃时间超过 [timeout]ms 的数据包
     */
    fun refresh(timeout: Int) {
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

        private val list = mutableListOf<Hook>()
        private val mark = hashMapOf<Int, Hook>()

        private var command: Byte? = null
        private val done get() = command != null

        /**
         * @return 最后活跃时间到当前的延时
         */
        infix fun by(now: Long) = now - time

        /**
         * 置入一个小包
         * @param cmd     包指令
         * @param index   序号
         * @param payload 负载
         *
         * @return 已完结则返回完整包
         */
        fun put(
            cmd: Byte?,
            index: Int,
            payload: ByteArray
        ): Pair<Byte, ByteArray>? {
            // 修改状态，加锁保护
            synchronized(this) {
                if (done) mark.remove(index)!!.ptr = payload
                else {
                    command = cmd

                    for (i in list.size until index) {
                        val hook = Hook(null)
                        mark[i] = hook
                        list.add(hook)
                    }

                    if (list.size != index)
                        mark.remove(index)!!.ptr = payload
                    else
                        list.add(Hook(payload))
                }
            }

            // 已经保存最后一包并且不缺包
            if (done && mark.isEmpty())
                return command!! to SimpleOutputStream(list.sumBy { it.ptr!!.size })
                    .apply { for (sub in list) write(sub.ptr!!) }
                    .core

            // 更新最后活跃时间
            time = System.currentTimeMillis()
            return null
        }

        private class Hook(var ptr: ByteArray?)
    }

    private companion object {
        val TYPE_HASH = hashOf<PacketSlicer>()
        val INTEREST = setOf(PACKET_SLICE)
        const val LAST = 0.toByte()
    }
}
