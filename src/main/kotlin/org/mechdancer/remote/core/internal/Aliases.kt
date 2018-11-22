package org.mechdancer.remote.core.internal

import org.mechdancer.remote.core.RemoteHub
import java.net.InetSocketAddress

internal typealias Received = RemoteHub.(String, ByteArray) -> Unit
internal typealias CallBack = RemoteHub.(String, ByteArray) -> ByteArray
internal typealias MemberMap = Map<String, Long>
internal typealias AddressMap = Map<String, InetSocketAddress>
