package no.nav.tiltakspenger.saksbehandling.person.infra.http

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.person.Fødselsnumre
import no.nav.tiltakspenger.saksbehandling.person.Identtype
import no.nav.tiltakspenger.saksbehandling.person.Personident
import tools.jackson.module.kotlin.readValue

private data class PdlHentIdenterResponse(
    val hentIdenter: HentIdenter,
)

private data class HentIdenter(
    val identer: List<Ident>,
)

private data class Ident(
    val ident: String,
    val historisk: Boolean,
    val gruppe: Identtype,
) {
    fun toPersonident(): Personident =
        Personident(
            ident = ident,
            historisk = historisk,
            identtype = gruppe,
        )
}

private val logger = KotlinLogging.logger { }

fun String.toPersonidenter(aktorId: String): List<Personident> {
    val data: PdlHentIdenterResponse = Either.catch {
        objectMapper.readValue<PdlHentIdenterResponse>(this)
    }.getOrElse {
        logger.error { "Klarte ikke deserialisere ident-respons fra pdl. Se sikkerlog for mer informasjon" }
        Sikkerlogg.error(it) { "Klarte ikke deserialisere ident-respons fra pdl. Aktørid $aktorId respons: $this " }
        throw it
    }
    return data.hentIdenter.identer.map {
        it.toPersonident()
    }
}

fun String.toFødselsnumre(gjeldende: Fnr): Fødselsnumre {
    val identer = objectMapper.readValue<PdlHentIdenterResponse>(this).hentIdenter.identer
    return Fødselsnumre(gjeldende = gjeldende, tidligere = identer.map { Fnr.fromString(it.ident) }.distinct() - gjeldende)
}
