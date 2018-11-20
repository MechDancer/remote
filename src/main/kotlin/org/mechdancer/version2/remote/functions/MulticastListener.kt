package org.mechdancer.version2.remote.functions

import org.mechdancer.remote.core.protocol.RemotePackage
import org.mechdancer.version2.dependency.FunctionModule

interface MulticastListener : FunctionModule {
    infix fun process(remotePackage: RemotePackage)
}
