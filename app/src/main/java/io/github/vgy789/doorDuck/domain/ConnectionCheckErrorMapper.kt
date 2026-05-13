package io.github.vgy789.doorDuck.domain

import io.github.vgy789.doorDuck.model.ConnectionCheckResult
import java.io.IOException
import retrofit2.HttpException

object ConnectionCheckErrorMapper {
    fun map(throwable: Throwable): ConnectionCheckResult {
        val http = throwable as? HttpException
        if (http != null) {
            return when (http.code()) {
                401, 403 -> ConnectionCheckResult.UNAUTHORIZED
                400, 404 -> ConnectionCheckResult.BOT_UNAVAILABLE
                else -> ConnectionCheckResult.UNKNOWN
            }
        }
        return when (throwable) {
            is IOException -> ConnectionCheckResult.NETWORK_ERROR
            else -> ConnectionCheckResult.UNKNOWN
        }
    }
}
