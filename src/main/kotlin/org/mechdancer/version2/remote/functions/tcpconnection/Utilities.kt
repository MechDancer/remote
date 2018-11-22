package org.mechdancer.version2.remote.functions.tcpconnection

import org.mechdancer.remote.core.internal.Command
import org.mechdancer.remote.core.protocol.readWithLength
import org.mechdancer.remote.core.protocol.writeWithLength
import java.net.Socket

infix fun Socket.order(cmd: Command) {
    getOutputStream().write(cmd.id.toInt())
}

infix fun Socket.say(byteArray: ByteArray) =
    getOutputStream().writeWithLength(byteArray)

fun Socket.listen() =
    getInputStream().readWithLength()
