package org.mechdancer.remote.core

data class ConnectionInfo(
	val address: HubAddress?,
	val stamp: Long
) {
	constructor(address: HubAddress?) :
		this(address, System.currentTimeMillis())
}
