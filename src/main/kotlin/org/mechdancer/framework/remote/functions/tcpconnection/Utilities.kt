package org.mechdancer.framework.remote.functions.tcpconnection

import org.mechdancer.framework.remote.protocol.readWithLength
import org.mechdancer.framework.remote.protocol.writeWithLength
import org.mechdancer.framework.remote.resources.Command
import java.net.Socket

infix fun Socket.say(cmd: Command) {
    getOutputStream().write(cmd.id.toInt())
}

fun <T : Command> Socket.listen(block: (Byte) -> T) =
    getInputStream().read().toByte().let(block)

infix fun Socket.say(byteArray: ByteArray) =
    getOutputStream().writeWithLength(byteArray)

fun Socket.listen() =
    getInputStream().readWithLength()