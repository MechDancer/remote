package org.mechdancer.remote

import org.mechdancer.remote.builder.remoteHub
import org.mechdancer.remote.core.get
import org.mechdancer.remote.plugins.LongConnection
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    val a = remoteHub("a") {
        plugins {
            setup(LongConnection())
        }
    }.apply {
        thread {
            while (true) invoke()
        }
    }

    val b = remoteHub("b") {
        plugins {
            setup(LongConnection())
        }
    }.apply {
        thread {
            while (true) invoke()
        }
    }

    a.get<LongConnection>()!!.sendTo("b", "hello".toByteArray())
    b.get<LongConnection>()!!.receiveFrom("a").let { println(String(it)) }
}
