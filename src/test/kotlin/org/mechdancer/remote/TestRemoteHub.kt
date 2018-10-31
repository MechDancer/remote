package org.mechdancer.remote

import org.mechdancer.remote.builder.remoteHub
import java.rmi.Remote
import kotlin.concurrent.thread

object A {
	@JvmStatic
	fun main(args: Array<String>) {
		remoteHub<Remote> {
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
		remoteHub<Remote>("BB") {
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
		remoteHub<Remote> {
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
					i++.toString()
						.toByteArray()
						.let { String(remoteCallBack("BB", it)) }
						.let(::println)
					Thread.sleep(100)
				}
			}
			while (true) invoke()
		}
	}
}
