package org.mechdancer.remote

import org.mechdancer.remote.builder.remoteHub
import org.mechdancer.remote.util.network.contains
import org.mechdancer.remote.util.network.waitSingle
import kotlin.concurrent.thread

object A {
    @JvmStatic
    fun main(args: Array<String>) {
        remoteHub("A") {
            newMemberDetected = ::println
            broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
            commandReceived = { name, ask ->
                String(ask)
                    .also { println("$name(\"${String(ask)}\"): $it") }
                    .let { "ok: $it" }
                    .toByteArray()
            }
        }.run {
            launch { listen() }
            forever { invoke() }
        }
    }
}

object B {
    @JvmStatic
    fun main(args: Array<String>) {
        remoteHub("BB") {
            newMemberDetected = ::println
            broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
            commandReceived = { name, ask ->
                String(ask)
                    .also { println("$name: \"$it\"") }
                    .let { "ok: $it" }
                    .toByteArray()
            }
        }.run {
            println(address)
            launch { listen() }
            launch { "members: ${refresh(200)}".let(::println) }
            forever { invoke() }
        }
    }
}

object C {
    @JvmStatic
    fun main(args: Array<String>) {
        remoteHub("CC") {
            selector = { it waitSingle { net -> "BB" in net } }
            newMemberDetected = ::println
            broadcastReceived = { name, msg -> println("$name: ${String(msg)}") }
            commandReceived = { name, ask ->
                String(ask)
                    .also { println("$name(\"${String(ask)}\"): $it") }
                    .let { "ok: $it" }
                    .toByteArray()
            }
        }.run {
            println(address)
            broadcast("hello".toByteArray())
            thread {
                var i = 0
                while (i++ % 200 < 100) {
                    i.toString()
                        .toByteArray()
                        .let { String(call("BB", it)) }
                        .let(::println)
                    Thread.sleep(10)
                }
            }
            launch { "members: ${refresh(200)}".let(::println) }
            forever { invoke() }
        }
    }
}
