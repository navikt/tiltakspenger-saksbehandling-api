package no.nav.tiltakspenger.saksbehandling.person.infra.http

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.person.Personident
import tools.jackson.module.kotlin.readValue

data class PdlHentIdenterResponse(
    val hentIdenter: HentIdenter,
)

data class HentIdenter(
    val identer: List<Personident>,
)

private val logger = KotlinLogging.logger { }

fun String.toPersonidenter(aktorId: String): List<Personident> {
    val data: PdlHentIdenterResponse = Either.catch {
        objectMapper.readValue<PdlHentIdenterResponse>(this)
    }.getOrElse {
        logger.error { "Klarte ikke deserialisere ident-respons fra pdl. Se sikkerlog for mer informasjon" }
        Sikkerlogg.error(it) { "Klarte ikke deserialisere ident-respons fra pdl. Aktørid $aktorId respons: $this " }
        throw it
    }
    return data.hentIdenter.identer
}
