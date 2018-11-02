package org.mechdancer.remote.core

interface BroadcastPlugin {
	val id: Char
	operator fun invoke(
		host: RemoteHub,
		guest: String,
		payload: ByteArray
	)
}
