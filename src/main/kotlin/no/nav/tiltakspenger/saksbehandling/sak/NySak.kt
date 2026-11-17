package no.nav.tiltakspenger.saksbehandling.sak

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste

/**
 * En tom sak som ikke er lagret ennå, med versjonen av Hel Veds grensesnitt utbetalingene i saken skal gå gjennom.
 */
data class NySak(
    val sak: Sak,
    val oppdragsversjon: Oppdragsversjon,
) {
    companion object {
        /**
         * @param oppdragsversjonForNyeSaker versjonen miljøet gir nye saker.
         * @param valgtOppdragsversjon overstyrer [oppdragsversjonForNyeSaker], og skal bare settes utenfor prod.
         */
        fun opprett(
            fnr: Fnr,
            saksnummer: Saksnummer,
            oppdragsversjonForNyeSaker: Oppdragsversjon,
            valgtOppdragsversjon: Oppdragsversjon?,
        ): NySak = NySak(
            sak = Sak(
                id = SakId.random(),
                fnr = fnr,
                saksnummer = saksnummer,
                behandlinger = Behandlinger.empty(),
                vedtaksliste = Vedtaksliste.empty(),
                meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
                brukersMeldekort = emptyList(),
                søknader = emptyList(),
                tilbakekrevinger = emptyList(),
                kanSendeInnHelgForMeldekort = false,
                tiltakshistorikk = emptyList(),
            ),
            oppdragsversjon = valgtOppdragsversjon ?: oppdragsversjonForNyeSaker,
        )
    }
}
