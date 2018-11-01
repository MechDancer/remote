@file:Suppress("UNCHECKED_CAST")

package org.mechdancer.remote.topic

import org.mechdancer.remote.builder.remoteHub
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.rmi.Remote
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject
import kotlin.concurrent.thread

/**
 * 数据发布服务器
 * @param name 节点名
 * @param map  话题编解码方案表
 */
class PublishServer(
	name: String,
	val map: Map<String, SerialTool<*>>
) {
	// 生成话题发布节点
	val core = remoteHub<ParserServer>(name) {
		newMemberDetected = ::println
		rmiRemote = object : UnicastRemoteObject(), ParserServer {
			override operator fun <T> get(topic: String) =
				@Suppress("UNCHECKED_CAST")
				map[topic]
					?.let { it as? SerialTool<T> }
					?.input
					?: throw RemoteException("topic not exist or type goes wrong")
		}
	}

	init {
		core.startRMI()
		thread { while (true) core() }
	}

	/**
	 * 发送数据
	 * @param topic 话题名
	 * @param data  数据
	 */
	operator fun <T> set(topic: String, data: T) {
		val pack = map[topic]?.output as? (T) -> ByteArray
		if (pack != null) {
			ByteArrayOutputStream().apply {
				DataOutputStream(this).writeByte(topic.length)
				this.write(topic.toByteArray())
				this.write(pack(data))
			}.toByteArray().let(core::broadcast)
		} else throw IllegalArgumentException("topic is not register")
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			// 编解码方案
			// * topic : text
			//   type  : String
			// * topic : num
			//   type  : Int
			val functions = mapOf(
				"text" to SerialTool(
					{ s: String -> s.toByteArray() },
					{ buffer: ByteArray -> String(buffer) }
				),
				"num" to SerialTool(
					{ s: Int ->
						ByteArrayOutputStream().apply { DataOutputStream(this).writeInt(s) }.toByteArray()
					},
					{ buffer: ByteArray ->
						ByteArrayInputStream(buffer).let(::DataInputStream).readInt()
					}
				))

			// 启动服务器
			PublishServer("server", functions)

			// 启动接收端
			val receiver = remoteHub<Remote> { newMemberDetected = ::println }
			// 持续解析
			thread { while (true) receiver() }
			// 获取 text 解码方案
			val text = receiver.connect<ParserServer>("server").get<String>("text")
			// 获取 num  解码方案
			val num = receiver.connect<ParserServer>("server").get<Int>("num")
			// 接收
			val pText = (functions["text"]!!.output as (String) -> ByteArray)("abc")
			val pNum = (functions["num"]!!.output as (Int) -> ByteArray)(123)
			// 进行解码
			println(text(pText))
			println(num(pNum))
		}
	}
}
