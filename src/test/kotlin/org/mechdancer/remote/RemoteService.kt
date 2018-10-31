package org.mechdancer.remote

import java.rmi.Remote
import java.rmi.RemoteException

interface RemoteService : Remote {
	@Throws(RemoteException::class)
	fun hello(): String
}
