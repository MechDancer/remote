package org.mechdancer.remote

import org.mechdancer.remote.builder.broadcastHub

object A {
    @JvmStatic
    fun main(args: Array<String>) {
        broadcastHub("A") {
            newProcessDetected = ::println
            broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
        }.run { while (true) invoke() }
    }
}

object B {
    @JvmStatic
    fun main(args: Array<String>) {
        broadcastHub("B") {
            newProcessDetected = ::println
            broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
        }.run { while (true) invoke() }
    }
}

object C {
    @JvmStatic
    fun main(args: Array<String>) {
        broadcastHub("C") {
            newProcessDetected = ::println
            broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
        }.run { while (true) invoke() }
    }
}

object D {
    @JvmStatic
    fun main(args: Array<String>) {
        broadcastHub("D") {
            newProcessDetected = ::println
            broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
        }.run {
            broadcast("hello".toByteArray())
            while (true) invoke()
        }
    }
}
