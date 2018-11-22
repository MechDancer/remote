package org.mechdancer.version2.remote.resources

import org.mechdancer.version2.dependency.ResourceMemory
import java.net.NetworkInterface

/**
 * 网络接口资源
 * @param T 助记符类型
 */
class NetworkInterfaces<T : Any> :
    ResourceMemory<T, NetworkInterface> {
    override fun update(parameter: T, resource: NetworkInterface?): NetworkInterface? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun get(parameter: T): NetworkInterface? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun equals(other: Any?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hashCode(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
