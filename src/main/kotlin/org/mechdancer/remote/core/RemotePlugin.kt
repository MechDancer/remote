package org.mechdancer.remote.core

/**
 * 远程终端插件
 */
abstract class RemotePlugin {
    /**
     * 指令识别号
     */
    abstract val id: Char

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
     * @param receiver 接收终端
     * @param sender   发送终端
     * @param payload  数据负载
     */
    open operator fun invoke(
        receiver: RemoteHub,
        sender: String,
        payload: ByteArray
    ) = Unit

    /**
     * 收到相关单播时调用
     *
     * @param receiver 接收终端
     * @param sender   发送终端
     * @param payload  数据负载
     */
    open fun onCall(
        receiver: RemoteHub,
        sender: String,
        payload: ByteArray
    ): ByteArray = byteArrayOf()
}
