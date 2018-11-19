package org.mechdancer.remote.core.internal

import kotlin.math.max

/**
 * 由超时时间 [timeout] 计算结束时间
 * @param timeout 毫秒表示的超时时间，不大于 0 的超时时间视作无穷大
 * @return 标准毫秒时间表示的结束时刻，[Long.MAX_VALUE] 表示无穷远后
 */
fun endTime(timeout: Int): Long =
    timeout
        .takeIf { it >= 0 }
        ?.let { System.currentTimeMillis() + it }
        ?: Long.MAX_VALUE

/**
 * 由结束时间 [ending] 和重试时间 [retry] 计算阻塞时间
 * @param ending 用标准毫秒时间表示的最长阻塞时刻
 * @param retry  重试的时间，即一次阻塞的最长时间
 * @return `Int` 范围内的一次阻塞时间，若已经超时返回 `null`
 */
fun blockTime(ending: Long, retry: Int = Int.MAX_VALUE): Int? =
    minOf(
        max(ending - System.currentTimeMillis(), 0),
        Int.MAX_VALUE.toLong(),
        retry.takeIf { it > 0 }?.toLong()
            ?: throw IllegalArgumentException("retry time must greater than 0")
    ).takeIf { it > 0 }?.toInt()
