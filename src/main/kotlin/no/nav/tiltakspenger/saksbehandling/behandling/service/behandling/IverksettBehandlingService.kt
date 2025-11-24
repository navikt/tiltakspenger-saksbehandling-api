package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeIverksetteBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.genererStønadsstatistikkForRammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.opprettVedtak
import java.time.Clock

class IverksettBehandlingService(
    private val behandlingRepo: BehandlingRepo,
    private val rammevedtakRepo: RammevedtakRepo,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val sessionFactory: SessionFactory,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val statistikkStønadRepo: StatistikkStønadRepo,
    private val sakService: SakService,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
) {
    suspend fun iverksett(
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        sakId: SakId,
    ): Either<KanIkkeIverksetteBehandling, Pair<Sak, Rammebehandling>> {
        val sak = sakService.hentForSakId(sakId)
        val behandling = sak.hentRammebehandling(behandlingId)!!

        if (behandling.beslutter != beslutter.navIdent) {
            // TODO jah: Fjern denne feilen? Skal vel mye til at denne skjer i praksis?
            return KanIkkeIverksetteBehandling.BehandlingenEiesAvAnnenBeslutter(
                eiesAvBeslutter = behandling.beslutter,
            ).left()
        }

        val attestering = Attestering(
            status = Attesteringsstatus.GODKJENT,
            begrunnelse = null,
            beslutter = beslutter.navIdent,
            tidspunkt = nå(clock),
        )
        // Denne validerer saksbehandler
        val iverksattBehandling = behandling.iverksett(beslutter, attestering, clock)

        val (oppdatertSak, vedtak) = sak.opprettVedtak(iverksattBehandling, clock)

        val sakStatistikk = statistikkSakService.genererStatistikkForRammevedtak(
            rammevedtak = vedtak,
        )
        val stønadStatistikk = if (vedtak.resultat is SøknadsbehandlingResultat.Avslag) {
            null
        } else {
            genererStønadsstatistikkForRammevedtak(vedtak)
        }

        when (behandling) {
            is Revurdering -> oppdatertSak.iverksett(
                vedtak = vedtak,
                sakStatistikk = sakStatistikk,
                stønadStatistikk = stønadStatistikk!!,
            )

            is Søknadsbehandling -> oppdatertSak.iverksettSøknadsbehandling(
                vedtak = vedtak,
                sakStatistikk = sakStatistikk,
                stønadStatistikk = stønadStatistikk,
            )
        }

        return (oppdatertSak to iverksattBehandling).right()
    }

    private fun Sak.iverksettSøknadsbehandling(
        vedtak: Rammevedtak,
        sakStatistikk: StatistikkSakDTO,
        stønadStatistikk: StatistikkStønadDTO?,
    ): Sak {
        return when (vedtak.resultat) {
            is BehandlingResultat.Innvilgelse -> iverksett(vedtak, sakStatistikk, stønadStatistikk!!)

            is SøknadsbehandlingResultat.Avslag -> {
                // journalføring og dokumentdistribusjon skjer i egen jobb
                sessionFactory.withTransactionContext { tx ->
                    behandlingRepo.lagre(vedtak.behandling, tx)
                    sakService.oppdaterSkalSendesTilMeldekortApi(
                        sakId = this.id,
                        skalSendesTilMeldekortApi = true,
                        sessionContext = tx,
                    )
                    rammevedtakRepo.lagre(vedtak, tx)
                    statistikkSakRepo.lagre(sakStatistikk, tx)
                }
                this
            }

            is RevurderingResultat.Stans -> throw IllegalArgumentException("Kan ikke iverksette stans-vedtak på en søknadsbehandling")
        }
    }

    private fun Sak.iverksett(
        vedtak: Rammevedtak,
        sakStatistikk: StatistikkSakDTO,
        stønadStatistikk: StatistikkStønadDTO,
    ): Sak {
        require(vedtak.resultat is BehandlingResultat.Innvilgelse || vedtak.resultat is RevurderingResultat.Stans) {
            "Kan kun iverksette innvilgelse eller stans"
        }

        val (oppdatertSak, oppdaterteMeldeperioder) = this.genererMeldeperioder(clock)
        val (oppdaterteMeldekortbehandlinger, oppdaterteMeldekort) =
            this.meldekortbehandlinger.oppdaterMedNyeKjeder(oppdatertSak.meldeperiodeKjeder, tiltakstypeperioder, clock)

        // journalføring og dokumentdistribusjon skjer i egen jobb
        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(vedtak.behandling, tx)
            sakService.oppdaterSkalSendeMeldeperioderTilDatadelingOgSkalSendesTilMeldekortApi(
                sakId = oppdatertSak.id,
                skalSendesTilMeldekortApi = true,
                skalSendeMeldeperioderTilDatadeling = true,
                sessionContext = tx,
            )
            rammevedtakRepo.lagre(vedtak, tx)
            statistikkSakRepo.lagre(sakStatistikk, tx)
            statistikkStønadRepo.lagre(stønadStatistikk, tx)
            meldeperiodeRepo.lagre(oppdaterteMeldeperioder, tx)
            // Merk at simuleringen vil nulles ut her. Gjelder kun åpne meldekortbehandlinger.
            oppdaterteMeldekort.forEach { meldekortBehandlingRepo.oppdater(it, null, tx) }

            if (vedtak.omgjørRammevedtak != null) {
                // Oppdaterer omgjort vedtak slik at det peker på dette vedtaket
                rammevedtakRepo.markerOmgjortAv(vedtak.omgjørRammevedtak.id, vedtak.id, tx)
            }
        }
        return oppdatertSak.oppdaterMeldekortbehandlinger(oppdaterteMeldekortbehandlinger)
    }
}
