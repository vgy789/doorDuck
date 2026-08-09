package io.github.vgy789.doorDuck.domain

/**
 * The small set of build-time parameters that define doorDuck's timing behavior.
 *
 * User choices, such as automatic refresh and maximum brightness, do not belong
 * here. They are persisted by the platform settings store. Keep parameter
 * meaning and safety constraints close to the values so this file remains the
 * single place to inspect when changing the app's built-in behavior.
 */
data class DoorDuckBehavior(
    /** How long before QR expiration an automatic refresh becomes due. */
    val refreshBeforeExpirationMs: Long = 0L,

    /** Delay after each failed automatic refresh; the final delay is reused. */
    val automaticRetryDelaysMs: List<Long> = listOf(
        1L * 60L * 60L * 1_000L,
        2L * 60L * 60L * 1_000L,
        3L * 60L * 60L * 1_000L,
        6L * 60L * 60L * 1_000L,
        12L * 60L * 60L * 1_000L,
        24L * 60L * 60L * 1_000L,
    ),

    /** Prevents accidental repeated taps on manual refresh. */
    val manualRefreshCooldownMs: Long = 5_000L,

    /** How long a hidden widget reveals its QR after a tap. */
    val widgetRevealDurationMs: Long = 60_000L,

    /** Stored syncs older than this are treated as interrupted. */
    val syncInProgressTimeoutMs: Long = 5L * 60L * 1_000L,
) {
    init {
        require(refreshBeforeExpirationMs >= 0L)
        require(automaticRetryDelaysMs.isNotEmpty())
        require(automaticRetryDelaysMs.all { it > 0L })
        require(automaticRetryDelaysMs.zipWithNext().all { (first, second) -> first <= second })
        require(manualRefreshCooldownMs >= 0L)
        require(widgetRevealDurationMs > 0L)
        require(syncInProgressTimeoutMs > 0L)
    }

    companion object {
        val Default = DoorDuckBehavior()
    }
}
