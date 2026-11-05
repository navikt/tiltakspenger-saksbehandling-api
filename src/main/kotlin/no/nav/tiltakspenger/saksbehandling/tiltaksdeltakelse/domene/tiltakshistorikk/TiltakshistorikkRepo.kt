package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/** Tiltakshistorikken som er hentet for tiltaksdeltakere når endringer i deltakelsen behandles. */
interface TiltakshistorikkRepo {

    fun lagre(innslag: Tiltakshistorikkinnslag, sessionContext: SessionContext)

    /**
     * Sammenligner med verdien slik den ble lagret i siste innslag for deltakeren.
     * Felter som ikke lagres, som tittel og arrangør, regnes derfor ikke som endringer.
     * Er false når deltakeren ikke har noen innslag.
     */
    fun erLikSisteVerdi(
        tiltaksdeltakerId: TiltaksdeltakerId,
        verdi: Tiltaksdeltakelse.GirRett,
        sessionContext: SessionContext,
    ): Boolean
}
