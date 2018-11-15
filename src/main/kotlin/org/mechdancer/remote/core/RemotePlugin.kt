package org.mechdancer.remote.core

/**
 * 远程终端插件
 * @param key 键
 */
abstract class RemotePlugin(val key: Key<*>) {
    /**
     * 加载到终端时调用
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

    /**
     * 键类型
     */
    interface Key<out RemotePlugin> {
        /**
         * 指令识别号
         */
        val id: Char
    }
}
