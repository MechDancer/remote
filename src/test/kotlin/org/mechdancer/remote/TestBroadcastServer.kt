package org.mechdancer.remote

import org.mechdancer.remote.builder.broadcastHub
import kotlin.concurrent.thread

object A {
	@JvmStatic
	fun main(args: Array<String>) {
		broadcastHub("A") {
			newProcessDetected = ::println
			broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
			remoteProcess = { name, ask ->
				String(ask).also(::println).let { "$name(\"${String(ask)}\"): $it".toByteArray() }
			}
		}.run {
			thread { while (true) listen() }
			while (true) invoke()
		}
	}
}

object B {
	@JvmStatic
	fun main(args: Array<String>) {
		broadcastHub("BB") {
			newProcessDetected = ::println
			broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
			remoteProcess = { name, ask ->
				String(ask).also(::println).let { "$name(\"${String(ask)}\"): $it".toByteArray() }
			}
		}.run {
			thread { while (true) listen() }
			while (true) invoke()
		}
	}
}

object C {
	@JvmStatic
	fun main(args: Array<String>) {
		broadcastHub("CCC") {
			newProcessDetected = ::println
			broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
			remoteProcess = { name, ask ->
				String(ask).also(::println).let { "$name(\"${String(ask)}\"): $it".toByteArray() }
			}
		}.run {
			thread { while (true) listen() }
			while (true) invoke()
		}
	}
}

object D {
	@JvmStatic
	fun main(args: Array<String>) {
		broadcastHub("DDDD") {
			newProcessDetected = ::println
			broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
			remoteProcess = { name, ask ->
				String(ask).also(::println).let { "$name(\"${String(ask)}\"): $it".toByteArray() }
			}
		}.run {
			broadcast("hello".toByteArray())
			thread {
				var i = 0
				while (true) {
					println(String(remoteCallBack("CCC", i++.toString().toByteArray()).second))
					Thread.sleep(100)
				}
			}
			while (true) invoke()
		}
	}
}
