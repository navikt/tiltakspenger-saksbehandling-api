package no.nav.tiltakspenger.saksbehandling.oppgave.infra.repo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag.EndretTiltaksdeltakelse.Kilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFraRegister
import java.time.LocalDateTime

/**
 * Grunnlaget skrives kun, og leses tilbake som rå json i [no.nav.tiltakspenger.saksbehandling.oppgave.LagretEksternOppgave].
 * Konvolutten (type og kilde) har et fast format.
 * Verdien serialiseres slik den er, for sporbarhet, og formatet følger klassen — det er greit at det endrer seg over tid.
 * Et nytt avledet felt som ikke kan serialiseres, fanges av OppgavegrunnlagDbJsonTest, som bruker en verdi med alle felter satt.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = OppgavegrunnlagDb.EndretTiltaksdeltakelse::class, name = "ENDRET_TILTAKSDELTAKELSE"),
)
private sealed interface OppgavegrunnlagDb {
    class EndretTiltaksdeltakelse(
        val kilde: TiltaksdeltakelseKildeDb,
        // Avledede getter-verdier fra TiltaksdeltakelseLegacy er ikke data, og Periode kan ikke serialiseres.
        @get:JsonIgnoreProperties("kanInnvilges", "periode")
        val verdi: TiltaksdeltakelseFraRegister,
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
            verdi = verdi,
        )
    },
)
