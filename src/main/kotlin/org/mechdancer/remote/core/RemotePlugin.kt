package org.mechdancer.remote.core

/**
 * 远程终端插件
 * @param id 指令识别号
 */
abstract class RemotePlugin(val id: Char) {
    init {
        assert(id.isLetterOrDigit())
    }

    /**
     * 加载到终端时调用
     * @param host 目标终端
     */
    open fun onSetup(host: RemoteHub) {}

    /**
     * 从终端卸载时调用
     */
    open fun onTeardown() {}

    /**
     * 收到相关广播时调用
     *
     * @param sender   发送终端
     * @param payload  数据负载
     */
    open fun onBroadcast(sender: String, payload: ByteArray) = Unit

    /**
     * 收到相关单播时调用
     *
     * @param sender   发送终端
     * @param payload  数据负载
     */
    open fun onCall(sender: String, payload: ByteArray): ByteArray = byteArrayOf()

    override fun equals(other: Any?) =
        this === other || other is RemotePlugin && id == other.id

    override fun hashCode() = id.toInt()
}
