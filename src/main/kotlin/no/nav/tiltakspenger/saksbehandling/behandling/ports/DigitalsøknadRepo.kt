package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.søknad.Digitalsøknad

interface DigitalsøknadRepo {
    fun lagre(
        søknad: Digitalsøknad,
        txContext: TransactionContext? = null,
    )

    fun hentForSøknadId(søknadId: SøknadId): Digitalsøknad?

    fun hentSakIdForSoknad(søknadId: SøknadId): SakId?

    fun hentSøknaderForFnr(fnr: Fnr): List<Digitalsøknad>

    fun finnSakIdForTiltaksdeltakelse(eksternId: String): SakId?

    fun lagreAvbruttSøknad(søknad: Digitalsøknad, txContext: TransactionContext? = null)

    fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, context: TransactionContext? = null)

    fun hentAlleUbehandledeSoknader(limit: Int = 10): List<Digitalsøknad>
}
