package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.FunctionModule

interface MailListener : FunctionModule {
    fun process(sender: String, payload: ByteArray)
}
