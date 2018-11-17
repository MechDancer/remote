package org.mechdancer.remote.builder

import org.mechdancer.remote.plugins.LongConnection

fun RemoteCallbackBuilder.Plugins.longConnectionPlugin() =
	setup(LongConnection())