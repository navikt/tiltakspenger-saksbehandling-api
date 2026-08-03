package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

interface SøknadRepo {
    fun lagre(
        søknad: Søknad,
        txContext: TransactionContext? = null,
    )

    fun finnSakIdForTiltaksdeltakelse(tiltaksdeltakerId: TiltaksdeltakerId): SakId?

    /**
     * Avbrytingen skjer alltid sammen med avbrytingen av rammebehandlingen, så [txContext] har ingen default.
     * Se «Ingen defaults i prod for testenes skyld» i AGENTS-backend.md.
     * TODO jah: Siden/hvis denne eies av behandlingen, bør den lagres sammen med behandlingen fra RammebehandlingPostgresRepo via en companion object-funksjon i SøknadPostgresRepo.
     *  Slett så dette interfacet.
     */
    fun lagreAvbruttSøknad(søknad: Søknad, txContext: TransactionContext?)

    fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, context: TransactionContext?)

    fun hentUbehandledeSøknadIder(limit: Int): List<SøknadId>

    /** Henter søknaden kun dersom den fortsatt er ubehandlet (digital, ikke avbrutt og uten behandling). */
    fun hentUbehandletSøknad(søknadId: SøknadId): InnvilgbarSøknad?
}
