package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saker
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

interface SakRepo {
    fun hentForFnr(fnr: Fnr): Saker

    fun hentForSaksnummer(saksnummer: Saksnummer): Sak?

    /**
     * Denne er kun tenkt kalt når man mottar en søknad og det ikke finnes noen sak fra før.
     * Senere endringer på saken gjøres via methoder og repoer.
     */
    fun opprettSak(sak: Sak)

    fun hentForSakId(sakId: SakId): Sak?

    fun hentNesteSaksnummer(): Saksnummer

    fun hentFnrForSaksnummer(
        saksnummer: Saksnummer,
        sessionContext: SessionContext? = null,
    ): Fnr?

    fun hentFnrForSakId(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): Fnr?

    fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr)

    fun hentForSendingTilMeldekortApi(): List<Sak>

    fun oppdaterSkalSendesTilMeldekortApi(
        sakId: SakId,
        skalSendesTilMeldekortApi: Boolean,
        sessionContext: SessionContext? = null,
    )
}
