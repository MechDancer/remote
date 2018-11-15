package org.mechdancer.remote.core.plugin

import org.mechdancer.remote.core.RemoteHub

abstract class RemotePlugin(val key: Key<*>) {


    /**
     * 插件加载
     */
    open fun onSetup(host: RemoteHub) {}

    /**
     * 插件卸载
     */
    open fun onTeardown() {}

    /**
     * 广播接收回调
     *
     * @param host    接收终端
     * @param sender   发送终端
     * @param payload 数据负载
     */
    open operator fun invoke(
        host: RemoteHub,
        sender: String,
        payload: ByteArray
    ) = Unit

    /**
     * 调用接收回调
     *
     * @param host    接收终端
     * @param sender   发送终端
     * @param payload 数据负载
     */
    open fun onCall(
        host: RemoteHub,
        sender: String,
        payload: ByteArray
    ): ByteArray = byteArrayOf()


    interface Key<R : RemotePlugin> {
        /**
         * 指令识别号
         */
        val id: Char
    }
}
