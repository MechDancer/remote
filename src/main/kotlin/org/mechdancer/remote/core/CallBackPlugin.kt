package org.mechdancer.remote.core

/**
 * 远程调用解析插件
 */
interface CallBackPlugin {
	/**
	 * 指令识别号
	 */
	val id: Char

	/**
	 * 接收回调
	 * @param host    接收终端
	 * @param guest   发送终端
	 * @param payload 数据负载
	 */
	operator fun invoke(
		host: RemoteHub,
		guest: String,
		payload: ByteArray
	): ByteArray
}
