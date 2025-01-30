package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.ports.SøknadRepo

class SøknadFakeRepo : SøknadRepo {
    private val data = Atomic(mutableMapOf<SøknadId, Søknad>())
    private val soknadSakKobling = Atomic(mutableMapOf<SøknadId, SakId>())

    val alle get() = data.get().values.toList()
    val alleSoknadSakKoblinger get() = soknadSakKobling.get().values.toList()

    override fun lagre(
        søknad: Søknad,
        sakId: SakId,
        txContext: TransactionContext?,
    ) {
        data.get()[søknad.id] = søknad
        soknadSakKobling.get()[søknad.id] = sakId
    }

    override fun hentForSøknadId(søknadId: SøknadId): Søknad = data.get()[søknadId]!!

    override fun hentSakIdForSoknad(søknadId: SøknadId): SakId = soknadSakKobling.get()[søknadId]!!

    fun hentForSakId(sakId: SakId): List<Søknad> {
        val soknadIder = soknadSakKobling.get().filter { it.value == sakId }.keys
        return data.get().filter { it.key in soknadIder }.values.toList()
    }
}
