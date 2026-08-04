@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.RammebehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.søknad.domene.IkkeInnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

class SøknadFakeRepo(private val behandlingRepo: RammebehandlingFakeRepo) : SøknadRepo {
    private val data = Atomic(mutableMapOf<SøknadId, Søknad>())

    val alle get() = data.get().values.toList()

    override fun lagre(
        søknad: Søknad,
        txContext: TransactionContext?,
    ) {
        data.get()[søknad.id] = søknad
    }

    fun hentForSøknadId(søknadId: SøknadId): Søknad = data.get()[søknadId]!!

    override fun finnSakIdForTiltaksdeltakelse(tiltaksdeltakerId: TiltaksdeltakerId): SakId? {
        return null
    }

    override fun lagreAvbruttSøknad(søknad: Søknad, txContext: TransactionContext?) {
        data.get()[søknad.id] = søknad
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, context: TransactionContext?) {
        val soknad = data.get().values.find { it.fnr == gammeltFnr }
        soknad?.let {
            val personopplysninger = it.personopplysninger.copy(fnr = nyttFnr)

            when (soknad) {
                is InnvilgbarSøknad -> data.get()[it.id] = soknad.copy(personopplysninger = personopplysninger)
                is IkkeInnvilgbarSøknad -> data.get()[it.id] = soknad.copy(personopplysninger = personopplysninger)
            }
        }
    }

    private fun hentAlleUbehandledeSoknader(): List<InnvilgbarSøknad> {
        val soknaderUtenBehandling = mutableListOf<InnvilgbarSøknad>()
        val alleBehandlinger = behandlingRepo.alle.filterIsInstance<Søknadsbehandling>()
        val alleSoknader = data.get().values.toList().filterIsInstance<InnvilgbarSøknad>()
        alleSoknader.forEach { soknad ->
            if (!soknad.erAvbrutt && alleBehandlinger.find { it.søknad.id == soknad.id } == null) {
                soknaderUtenBehandling.add(soknad)
            }
        }
        return soknaderUtenBehandling
    }

    override fun hentUbehandledeSøknadIder(limit: Int): List<SøknadId> {
        return hentAlleUbehandledeSoknader().take(limit).map { it.id }
    }

    override fun hentUbehandletSøknad(søknadId: SøknadId): InnvilgbarSøknad? {
        return hentAlleUbehandledeSoknader().find { it.id == søknadId }
    }

    fun hentForSakId(sakId: SakId): List<Søknad> {
        return data.get().filter { it.value.sakId == sakId }.values.toList()
    }
}
