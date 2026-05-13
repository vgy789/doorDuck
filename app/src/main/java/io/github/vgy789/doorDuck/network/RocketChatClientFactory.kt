package io.github.vgy789.doorDuck.network

import io.github.vgy789.doorDuck.model.Credentials
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

class RocketChatClientFactory(
    private val json: Json,
) {
    fun create(endpoint: String, credentials: Credentials): RocketChatApi {
        val normalizedBaseUrl = normalizeBaseUrl(endpoint)
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("X-Auth-Token", credentials.authToken)
                .header("X-User-Id", credentials.userId)
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RocketChatApi::class.java)
    }

    private fun normalizeBaseUrl(rawEndpoint: String): String {
        val trimmed = rawEndpoint.trim().removeSuffix("/")
        return "$trimmed/"
    }
}
