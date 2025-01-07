package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saker
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.sak.TynnSak
import no.nav.tiltakspenger.saksbehandling.ports.SakRepo
import java.time.LocalDate

class SakFakeRepo(
    private val behandlingRepo: BehandlingFakeRepo,
    private val rammevedtakRepo: RammevedtakFakeRepo,
    private val meldekortRepo: MeldekortFakeRepo,
    private val utbetalingsvedtakRepo: UtbetalingsvedtakFakeRepo,
) : SakRepo {
    val data = Atomic(mutableMapOf<SakId, Sak>())

    override fun hentForFnr(fnr: Fnr): Saker = Saker(fnr, data.get().values.filter { it.fnr == fnr })

    override fun hentForSaksnummer(saksnummer: Saksnummer): Sak? {
        val sakId = data.get().values.find { it.saksnummer == saksnummer }?.id ?: return null
        return hentSak(sakId)
    }

    override fun opprettSakOgFørstegangsbehandling(
        sak: Sak,
        transactionContext: TransactionContext?,
    ) {
        data.get()[sak.id] = sak
        behandlingRepo.lagre(sak.førstegangsbehandling)
    }

    override fun hentForSakId(sakId: SakId): Sak? {
        return hentSak(sakId)
    }

    fun hentForBehandlingId(behandlingId: BehandlingId): Sak? {
        val sakId = data.get().values.find { it.behandlinger.any { it.id == behandlingId } }?.id ?: return null
        return hentSak(sakId)
    }

    private fun hentSak(
        sakId: SakId,
    ): Sak? {
        val behandlinger = behandlingRepo.hentBehandlingerForSakId(sakId)
        return data.get()[sakId]?.copy(
            behandlinger = behandlinger,
            vedtaksliste = rammevedtakRepo.hentForSakId(sakId),
            meldekortBehandlinger = meldekortRepo.hentForSakId(sakId) ?: MeldekortBehandlinger.empty(behandlinger.first().tiltakstype),
            utbetalinger = utbetalingsvedtakRepo.hentForSakId(sakId),
        )
    }

    override fun hentDetaljerForSakId(sakId: SakId): TynnSak? =
        data.get()[sakId]?.let {
            TynnSak(
                id = it.id,
                fnr = it.fnr,
                saksnummer = it.saksnummer,
            )
        }

    override fun hentNesteSaksnummer(): Saksnummer =
        data
            .get()
            .values
            .map { it.saksnummer }
            .lastOrNull()
            ?.nesteSaksnummer()
            ?: Saksnummer.genererSaknummer(dato = LocalDate.now(), løpenr = "1001")

    override fun hentFnrForSaksnummer(saksnummer: Saksnummer, sessionContext: SessionContext?): Fnr? {
        return data.get().values.singleOrNull { it.saksnummer == saksnummer }?.fnr
    }

    override fun hentFnrForSakId(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): Fnr? {
        return data.get()[sakId]?.fnr
    }

    override fun hentForSøknadId(søknadId: SøknadId): Sak? {
        val sakId = data.get().values.find {
            it.behandlinger.any { behandling -> behandling.søknad?.id == søknadId }
        }?.id ?: return null
        return hentSak(sakId)
    }
}
