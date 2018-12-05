package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.Component

interface MailListener : Component {
    fun process(sender: String, payload: ByteArray)
}
