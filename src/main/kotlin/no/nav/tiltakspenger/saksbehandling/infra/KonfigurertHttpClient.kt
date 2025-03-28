package no.nav.tiltakspenger.saksbehandling.infra

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.jackson.jackson
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import java.time.Duration

private val LOG = KotlinLogging.logger {}

private const val SIXTY_SECONDS = 60L

fun httpClientApache(timeout: Long = SIXTY_SECONDS) = HttpClient(Apache).config(timeout)

fun httpClientGeneric(
    engine: HttpClientEngine?,
    timeout: Long = SIXTY_SECONDS,
) = engine
    ?.let { HttpClient(engine).config(timeout) }
    ?: HttpClient(Apache).config(timeout)

fun httpClientWithRetry(timeout: Long = SIXTY_SECONDS) =
    httpClientApache(timeout).also { httpClient ->
        httpClient.config {
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                retryOnException(maxRetries = 3, retryOnTimeout = true)
                constantDelay(100, 0, false)
            }
        }
    }

private fun HttpClient.config(timeout: Long) =
    this.config {
        install(ContentNegotiation) {
            jackson {
                registerModule(KotlinModule.Builder().build())
                registerModule(JavaTimeModule())
                setDefaultPrettyPrinter(
                    DefaultPrettyPrinter().apply {
                        indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                        indentObjectsWith(DefaultIndenter("  ", "\n"))
                    },
                )
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = Duration.ofSeconds(timeout).toMillis()
            requestTimeoutMillis = Duration.ofSeconds(timeout).toMillis()
            socketTimeoutMillis = Duration.ofSeconds(timeout).toMillis()
        }
        install(Logging) {
            logger =
                object : Logger {
                    override fun log(message: String) {
                        LOG.info { "HttpClient detaljer logget til securelog" }
                        sikkerlogg.info { message }
                    }
                }
            level = LogLevel.INFO
        }
        expectSuccess = true
    }
