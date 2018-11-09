package org.mechdancer.remote.core

internal typealias Received = RemoteHub.(String, ByteArray) -> Unit
internal typealias CallBack = RemoteHub.(String, ByteArray) -> ByteArray
