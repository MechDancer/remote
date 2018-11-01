package org.mechdancer.remote

import org.mechdancer.remote.builder.remoteHub
import org.mechdancer.remote.core.forever
import org.mechdancer.remote.core.launch
import java.rmi.Remote
import kotlin.concurrent.thread

object A {
	@JvmStatic
	fun main(args: Array<String>) {
		remoteHub<Remote> {
			newMemberDetected = ::println
			broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
			commandReceived = { name, ask ->
				String(ask).also(::println).let { "$name(\"${String(ask)}\"): $it".toByteArray() }
			}
		}.run {
			launch { listen() }
			forever { invoke() }
		}
	}
}

object B {
	@JvmStatic
	fun main(args: Array<String>) {
		remoteHub<Remote>("BB") {
			newMemberDetected = ::println
			broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
			commandReceived = { name, ask ->
				String(ask).also(::println).let { "$name(\"${String(ask)}\"): $it".toByteArray() }
			}
		}.run {
			launch { listen() }
			launch { println("members: ${refresh(200)}") }
			forever { invoke() }
		}
	}
}

object C {
	@JvmStatic
	fun main(args: Array<String>) {
		remoteHub<Remote> {
			newMemberDetected = ::println
			broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
			commandReceived = { name, ask ->
				String(ask).also(::println).let { "$name(\"${String(ask)}\"): $it".toByteArray() }
			}
		}.run {
			broadcast("hello".toByteArray())
			thread {
				var i = 0
				while (i++ % 200 < 100) {
					i.toString()
						.toByteArray()
						.let { String(call("BB", it)) }
						.let(::println)
					Thread.sleep(10)
				}
			}
			forever { invoke() }
		}
	}
}
