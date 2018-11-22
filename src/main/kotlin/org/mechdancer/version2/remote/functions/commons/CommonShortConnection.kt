package org.mechdancer.version2.remote.functions.commons

import org.mechdancer.remote.core.internal.Command

interface CommonShortConnection {
    fun call(id: Command, payload: ByteArray)
}
