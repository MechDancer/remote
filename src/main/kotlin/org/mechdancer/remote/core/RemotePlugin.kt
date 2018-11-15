package org.mechdancer.remote.core

/**
 * 远程终端插件
 */
interface RemotePlugin {
    /**
     * 指令识别号
     */
    val id: Char

    /**
     * 加载到终端时调用
     * @param host 目标终端
     */
    fun onSetup(host: RemoteHub) {}

    /**
     * 从终端卸载时调用
     */
    fun onTeardown() {}

    /**
     * 收到相关广播时调用
     *
     * @param receiver 接收终端
     * @param sender   发送终端
     * @param payload  数据负载
     */
    operator fun invoke(
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
    fun onCall(
        receiver: RemoteHub,
        sender: String,
        payload: ByteArray
    ): ByteArray = byteArrayOf()
}
