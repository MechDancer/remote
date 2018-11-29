package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.remote.protocol.readWithLength
import org.mechdancer.framework.remote.protocol.writeWithLength
import org.mechdancer.framework.remote.resources.Command
import java.net.Socket

infix fun Socket.say(cmd: Command) =
    getOutputStream().write(cmd.id.toInt())

infix fun Socket.say(byteArray: ByteArray) =
    getOutputStream().writeWithLength(byteArray)

fun Socket.listenCommand() =
    getInputStream().read().toByte()

fun Socket.listen() =
    getInputStream().readWithLength()
