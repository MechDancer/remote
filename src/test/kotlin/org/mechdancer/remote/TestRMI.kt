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
		remoteHub<RemoteService>("RMIServer") {
			newProcessDetected = ::println
			rmiRemote = object :
				UnicastRemoteObject(),
				RemoteService {
				override fun hello() = "hello"
			}
		}.run {
			startRMI()
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
			thread(isDaemon = true) { while (true) invoke() }
			connect<RemoteService>("RMIServer").hello().let(::println)
		}
	}
}
