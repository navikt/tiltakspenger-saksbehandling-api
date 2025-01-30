package no.nav.tiltakspenger.saksbehandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saker
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.sak.TynnSak

interface SakRepo {
    fun hentForFnr(fnr: Fnr): Saker

    fun hentForSaksnummer(saksnummer: Saksnummer): Sak?

    /**
     * Denne er kun tenkt kalt når man mottar en søknad og det ikke finnes noen sak fra før.
     * Senere endringer på saken gjøres via methoder og repoer.
     */
    fun opprettSak(sak: Sak)

    fun hentForSakId(sakId: SakId): Sak?

    fun hentDetaljerForSakId(sakId: SakId): TynnSak?

    fun hentNesteSaksnummer(): Saksnummer

    fun hentFnrForSaksnummer(
        saksnummer: Saksnummer,
        sessionContext: SessionContext? = null,
    ): Fnr?

    fun hentFnrForSakId(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): Fnr?

    fun hentForSøknadId(søknadId: SøknadId): Sak?
}
