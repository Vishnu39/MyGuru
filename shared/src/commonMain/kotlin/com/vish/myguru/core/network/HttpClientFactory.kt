package com.vish.myguru.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Creates the app's [HttpClient].
 *
 * Without an [engine] Ktor picks the platform engine on the classpath (OkHttp on Android,
 * Darwin on iOS). Tests pass a `MockEngine` instead.
 */
fun createHttpClient(engine: HttpClientEngine? = null): HttpClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) {
            // Gist raw files are served as text/plain, so accept JSON regardless of content type.
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
                contentType = ContentType.Any
            )
        }
    }
    return if (engine == null) HttpClient(config) else HttpClient(engine, config)
}
