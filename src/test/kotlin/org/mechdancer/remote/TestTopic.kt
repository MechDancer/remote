package org.mechdancer.remote

import org.mechdancer.remote.builder.remoteHub
import org.mechdancer.remote.core.cancel
import org.mechdancer.remote.core.connectRMI
import org.mechdancer.remote.core.launch
import org.mechdancer.remote.topic.ParserServer
import org.mechdancer.remote.topic.PublishServer
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
	// 持续解析
	launch { receiver() }
	// 获取 text 解码方案
	val text = receiver.connectRMI<ParserServer>("server")?.get<String>("text")
	// 获取 num  解码方案
	val num = receiver.connectRMI<ParserServer>("server")?.get<String>("num")
	// 接收
	val pText = (functions["text"]!!.output as (String) -> ByteArray)("abc")
	val pNum = (functions["num"]!!.output as (Int) -> ByteArray)(123)
	// 进行解码
	println(text!!(pText))
	println(num!!(pNum))
	server.core.cancel<ParserServer>()
}
