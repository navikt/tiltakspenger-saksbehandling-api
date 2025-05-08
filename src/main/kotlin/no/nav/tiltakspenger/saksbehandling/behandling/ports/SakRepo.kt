package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saker
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDate

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

    fun hentForSøknadId(søknadId: SøknadId): Sak?

    /**
     * @param sisteDagSomGirRett er ikke masterdata - bruk vedtak & tidslinje på sak for å finne sisteDagSomGirRett.
     *
     * Ment for å optimalisere db-spørringer (generering av meldeperioder)
     */
    fun oppdaterFørsteOgSisteDagSomGirRett(
        sakId: SakId,
        førsteDagSomGirRett: LocalDate?,
        sisteDagSomGirRett: LocalDate?,
        sessionContext: SessionContext? = null,
    )

    fun hentSakerSomMåGenerereMeldeperioderFra(ikkeGenererEtter: LocalDate, limit: Int = 1000): List<SakId>

    fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr)

    fun hentForSendingTilMeldekortApi(): List<Sak>

    fun oppdaterSkalSendesTilMeldekortApi(id: SakId, skalSendesTilMeldekortApi: Boolean)
}
