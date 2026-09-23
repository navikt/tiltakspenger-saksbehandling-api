package no.nav.tiltakspenger.saksbehandling.oppgave

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFraRegister
import java.time.LocalDateTime

/**
 * Grunnlaget lagres kun for sporbarhet og feilsøking.
 * Det leses aldri tilbake til domenet, se [LagretEksternOppgave.grunnlag].
 */
sealed interface Oppgavegrunnlag {
    /**
     * [verdi] er nå-tilstanden fra tiltakshistorikk som førte til oppgaven.
     */
    data class EndretTiltaksdeltakelse(
        val kilde: Kilde,
        val verdi: TiltaksdeltakelseFraRegister,
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
