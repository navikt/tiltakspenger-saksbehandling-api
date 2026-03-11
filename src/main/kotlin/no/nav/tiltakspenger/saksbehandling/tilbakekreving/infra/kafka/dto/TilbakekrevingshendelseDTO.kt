package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.Tilbakekrevingshendelse
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 *  [eksternFagsakId] Tilsvarer saksnummer [no.nav.tiltakspenger.saksbehandling.sak.Saksnummer] for brukerens sak
 *
 * */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "hendelsestype")
@JsonSubTypes(
    JsonSubTypes.Type(value = TilbakekrevingInfoBehovDTO::class, name = "fagsysteminfo_behov"),
    JsonSubTypes.Type(value = TilbakekrevingInfoSvarDTO::class, name = "fagsysteminfo_svar"),
    JsonSubTypes.Type(value = TilbakekrevingBehandlingEndretDTO::class, name = "behandling_endret"),
)
sealed interface TilbakekrevingshendelseDTO {
    val hendelsestype: TilbakekrevingHendelsestypeDTO
    val versjon: Int
    val eksternFagsakId: String
    val hendelseOpprettet: LocalDateTime

    fun tilNyHendelse(key: String, clock: Clock): Tilbakekrevingshendelse?
}

@Suppress("ktlint:standard:enum-entry-name-case")
enum class TilbakekrevingHendelsestypeDTO {
    fagsysteminfo_behov,
    fagsysteminfo_svar,
    behandling_endret,
}

data class TilbakekrevingPeriodeDTO(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    fun tilPeriode(): Periode {
        return Periode(fom, tom)
    }
}

fun String.tilNyTilbakekrevingshendelse(key: String, clock: Clock): Either<Throwable, Tilbakekrevingshendelse?> {
    return Either.catch {
        deserialize<TilbakekrevingshendelseDTO>(this)
            .tilNyHendelse(key, clock)
            .right()
    }.getOrElse {
        return it.left()
    }
}
