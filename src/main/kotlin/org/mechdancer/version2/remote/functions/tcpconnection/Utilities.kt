package org.mechdancer.version2.remote.functions.tcpconnection

import org.mechdancer.version2.remote.protocol.readWithLength
import org.mechdancer.version2.remote.protocol.writeWithLength
import org.mechdancer.version2.remote.resources.Command
import java.net.Socket

infix fun Socket.order(cmd: Command) {
    getOutputStream().write(cmd.id.toInt())
}

fun <T : Command> Socket.listen(block: (Byte) -> T) =
    getInputStream().read().toByte().let(block)

infix fun Socket.say(byteArray: ByteArray) =
    getOutputStream().writeWithLength(byteArray)

fun Socket.listen() =
    getInputStream().readWithLength()
