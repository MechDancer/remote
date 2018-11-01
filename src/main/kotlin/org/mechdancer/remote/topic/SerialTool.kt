package org.mechdancer.remote.topic

/**
 * 正反序列化工具
 * @param output 打包
 * @param input  解包
 */
class SerialTool<T>(
	val output: (T) -> ByteArray,
	val input: (ByteArray) -> T
)
