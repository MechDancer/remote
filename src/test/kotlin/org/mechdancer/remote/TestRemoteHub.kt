package org.mechdancer.remote

import org.mechdancer.remote.builder.remoteHub
import org.mechdancer.remote.core.forever
import org.mechdancer.remote.core.launch
import kotlin.concurrent.thread

object A {
	@JvmStatic
	fun main(args: Array<String>) {
		remoteHub("A") {
			newMemberDetected = ::println
			broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
			commandReceived = { name, ask ->
				String(ask)
					.also { println("$name(\"${String(ask)}\"): $it") }
					.let { "ok: $it" }
					.toByteArray()
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
		remoteHub("BB") {
			newMemberDetected = ::println
			broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
			commandReceived = { name, ask ->
				String(ask)
					.also { println("$name: \"$it\"") }
					.let { "ok: $it" }
					.toByteArray()
			}
		}.run {
			println(address)
			launch { listen() }
			launch { println("members: ${refresh(1000)}") }
			forever { invoke() }
		}
	}
}

object C {
	@JvmStatic
	fun main(args: Array<String>) {
		remoteHub {
			newMemberDetected = ::println
			broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
			commandReceived = { name, ask ->
				String(ask)
					.also { println("$name(\"${String(ask)}\"): $it") }
					.let { "ok: $it" }
					.toByteArray()
			}
		}.run {
			broadcast("hello".toByteArray())
			launch {
				println("members: ${refresh(1000)}")
			}
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
