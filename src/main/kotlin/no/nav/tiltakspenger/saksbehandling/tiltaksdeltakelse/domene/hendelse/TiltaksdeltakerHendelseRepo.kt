package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

/**
 * Lagrer mottatte hendelser om endringer i tiltaksdeltakelse, kun for sporbarhet til feilsøking og etterlevelse.
 * Hendelsene leses aldri tilbake til domeneklasser.
 * Behandling av endringer styres av markøren på tiltaksdeltakeren, ikke av det som lagres her.
 * Nå-tilstanden som hentes når en endring behandles, lagres i tiltakshistorikken.
 */
interface TiltaksdeltakerHendelseRepo {

    /** Lagrer en mottatt hendelse med meldingen ordrett som verdi. */
    fun lagre(
        tiltaksdeltakerHendelse: TiltaksdeltakerHendelse,
        melding: String,
        kilde: TiltaksdeltakerHendelseKilde,
        sessionContext: SessionContext,
    )
}
