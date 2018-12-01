package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.Dependent

interface MailListener : Dependent {
    fun process(sender: String, payload: ByteArray)
}
