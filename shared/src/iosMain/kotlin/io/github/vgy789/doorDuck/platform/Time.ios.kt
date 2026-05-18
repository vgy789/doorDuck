package io.github.vgy789.doorDuck.platform

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.gettimeofday
import platform.posix.timeval

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual fun currentTimeMillis(): Long = memScoped {
    val timeValue = alloc<timeval>()
    gettimeofday(timeValue.ptr, null)
    (timeValue.tv_sec * 1000L) + (timeValue.tv_usec / 1000L)
}
