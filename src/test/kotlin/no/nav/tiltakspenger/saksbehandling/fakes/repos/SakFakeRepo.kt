package no.nav.tiltakspenger.saksbehandling.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saker
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDate

class SakFakeRepo(
    private val behandlingRepo: BehandlingFakeRepo,
    private val rammevedtakRepo: RammevedtakFakeRepo,
    private val meldekortBehandlingRepo: MeldekortBehandlingFakeRepo,
    private val meldeperiodeRepo: MeldeperiodeFakeRepo,
    private val utbetalingsvedtakRepo: UtbetalingsvedtakFakeRepo,
    private val søknadFakeRepo: SøknadFakeRepo,
) : SakRepo {
    val data = Atomic(mutableMapOf<SakId, Sak>())

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
        val behandlinger = behandlingRepo.hentBehandlingerForSakId(sakId)
        val meldekortBehandlinger =
            meldekortBehandlingRepo.hentForSakId(sakId) ?: MeldekortBehandlinger.empty()
        val soknader = søknadFakeRepo.hentForSakId(sakId)

        return data.get()[sakId]?.copy(
            behandlinger = behandlinger,
            vedtaksliste = rammevedtakRepo.hentForSakId(sakId),
            meldekortBehandlinger = meldekortBehandlinger,
            utbetalinger = utbetalingsvedtakRepo.hentForSakId(sakId),
            meldeperiodeKjeder = meldeperiodeRepo.hentForSakId(sakId),
            soknader = soknader,
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

    override fun oppdaterFørsteOgSisteDagSomGirRett(
        sakId: SakId,
        førsteDagSomGirRett: LocalDate?,
        sisteDagSomGirRett: LocalDate?,
        sessionContext: SessionContext?,
    ) {
    }

    override fun hentSakerSomMåGenerereMeldeperioderFra(limit: Int): List<SakId> {
        data.get().mapNotNull {
            hentSak(it.key)
        }.filter {
            val sisteMeldeperiode = it.meldeperiodeKjeder.meldeperioder.last()
            sisteMeldeperiode.periode.tilOgMed < it.sisteDagSomGirRett
        }.map {
            it.id
        }.let {
            return it
        }
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr) {
        val sak = data.get().values.find { it.fnr == gammeltFnr }
        sak?.let {
            data.get()[it.id] = it.copy(
                fnr = nyttFnr,
            )
        }
    }

    override fun hentForSendingTilMeldekortApi(): List<Sak> {
        return emptyList()
    }

    override fun oppdaterSkalSendesTilMeldekortApi(
        sakId: SakId,
        skalSendesTilMeldekortApi: Boolean,
        sessionContext: SessionContext?,
    ) {
    }
}
