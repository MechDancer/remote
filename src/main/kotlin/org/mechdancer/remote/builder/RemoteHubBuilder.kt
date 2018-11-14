package org.mechdancer.remote.builder

import org.mechdancer.remote.core.RemoteHub
import org.mechdancer.remote.core.network.selectNetwork

/**
 * 建造一个广播服务器
 *
 * @param name  进程名
 * @param block 请求回调
 */
fun remoteHub(
    name: String = "",
    block: RemoteCallbackBuilder.() -> Unit = {}
) = RemoteCallbackBuilder()
    .apply(block)
    .let { info ->
        RemoteHub(
            name,
            selectNetwork(info.filters1, info.filters2)
                ?: throw RuntimeException("no available network"),
            info.newMemberDetected,
            info.broadcastReceived,
            info.commandReceived
        ).also { hub ->
            info.plugins.forEach(hub::setup)
        }
    }
