package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad

interface SøknadRepo {
    fun lagre(
        søknad: Søknad,
        txContext: TransactionContext? = null,
    )

    fun hentForSøknadId(søknadId: SøknadId): Søknad?

    fun hentSakIdForSoknad(søknadId: SøknadId): SakId?

    fun hentSøknaderForFnr(fnr: Fnr): List<Søknad>

    fun finnSakIdForTiltaksdeltakelse(eksternId: String): SakId?

    fun lagreAvbruttSøknad(søknad: Søknad, txContext: TransactionContext? = null)
}
