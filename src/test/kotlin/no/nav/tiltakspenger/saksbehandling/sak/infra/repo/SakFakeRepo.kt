package no.nav.tiltakspenger.saksbehandling.sak.infra.repo

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.RammebehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagevedtakFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.BrukersMeldekortFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortbehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeFakeRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saker
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadFakeRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingBehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.MeldekortvedtakFakeRepo
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakFakeRepo
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class SakFakeRepo(
    private val behandlingRepo: RammebehandlingFakeRepo,
    private val rammevedtakRepo: RammevedtakFakeRepo,
    private val meldekortbehandlingRepo: MeldekortbehandlingFakeRepo,
    private val meldeperiodeRepo: MeldeperiodeFakeRepo,
    private val meldekortvedtakRepo: MeldekortvedtakFakeRepo,
    private val klagevedtakRepo: KlagevedtakFakeRepo,
    private val søknadFakeRepo: SøknadFakeRepo,
    private val klagebehandlingFakeRepo: KlagebehandlingFakeRepo,
    private val brukersMeldekortFakeRepo: BrukersMeldekortFakeRepo,
    private val tilbakekrevingBehandlingFakeRepo: TilbakekrevingBehandlingFakeRepo,
    private val clock: Clock,
) : SakRepo {
    val data = Atomic(mutableMapOf<SakId, Sak>())
    val skalSendesTilMeldekortApi = Atomic(mutableSetOf<SakId>())

    override fun hentForFnr(fnr: Fnr): Saker = Saker(fnr, data.get().values.filter { it.fnr == fnr })

    override fun hentForSaksnummer(saksnummer: Saksnummer): Sak? {
        val sakId = data.get().values.find { it.saksnummer == saksnummer }?.id ?: return null
        return hentSak(sakId)
    }

    override fun opprettSak(sak: Sak) {
        data.get()[sak.id] = sak
    }

    override fun hentForSakId(sakId: SakId): Sak? {
        return hentSak(sakId)
    }

    private fun hentSak(
        sakId: SakId,
    ): Sak? {
        val rammebehandlinger: Rammebehandlinger = behandlingRepo.hentRammebehandlingerForSakId(sakId)
        val meldekortbehandlinger =
            meldekortbehandlingRepo.hentForSakId(sakId) ?: Meldekortbehandlinger.empty()
        val soknader = søknadFakeRepo.hentForSakId(sakId)
        val klagebehandlinger = klagebehandlingFakeRepo.hentForSakId(sakId)

        return data.get()[sakId]?.copy(
            behandlinger = Behandlinger(
                rammebehandlinger = rammebehandlinger,
                meldekortbehandlinger = meldekortbehandlinger,
                klagebehandlinger = klagebehandlinger,
            ),
            vedtaksliste = Vedtaksliste(
                rammevedtaksliste = rammevedtakRepo.hentForSakId(sakId),
                meldekortvedtaksliste = meldekortvedtakRepo.hentForSakId(sakId),
                klagevedtaksliste = klagevedtakRepo.hentForSakId(sakId),
            ),
            meldeperiodeKjeder = meldeperiodeRepo.hentForSakId(sakId),
            brukersMeldekort = brukersMeldekortFakeRepo.hentForSakId(sakId),
            søknader = soknader,
            tilbakekrevinger = tilbakekrevingBehandlingFakeRepo.hentForSakId(sakId),
        )
    }

    override fun hentNesteSaksnummer(): Saksnummer =
        data
            .get()
            .values
            .map { it.saksnummer }
            .lastOrNull()
            ?.nesteSaksnummer()
            ?: Saksnummer.genererSaknummer(dato = LocalDate.now(clock), løpenr = "1001")

    override fun hentFnrForSaksnummer(saksnummer: Saksnummer, sessionContext: SessionContext?): Fnr? {
        return data.get().values.singleOrNull { it.saksnummer == saksnummer }?.fnr
    }

    override fun hentFnrForSakId(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): Fnr? {
        return data.get()[sakId]?.fnr
    }

    override fun hentSakIdForSaksnummer(
        saksnummer: Saksnummer,
        sessionContext: SessionContext?,
    ): SakId? {
        return data.get().values.singleOrNull { it.saksnummer == saksnummer }?.id
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, context: TransactionContext?) {
        val sak = data.get().values.find { it.fnr == gammeltFnr }
        sak?.let {
            data.get()[it.id] = it.copy(
                fnr = nyttFnr,
            )
        }
    }

    override fun hentForSendingTilMeldekortApi(limit: Int): List<Sak> {
        return skalSendesTilMeldekortApi.get().mapNotNull { hentSak(it) }.take(limit)
    }

    override fun hentForSendingAvMeldeperioderTilDatadeling(limit: Int): List<Sak> {
        return emptyList()
    }

    override fun markerSkalSendesTilMeldekortApi(
        sakId: SakId,
        sessionContext: SessionContext?,
    ) {
    }

    override fun markerErSendtTilMeldekortApi(
        sakId: SakId,
        nyesteVedtakOpprettet: LocalDateTime?,
        sessionContext: SessionContext?,
    ): Boolean {
        return true
    }

    override fun oppdaterSkalSendeMeldeperioderTilDatadeling(
        sakId: SakId,
        skalSendeMeldeperioderTilDatadeling: Boolean,
        sessionContext: SessionContext?,
    ) {
    }

    override fun oppdaterSkalSendeMeldeperioderTilDatadelingOgSkalSendesTilMeldekortApi(
        sakId: SakId,
        skalSendesTilMeldekortApi: Boolean,
        skalSendeMeldeperioderTilDatadeling: Boolean,
        sessionContext: SessionContext?,
    ) {
    }

    override fun oppdaterKanSendeInnHelgForMeldekort(
        sakId: SakId,
        kanSendeInnHelgForMeldekort: Boolean,
        sessionContext: SessionContext?,
    ) {
        data.get()[sakId] = data.get()[sakId]!!.copy(
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
        )
    }

    override fun hentSakerTilDatadeling(limit: Int): List<SakDb> {
        return emptyList()
    }

    override fun markerSendtTilDatadeling(
        id: SakId,
        tidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
    }
}
