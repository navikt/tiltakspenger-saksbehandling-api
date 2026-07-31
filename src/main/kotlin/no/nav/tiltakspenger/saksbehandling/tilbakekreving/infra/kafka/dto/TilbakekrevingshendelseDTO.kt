package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto

import arrow.core.Either
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingUkjentHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.Tilbakekrevingshendelse
import java.time.Clock
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

/**
 *  [eksternFagsakId] Tilsvarer saksnummer [no.nav.tiltakspenger.libs.common.Saksnummer] for brukerens sak
 *
 * */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "hendelsestype", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = TilbakekrevingInfoBehovDTO::class, name = "fagsysteminfo_behov"),
    JsonSubTypes.Type(value = TilbakekrevingInfoSvarDTO::class, name = "fagsysteminfo_svar"),
    JsonSubTypes.Type(value = TilbakekrevingBehandlingEndretDTO::class, name = "behandling_endret"),
)
sealed interface TilbakekrevingshendelseDTO {
    val hendelsestype: TilbakekrevingHendelsestypeDTO
    val versjon: Int
    val eksternFagsakId: String
    val hendelseOpprettet: String

    /**
     * @return [Tilbakekrevingshendelse] dersom hendelsen skal lagres i databasen, eller null dersom den ikke skal lagres
     * */
    fun tilHendelseForLagring(id: TilbakekrevinghendelseId): Tilbakekrevingshendelse?
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

/**
 * Forsøker å deserialisere en Kafka-melding til en [Tilbakekrevingshendelse].
 *
 * @param erDev styrer kun loggnivået, og injiseres fra komposisjonsroten.
 * Team tilbakekreving genererer testdata hos seg selv i dev og sender dem på køen.
 * Saksnumrene er ikke våre, og hendelsene skal forkastes, så en hendelse vi ikke klarer å lese er forventet i dev og ikke verdt en ERROR.
 * I prod er den et ekte problem, og da vil vi ha stacktracen.
 *
 * @return [Tilbakekrevingshendelse] dersom hendelsen skal lagres - en [TilbakekrevingUkjentHendelse] dersom deserialiseringen feilet - eller null dersom hendelsen ikke skal lagres.
 */
fun String.tilNyTilbakekrevingshendelse(
    clock: Clock,
    // TODO jah: Den burde heller reflektert loggnivået
    erDev: Boolean,
    id: TilbakekrevinghendelseId = TilbakekrevinghendelseId.random(),
): Tilbakekrevingshendelse? {
    return Either.catch {
        deserialize<TilbakekrevingshendelseDTO>(this).tilHendelseForLagring(id)
    }.fold(
        ifLeft = { throwable ->
            if (erDev) {
                logger.info {
                    "Mottatt tilbakekrevingshendelse som vi ikke klarte å deserialisere - Lagrer som ukjent hendelse $id"
                }
            } else {
                logger.error(throwable) {
                    "Mottatt tilbakekrevingshendelse som vi ikke klarte å deserialisere - Lagrer som ukjent hendelse $id"
                }
            }
            TilbakekrevingUkjentHendelse(
                id = id,
                opprettet = nå(clock),
                value = this,
            )
        },
        ifRight = { it },
    )
}
