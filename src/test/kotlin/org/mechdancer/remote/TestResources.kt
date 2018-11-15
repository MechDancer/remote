package org.mechdancer.remote

import org.mechdancer.remote.builder.remoteHub
import org.mechdancer.remote.builder.resourcePlugin
import org.mechdancer.remote.core.plugin.ResourcePlugin
import kotlin.concurrent.thread

object M {
    @JvmStatic
    fun main(args: Array<String>) {
        remoteHub("M") {
            plugins {
                resourcePlugin {
                    resources += "Apple" to "Banana".toByteArray()
                }
            }
        }.run {
            thread {
                println((this[ResourcePlugin]!!["Foo"]?.let { String(it) }))
            }
            forever { invoke() }
        }
    }
}

object N {
    @JvmStatic
    fun main(args: Array<String>) {
        remoteHub("N") {
            plugins {
                resourcePlugin {
                    resources += "Foo" to "Bar".toByteArray()
                }
            }
        }.run {
            thread {
                println((this[ResourcePlugin]!!["Apple"]?.let { String(it) }))
            }
            forever { invoke() }
        }
    }
}