package org.mechdancer.remote

import org.mechdancer.remote.builder.remoteHub
import org.mechdancer.remote.core.connectRMI
import org.mechdancer.remote.core.forever
import org.mechdancer.remote.core.put
import java.rmi.Remote
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject
import kotlin.concurrent.thread

interface Greeting : Remote {
	@Throws(RemoteException::class)
	fun hello(): String
}

object D {
	@JvmStatic
	fun main(args: Array<String>) {
		val hub = remoteHub("RMIServer") {
			newMemberDetected = ::println
			put<Greeting>(object :
				UnicastRemoteObject(),
				Greeting {
				override fun hello() = "hello"
			})
		}
		forever { hub() }
	}
}

object E {
	@JvmStatic
	fun main(args: Array<String>) {
		remoteHub("RMIClient") {
			newMemberDetected = ::println
		}.run {
			thread(isDaemon = true) { while (true) invoke() }
			connectRMI<Greeting>("RMIServer")?.hello().let(::println)
		}
	}
}
