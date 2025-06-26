@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad

class SøknadFakeRepo(private val behandlingRepo: BehandlingFakeRepo) : SøknadRepo {
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

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, context: TransactionContext?) {
        val soknad = data.get().values.find { it.fnr == gammeltFnr }
        soknad?.let {
            data.get()[it.id] = it.copy(
                personopplysninger = it.personopplysninger.copy(
                    fnr = nyttFnr,
                ),
            )
        }
    }

    override fun hentAlleUbehandledeSoknader(limit: Int): List<Søknad> {
        val soknaderUtenBehandling = mutableListOf<Søknad>()
        val alleBehandlinger = behandlingRepo.alle.filterIsInstance<Søknadsbehandling>()
        val alleSoknader = data.get().values.toList()
        alleSoknader.forEach { soknad ->
            if (!soknad.erAvbrutt && alleBehandlinger.find { it.søknad.id == soknad.id } == null) {
                soknaderUtenBehandling.add(soknad)
            }
        }
        return soknaderUtenBehandling
    }

    fun hentForSakId(sakId: SakId): List<Søknad> {
        return data.get().filter { it.value.sakId == sakId }.values.toList()
    }
}
