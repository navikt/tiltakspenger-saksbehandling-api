package no.nav.tiltakspenger.common

import com.github.tomakehurst.wiremock.WireMockServer

fun withWireMockServer(block: (WireMockServer) -> Unit) {
    val wireMockServer = WireMockServer(0)
    wireMockServer.start()
    try {
        block(wireMockServer)
    } finally {
        wireMockServer.stop()
    }
}
