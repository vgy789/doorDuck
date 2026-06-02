package io.github.vgy789.doorDuck.config

import io.github.vgy789.doorDuck.BuildConfig
import io.github.vgy789.doorDuck.model.IntensiveCampus
import io.github.vgy789.doorDuck.ui.InputSanitizer

object AndroidEndpointSecrets {
    val intensiveMskEndpoint: String = InputSanitizer.endpoint(BuildConfig.INTENSIVE_MSK_URL)
    val intensiveNskEndpoint: String = InputSanitizer.endpoint(BuildConfig.INTENSIVE_NSK_URL)
    val intensiveKznEndpoint: String = InputSanitizer.endpoint(BuildConfig.INTENSIVE_KZN_URL)

    val hasFixedIntensiveEndpoints: Boolean
        get() = fixedIntensiveEndpoints().isNotEmpty()

    fun endpointFor(campus: IntensiveCampus): String? {
        return when (campus) {
            IntensiveCampus.MOSCOW -> intensiveMskEndpoint
            IntensiveCampus.NOVOSIBIRSK -> intensiveNskEndpoint
            IntensiveCampus.KAZAN -> intensiveKznEndpoint
            IntensiveCampus.OTHER -> null
        }.takeIf { !it.isNullOrBlank() }
    }

    fun isFixedCampusAvailable(campus: IntensiveCampus): Boolean {
        return campus == IntensiveCampus.OTHER || !endpointFor(campus).isNullOrBlank()
    }

    fun isIntensiveEndpoint(endpoint: String): Boolean {
        val normalized = InputSanitizer.endpoint(endpoint)
        if (normalized.isBlank()) return false
        return normalized in fixedIntensiveEndpoints() ||
            normalized.substringAfter("https://").substringBefore('/').contains("intensive")
    }

    fun campusFor(endpoint: String): IntensiveCampus {
        return when (InputSanitizer.endpoint(endpoint)) {
            intensiveMskEndpoint -> IntensiveCampus.MOSCOW
            intensiveNskEndpoint -> IntensiveCampus.NOVOSIBIRSK
            intensiveKznEndpoint -> IntensiveCampus.KAZAN
            else -> IntensiveCampus.OTHER
        }
    }

    private fun fixedIntensiveEndpoints(): Set<String> {
        return setOf(
            intensiveMskEndpoint,
            intensiveNskEndpoint,
            intensiveKznEndpoint,
        ).filterTo(mutableSetOf()) { it.isNotBlank() }
    }
}
