package org.mechdancer.remote.core.internal

import kotlin.math.min

/**
 * 由超时时间 [timeout] 计算结束时间
 */
internal fun endTime(timeout: Long) =
    timeout
        .takeIf { it > 0 }
        ?.let { System.currentTimeMillis() + it }
        ?: Long.MAX_VALUE

/**
 * 由结束时间 [ending] 和重试时间 [retry] 计算阻塞时间
 */
internal fun blockTime(ending: Long, retry: Long = Long.MAX_VALUE) =
    min(retry, ending - System.currentTimeMillis()).takeIf { it > 0 }
