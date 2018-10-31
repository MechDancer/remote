package org.mechdancer.remote

import java.rmi.registry.LocateRegistry
import java.rmi.server.UnicastRemoteObject

object Client {
	@JvmStatic
	fun main(args: Array<String>) {
		val studentService =
			LocateRegistry
				.getRegistry("192.168.18.103", 5008)
				.lookup("Service") as RemoteService
		println(studentService.hello())
	}
}

object Service {
	@JvmStatic
	fun main(args: Array<String>) {
		val registry = LocateRegistry.createRegistry(5008)
		registry.rebind(
			"Service",
			object :
				UnicastRemoteObject(),
				RemoteService {
				override fun hello() = "hello"
			})
		registry.unbind("Service")
	}
}
