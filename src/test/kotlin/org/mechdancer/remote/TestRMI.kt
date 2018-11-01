package org.mechdancer.remote

import org.mechdancer.remote.builder.remoteHub
import java.rmi.Remote
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject
import kotlin.concurrent.thread

interface RemoteService : Remote {
	@Throws(RemoteException::class)
	fun hello(): String
}

object D {
	@JvmStatic
	fun main(args: Array<String>) {
		remoteHub("RMIServer") { newMemberDetected = ::println }
			.run {
				load("hello", object :
					UnicastRemoteObject(),
					RemoteService {
					override fun hello() = "hello"
				})
				while (true) invoke()
			}
	}
}

object E {
	@JvmStatic
	fun main(args: Array<String>) {
		remoteHub("RMIClient") {
			newMemberDetected = ::println
		}.run {
			thread(isDaemon = true) { while (true) invoke() }
			connect<RemoteService>("RMIServer", "hello")?.hello().let(::println)
		}
	}
}
