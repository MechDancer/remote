package org.mechdancer.remote

import org.mechdancer.remote.builder.remoteHub
import org.mechdancer.remote.core.forever
import org.mechdancer.remote.core.launch
import org.mechdancer.remote.topic.PublishServer
import org.mechdancer.remote.topic.ReceivePlugin
import org.mechdancer.remote.topic.SerialTool
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*
import kotlin.random.Random

object TopicServer {
	@JvmStatic
	fun main(args: Array<String>) {
		// 编解码方案
		// - text : String
		// - num  : Int
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
		val server = PublishServer(remoteHub(), functions)
		launch { server.core() }
		server.start()

		forever {
			server["text"] = UUID.randomUUID().toString()
			server["num"] = Random.nextInt(55536)
			Thread.sleep(500)
		}
	}
}

object TopicReceiver {
	@JvmStatic
	fun main(args: Array<String>) {
		// 启动接收端
		val receiver = remoteHub { newMemberDetected = ::println }
		// 加载接收插件
		receiver setup ReceivePlugin { sender, topic, data ->
			when (topic) {
				"text" -> println("$sender: ${data as String}")
				"num"  -> println("$sender: ${data as Int}")
			}
		}
		// 持续解析
		forever { receiver() }
	}
}
