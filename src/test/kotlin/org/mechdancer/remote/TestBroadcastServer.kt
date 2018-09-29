package org.mechdancer.remote

import org.mechdancer.remote.builder.broadcastServer

object A {
    @JvmStatic
    fun main(args: Array<String>) {
        broadcastServer("A") {
            newProcessDetected = ::println
            broadcastReceived = { println("$this: ${String(it)}") }
        }
        while (true);
    }
}

object B {
    @JvmStatic
    fun main(args: Array<String>) {
        broadcastServer("B") {
            newProcessDetected = ::println
            broadcastReceived = { println("$this: ${String(it)}") }
        }
        while (true);
    }
}

object C {
    @JvmStatic
    fun main(args: Array<String>) {
        broadcastServer("C") {
            newProcessDetected = ::println
            broadcastReceived = { println("$this: ${String(it)}") }
        }
        while (true);
    }
}

object D {
    @JvmStatic
    fun main(args: Array<String>) {
        val temp = broadcastServer("D") {
            newProcessDetected = ::println
            broadcastReceived = { println("$this: ${String(it)}") }
        }
        temp.broadcast("hello".toByteArray())
        while (true);
    }
}
