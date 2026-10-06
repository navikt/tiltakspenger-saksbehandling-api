package no.nav.tiltakspenger.saksbehandling.oppgave.infra.repo

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag.EndretTiltaksdeltakelse.Kilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakelseNåtilstandDbJson
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.tilNåtilstandDbJson
import java.time.LocalDateTime

/**
 * Grunnlaget skrives kun, og leses tilbake som rå json i [no.nav.tiltakspenger.saksbehandling.oppgave.LagretEksternOppgave].
 * Konvolutten (type og kilde) har et fast format.
 * Verdien er nå-tilstanden fra tiltakshistorikk i samme format som i `tiltaksdeltaker_endring`, se [TiltaksdeltakelseNåtilstandDbJson].
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = OppgavegrunnlagDb.EndretTiltaksdeltakelse::class, name = "ENDRET_TILTAKSDELTAKELSE"),
)
private sealed interface OppgavegrunnlagDb {
    class EndretTiltaksdeltakelse(
        val kilde: TiltaksdeltakelseKildeDb,
        val verdi: TiltaksdeltakelseNåtilstandDbJson,
    ) : OppgavegrunnlagDb
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TiltaksdeltakelseKildeDb.Tiltakshistorikk::class, name = "TILTAKSHISTORIKK"),
)
private sealed interface TiltaksdeltakelseKildeDb {
    class Tiltakshistorikk(val sisteUbehandletEndring: LocalDateTime) : TiltaksdeltakelseKildeDb
}

private fun Kilde.toDb(): TiltaksdeltakelseKildeDb = when (this) {
    is Kilde.Tiltakshistorikk -> TiltaksdeltakelseKildeDb.Tiltakshistorikk(sisteUbehandletEndring)
}

fun Oppgavegrunnlag.toDbJson(): String = serialize(
    when (this) {
        is Oppgavegrunnlag.EndretTiltaksdeltakelse -> OppgavegrunnlagDb.EndretTiltaksdeltakelse(
            kilde = kilde.toDb(),
            verdi = verdi.tilNåtilstandDbJson(),
        )
    },
)
