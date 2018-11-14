package org.mechdancer.remote

import org.mechdancer.remote.builder.remoteHub
import org.mechdancer.remote.core.network.multicastFilters
import org.mechdancer.remote.core.network.wirelessFirst
import kotlin.concurrent.thread

object A {
    @JvmStatic
    fun main(args: Array<String>) {
        remoteHub("A") {
            filters1 = multicastFilters
            filters2 = wirelessFirst
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
            filters1 = multicastFilters
            filters2 = wirelessFirst
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
            filters1 = multicastFilters
            filters2 = wirelessFirst
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
