package org.mechdancer.remote

import org.mechdancer.remote.builder.remoteHub
import org.mechdancer.remote.core.forever
import org.mechdancer.remote.core.launch
import org.mechdancer.remote.topic.PublishPlugin
import org.mechdancer.remote.topic.TopicPublisher
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*
import kotlin.random.Random

object TopicServer {
	@JvmStatic
	fun main(args: Array<String>) {
		// 启动服务器
		val server = remoteHub()
		val publisher = PublishPlugin(mapOf(
			"text" to { b -> String(b) },
			"num" to { b -> ByteArrayInputStream(b).let(::DataInputStream).readInt() }
		))
		val text = TopicPublisher("text", server) { s: String -> s.toByteArray() }
		val num = TopicPublisher("num", server) { s: Int -> ByteArrayOutputStream().apply { DataOutputStream(this).writeInt(s) }.toByteArray() }
		server setup publisher
		launch { server.listen() }
		launch { server() }

		forever {
			text publish UUID.randomUUID().toString()
			num publish Random.nextInt(55536)
			Thread.sleep(500)
		}
	}
}

object TopicReceiver {
	@JvmStatic
	fun main(args: Array<String>) {
		// 启动接收端
		val receiver = remoteHub {
			plugins {
				topicReceiver { sender, topic, data ->
					when (topic) {
						"text" -> println("$sender: ${data as String}")
						"num"  -> println("$sender: ${data as Int}")
					}
				}
			}
		}
		// 持续解析
		forever { receiver() }
	}
}
