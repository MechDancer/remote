package org.mechdancer.remote.topic

import org.mechdancer.remote.builder.remoteHub
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.rmi.Remote
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject
import kotlin.concurrent.thread

class SerialTool<T>(
	val output: (T) -> ByteArray,
	val input: (ByteArray) -> T
)

class PublishServer(
	name: String,
	val map: Map<String, SerialTool<*>>
) {
	val core = remoteHub<ParserServer>(name) {
		newMemberDetected = ::println
		rmiRemote = object : UnicastRemoteObject(), ParserServer {
			override fun <T> get(topic: String) =
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

	operator fun <T> set(topic: String, msg: T) {
		@Suppress("UNCHECKED_CAST")
		val pack = map[topic]?.output as? (T) -> ByteArray
		if (pack != null) {
			ByteArrayOutputStream().apply {
				DataOutputStream(this).writeByte(topic.length)
				this.write(topic.toByteArray())
				this.write(pack(msg))
			}.toByteArray().let(core::broadcast)
		} else throw IllegalArgumentException("topic is not register")
	}
}

fun main(args: Array<String>) {
	PublishServer("a", mapOf(
		"hello" to SerialTool(
			{ s: String -> s.toByteArray() },
			{ buffer: ByteArray -> String(buffer) }
		)))
	val temp2 = remoteHub<Remote> {
		newMemberDetected = ::println
	}
	thread { while (true) temp2() }
	val function = temp2.connect<ParserServer>("a").get<String>("hello")
	println(function("xxx".toByteArray()))
}
