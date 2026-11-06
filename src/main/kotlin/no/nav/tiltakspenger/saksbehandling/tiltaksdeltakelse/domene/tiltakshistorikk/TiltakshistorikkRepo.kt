package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/** Tiltakshistorikken som er hentet for tiltaksdeltakere når endringer i deltakelsen behandles. */
interface TiltakshistorikkRepo {

    fun lagre(innslag: Tiltakshistorikkinnslag, sessionContext: SessionContext)

    /**
     * Verdien slik den ble lagret i siste innslag for deltakeren.
     * Tittel og arrangør lagres ikke, og er derfor alltid ukjente.
     * Er null når deltakeren ikke har noen innslag.
     */
    fun hentSisteVerdi(
        tiltaksdeltakerId: TiltaksdeltakerId,
        sessionContext: SessionContext,
    ): Tiltaksdeltakelse.GirRett?
}
