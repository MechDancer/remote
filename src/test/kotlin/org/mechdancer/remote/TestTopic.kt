package org.mechdancer.remote

import org.mechdancer.remote.builder.remoteHub
import org.mechdancer.remote.core.cancel
import org.mechdancer.remote.core.launch
import org.mechdancer.remote.topic.ParserServer
import org.mechdancer.remote.topic.PublishServer
import org.mechdancer.remote.topic.ReceivePlugin
import org.mechdancer.remote.topic.SerialTool
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

@Suppress("UNCHECKED_CAST")
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
	val server = PublishServer(remoteHub("server") { newMemberDetected = ::println }, functions)
	launch { server.core() }

	// 启动接收端
	val receiver = remoteHub { newMemberDetected = ::println }
	// 加载接收插件
	receiver.setup(ReceivePlugin())
	// 持续解析
	launch { receiver() }

	// 发送
	server["text"] = "xyz"

	Thread.sleep(1000)

	server.core.cancel<ParserServer>()
}
