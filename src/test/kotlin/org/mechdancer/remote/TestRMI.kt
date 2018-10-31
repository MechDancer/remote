package org.mechdancer.remote

import org.mechdancer.remote.builder.remoteHub
import java.rmi.Remote
import java.rmi.server.UnicastRemoteObject
import kotlin.concurrent.thread

object D {
	@JvmStatic
	fun main(args: Array<String>) {
		remoteHub<RemoteService>("RMIServer") {
			newProcessDetected = ::println
			rmiRemote = object :
				UnicastRemoteObject(),
				RemoteService {
				override fun hello() = "hello"
			}
		}.run {
			while (true) invoke()
		}
	}
}

object E {
	@JvmStatic
	fun main(args: Array<String>) {
		remoteHub<Remote>("RMIClient") {
			newProcessDetected = ::println
		}.run {
			thread { while (true) invoke() }
			getRegistry<RemoteService>("RMIServer").hello().let(::println)
		}
	}
}
