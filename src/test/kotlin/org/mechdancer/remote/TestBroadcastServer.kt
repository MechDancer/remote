package org.mechdancer.remote

import org.mechdancer.remote.builder.broadcastServer

object A {
    @JvmStatic
    fun main(args: Array<String>) {
        broadcastServer("A") {
            newProcessDetected = ::println
            broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
        }
        while (true);
    }
}

object B {
    @JvmStatic
    fun main(args: Array<String>) {
        broadcastServer("B") {
            newProcessDetected = ::println
            broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
        }
        while (true);
    }
}

object C {
    @JvmStatic
    fun main(args: Array<String>) {
        broadcastServer("C") {
            newProcessDetected = ::println
            broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
        }
        while (true);
    }
}

object D {
    @JvmStatic
    fun main(args: Array<String>) {
        val temp = broadcastServer("D") {
            newProcessDetected = ::println
            broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
        }
        temp.broadcast("hello".toByteArray())
        while (true);
    }
}
