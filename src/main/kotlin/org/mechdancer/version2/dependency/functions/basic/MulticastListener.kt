package org.mechdancer.version2.dependency.functions.basic

import org.mechdancer.remote.core.protocol.RemotePackage
import org.mechdancer.version2.dependency.FunctionModule

interface MulticastListener : FunctionModule {
    infix fun process(remotePackage: RemotePackage)
}
