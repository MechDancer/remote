package org.mechdancer.remote

import org.mechdancer.remote.PublisherConfig.num
import org.mechdancer.remote.PublisherConfig.server
import org.mechdancer.remote.PublisherConfig.text
import org.mechdancer.remote.ReceiverConfig.receiver
import org.mechdancer.remote.builder.remoteHub
import org.mechdancer.remote.core.forever
import org.mechdancer.remote.core.launch
import org.mechdancer.remote.topic.TopicPublisher
import org.mechdancer.remote.topic.encode
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.*
import kotlin.random.Random

object PublisherConfig {
	val server = remoteHub {
		plugins {
			topicParserServer(
				"text" to { b -> String(b) },
				"num" to { b -> ByteArrayInputStream(b).let(::DataInputStream).readInt() }
			)
		}
	}
	val text = TopicPublisher("text", server) { s: String -> s.toByteArray() }
	val num = TopicPublisher("num", server) { s: Int -> encode(s) }
}

object ReceiverConfig {
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
}

object TopicServer {
	@JvmStatic
	fun main(args: Array<String>) {
		launch { server.listen() }
		launch { server() }

		for (i in 1..20) {
			text publish UUID.randomUUID().toString()
			num publish Random.nextInt(55536)
			//Thread.sleep(500)
		}
		forever { }
	}
}

object TopicReceiver {
	@JvmStatic
	fun main(args: Array<String>) = forever { receiver() }
}
