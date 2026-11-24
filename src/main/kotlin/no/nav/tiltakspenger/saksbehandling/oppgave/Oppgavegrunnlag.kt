package no.nav.tiltakspenger.saksbehandling.oppgave

import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import java.time.LocalDateTime

/**
 * Grunnlaget lagres kun for sporbarhet og feilsøking.
 * Det leses aldri tilbake til domenet, se [LagretEksternOppgave.grunnlag].
 */
sealed interface Oppgavegrunnlag {
    /**
     * [verdi] er nå-tilstanden fra tiltakshistorikk som førte til oppgaven.
     * Slike oppgaver opprettes ikke lenger, men typen beskriver eksisterende rader.
     */
    data class EndretTiltaksdeltakelse(
        val kilde: Kilde,
        val verdi: Tiltaksdeltakelse.GirRett,
    ) : Oppgavegrunnlag {
        sealed interface Kilde {
            /**
             * Øyeblikksbildet er nå-tilstanden hentet fra tiltakshistorikk-tjenesten.
             * [sisteUbehandletEndring] er markøren på deltakeren som ble behandlet, altså tidspunktet for siste mottatte hendelse.
             */
            data class Tiltakshistorikk(val sisteUbehandletEndring: LocalDateTime) : Kilde
        }
    }
}
