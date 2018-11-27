package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.remote.protocol.readWithLength
import org.mechdancer.framework.remote.protocol.writeWithLength
import org.mechdancer.framework.remote.resources.Command
import java.net.Socket

infix fun Socket.say(cmd: Command) =
    getOutputStream().write(cmd.id.toInt())

infix fun Socket.say(byteArray: ByteArray) =
    getOutputStream().writeWithLength(byteArray)

fun <T> Socket.listen(block: (Byte) -> T) =
    getInputStream().read().toByte().let(block)

fun Socket.listen() =
    getInputStream().readWithLength()
