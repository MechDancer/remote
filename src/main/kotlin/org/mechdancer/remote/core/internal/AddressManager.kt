package org.mechdancer.remote.core.internal

import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.math.max

/**
 * 地址管理器
 * @param abhiseca 继承已知的信息
 */
internal class AddressManager(
    abhiseca: AddressMap? = null,
    private val ask: (String) -> Unit
) {
    // 地址映射
    private val core = abhiseca?.toMutableMap() ?: mutableMapOf()

    // 阻塞信号
    private val blocker = SignalBlocker()

    /**
     * 浏览地址映射
     */
    val view = object : AddressMap by core {}

    /**
     * 尝试获取一个地址
     * @param name    目标终端名字
     * @param retry   询问重试时间
     * @param timeout 最长阻塞时间
     */
    operator fun get(
        name: String,
        retry: Long = 500,
        timeout: Long = Long.MAX_VALUE
    ): InetSocketAddress? {
        val ending = endTime(timeout)
        while (true) {
            core[name]?.let { return it }
            ask(name)
            blocker block (blockTime(ending, retry) ?: return null)
        }
    }

    /**
     * 置入一个地址
     */
    operator fun set(name: String, address: InetSocketAddress) {
        core[name] = address
        blocker.awake()
    }

    /**
     * 尝试连接到一个远端一次
     *
     * @param name    目标终端名字
     * @param retry   获取地址的重新询问时间
     * @param timeout 总的超时时间
     *
     * @return 已连接的 TCP 客户端或 `null`
     */
    fun connectOnce(
        name: String,
        retry: Long = 500,
        timeout: Long = Long.MAX_VALUE
    ): Socket? {
        val ending = endTime(timeout)
        // 尝试获取 IP 地址
        //   若设计超时短于 100ms 将仅在本地缓存查找
        //   若已超时或超时获取不到将直接返回
        val address =
            blockTime(ending)
                ?.let { get(name, retry, it - 100) }
                ?: return null
        // 设定连接超时时间
        val connectTimeout =
            blockTime(ending)
                ?.let { if (it < Int.MAX_VALUE) it.toInt() else 0 }
                ?.let { max(it, 100) }
                ?: return null
        // 尝试建立连接
        val socket = Socket()
        return try {
            // 连接成功
            socket.apply { connect(address, connectTimeout) }
        } catch (e: SocketTimeoutException) {
            // 获取到的地址无法建立连接
            core -= name
            socket.close()
            null
        }
    }

    /**
     * 反复尝试连接到远端，直到成功或到达超时时间
     *
     * @param name    目标终端名字
     * @param retry   获取地址的重新询问时间
     * @param timeout 总的超时时间
     *
     * @return 已连接的 TCP 客户端或 `null`
     */
    fun connect(
        name: String,
        retry: Long = 500,
        timeout: Long = Long.MAX_VALUE
    ): Socket? {
        val ending = endTime(timeout)
        while (true)
            return connectOnce(
                name,
                retry,
                blockTime(ending) ?: return null
            ) ?: continue
    }
}
