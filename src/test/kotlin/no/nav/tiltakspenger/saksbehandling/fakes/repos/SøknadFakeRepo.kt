package no.nav.tiltakspenger.saksbehandling.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad

class SøknadFakeRepo : SøknadRepo {
    private val data = Atomic(mutableMapOf<SøknadId, Søknad>())

    val alle get() = data.get().values.toList()

    override fun lagre(
        søknad: Søknad,
        txContext: TransactionContext?,
    ) {
        data.get()[søknad.id] = søknad
    }

    override fun hentForSøknadId(søknadId: SøknadId): Søknad = data.get()[søknadId]!!

    override fun hentSakIdForSoknad(søknadId: SøknadId): SakId = data.get()[søknadId]!!.sakId

    override fun hentSøknaderForFnr(fnr: Fnr): List<Søknad> = data.get().values.filter { it.fnr == fnr }

    override fun finnSakIdForTiltaksdeltakelse(eksternId: String): SakId? {
        return null
    }

    override fun lagreAvbruttSøknad(søknad: Søknad, txContext: TransactionContext?) {
        data.get()[søknad.id] = søknad
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr) {
        val soknad = data.get().values.find { it.fnr == gammeltFnr }
        soknad?.let {
            data.get()[it.id] = it.copy(
                personopplysninger = it.personopplysninger.copy(
                    fnr = nyttFnr,
                ),
            )
        }
    }

    fun hentForSakId(sakId: SakId): List<Søknad> {
        return data.get().filter { it.value.sakId == sakId }.values.toList()
    }
}
