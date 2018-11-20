package org.mechdancer.version2.dependency.functions.basic

import org.mechdancer.remote.core.protocol.RemotePackage
import org.mechdancer.version2.dependency.FunctionModule

abstract class MulticastListener : FunctionModule {
    abstract operator fun invoke(remotePackage: RemotePackage)
}
