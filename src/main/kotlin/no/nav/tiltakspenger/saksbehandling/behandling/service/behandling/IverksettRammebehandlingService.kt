package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeIverksetteBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.OppdaterBeregningOgSimuleringService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.genererStønadsstatistikkForRammevedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.opprettRammevedtak
import java.time.Clock

class IverksettRammebehandlingService(
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val rammevedtakRepo: RammevedtakRepo,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val sessionFactory: SessionFactory,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val statistikkStønadRepo: StatistikkStønadRepo,
    private val sakService: SakService,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val oppdaterBeregningOgSimuleringService: OppdaterBeregningOgSimuleringService,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun iverksettRammebehandling(
        rammebehandlingId: BehandlingId,
        beslutter: Saksbehandler,
        sakId: SakId,
        correlationId: CorrelationId,
    ): Either<KanIkkeIverksetteBehandling, Pair<Sak, Rammebehandling>> {
        val sak: Sak = sakService.hentForSakId(sakId)
        // TODO jah: Mye som kan flyttes ut av service her
        val behandling: Rammebehandling = sak.hentRammebehandling(rammebehandlingId)!!

        if (behandling.resultat is Omgjøringsresultat.OmgjøringOpphør && Configuration.isProd()) {
            throw IllegalArgumentException("Iverksetting av omgjøring til opphør er ikke aktivert i prod")
        }

        if (behandling.beslutter != beslutter.navIdent) {
            return KanIkkeIverksetteBehandling.BehandlingenEiesAvAnnenBeslutter(
                eiesAvBeslutter = behandling.beslutter,
            ).left()
        }

        val (_, behandlingMedOppdatertSimulering) = oppdaterBeregningOgSimuleringService.hentOppdatertBeregningOgSimuleringForRammebehandling(
            sak,
            rammebehandlingId,
            beslutter,
        ).getOrElse {
            return KanIkkeIverksetteBehandling.SimuleringFeilet(it).left()
        }

        behandlingMedOppdatertSimulering.utbetaling?.also { utbetaling ->
            utbetaling.validerKanIverksetteUtbetaling(behandling.utbetaling).onLeft {
                logger.error { "Utbetaling på behandlingen har et resultat som vi ikke kan iverksette - $rammebehandlingId / $it" }
                return KanIkkeIverksetteBehandling.UtbetalingStøttesIkke(it).left()
            }
        }

        val attestering = Attestering(
            status = Attesteringsstatus.GODKJENT,
            begrunnelse = null,
            beslutter = beslutter.navIdent,
            tidspunkt = nå(clock),
        )
        // Denne validerer saksbehandler
        val iverksattRammebehandling = behandling.iverksett(
            utøvendeBeslutter = beslutter,
            attestering = attestering,
            correlationId = correlationId,
            clock = clock,
        )
        val (oppdatertSak, vedtak) = sak.opprettRammevedtak(iverksattRammebehandling, clock)

        val sakStatistikk = statistikkSakService.genererStatistikkForRammevedtak(
            rammevedtak = vedtak,
        )
        val stønadStatistikk = if (vedtak.rammebehandlingsresultat is Søknadsbehandlingsresultat.Avslag) {
            null
        } else {
            genererStønadsstatistikkForRammevedtak(vedtak)
        }
        val doubleOppdatertSak = when (behandling) {
            is Revurdering -> oppdatertSak.iverksettRammebehandling(
                rammevedtak = vedtak,
                sakStatistikk = sakStatistikk,
                stønadStatistikk = stønadStatistikk!!,
            )

            is Søknadsbehandling -> oppdatertSak.iverksettSøknadsbehandling(
                rammevedtak = vedtak,
                sakStatistikk = sakStatistikk,
                stønadStatistikk = stønadStatistikk,
            )
        }

        return (doubleOppdatertSak to iverksattRammebehandling).right()
    }

    private fun Sak.iverksettSøknadsbehandling(
        rammevedtak: Rammevedtak,
        sakStatistikk: StatistikkSakDTO,
        stønadStatistikk: StatistikkStønadDTO?,
    ): Sak {
        return when (rammevedtak.rammebehandlingsresultat) {
            is Søknadsbehandlingsresultat.Innvilgelse -> this.iverksettRammebehandling(
                rammevedtak,
                sakStatistikk,
                stønadStatistikk!!,
            )

            is Søknadsbehandlingsresultat.Avslag -> {
                // journalføring og dokumentdistribusjon skjer i egen jobb
                sessionFactory.withTransactionContext { tx ->
                    // Obs: Dersom du endrer eller legger til noe her som angår klage, merk at du må gjøre tilsvarende i [no.nav.tiltakspenger.saksbehandling.klage.service.IverksettKlagebehandlingService]
                    rammebehandlingRepo.lagre(rammevedtak.rammebehandling, tx)
                    sakService.oppdaterSkalSendesTilMeldekortApi(
                        sakId = this.id,
                        skalSendesTilMeldekortApi = true,
                        sessionContext = tx,
                    )
                    rammevedtakRepo.lagre(rammevedtak, tx)
                    statistikkSakRepo.lagre(sakStatistikk, tx)
                    // TODO jah: Å gjøre om withTransactionContext til suspend function er målet, men krever noen dagers arbeid
                    runBlocking {
                        tx.onSuccess { MetricRegister.IVERKSATT_BEHANDLING.inc() }
                    }
                }
                this
            }

            is Revurderingsresultat -> throw IllegalArgumentException("Kan ikke iverksette revurdering-resultat på en søknadsbehandling")
        }
    }

    private fun Sak.iverksettRammebehandling(
        rammevedtak: Rammevedtak,
        sakStatistikk: StatistikkSakDTO,
        stønadStatistikk: StatistikkStønadDTO,
    ): Sak {
        when (rammevedtak.rammebehandlingsresultat) {
            is Omgjøringsresultat.OmgjøringIkkeValgt,
            is Søknadsbehandlingsresultat.Avslag,
            -> throw IllegalArgumentException("Kan ikke iverksette en behandling med resultat ${rammevedtak.rammebehandlingsresultat.tilRammebehandlingResultatTypeDTO()}")

            is Omgjøringsresultat.OmgjøringInnvilgelse,
            is Revurderingsresultat.Innvilgelse,
            is Omgjøringsresultat.OmgjøringOpphør,
            is Revurderingsresultat.Stans,
            is Søknadsbehandlingsresultat.Innvilgelse,
            -> Unit
        }

        require(this.rammevedtaksliste.last().id == rammevedtak.id) {
            "Vedtaket som iverksettes må være siste vedtak på saken (forventet at ${rammevedtak.id} skal være siste vedtak på ${this.id})"
        }

        val (sakOppdatertMedMeldeperioder, oppdaterteMeldeperioder) = this.genererMeldeperioder(clock)
        val (oppdaterteMeldekortbehandlinger, oppdaterteMeldekort) = this.meldekortbehandlinger.oppdaterMedNyeKjeder(
            oppdaterteKjeder = sakOppdatertMedMeldeperioder.meldeperiodeKjeder,
            tiltakstypePerioder = tiltakstypeperioder,
            clock = clock,
        )
        val sakOppdatertMedMeldekortbehandlinger =
            sakOppdatertMedMeldeperioder.oppdaterMeldekortbehandlinger(oppdaterteMeldekortbehandlinger)

        val tidligereVedtak = sakOppdatertMedMeldekortbehandlinger.rammevedtaksliste

        // journalføring og dokumentdistribusjon skjer i egen jobb
        sessionFactory.withTransactionContext { tx ->
            // Obs: Dersom du endrer eller legger til noe her som angår klage, merk at du må gjøre tilsvarende i [no.nav.tiltakspenger.saksbehandling.klage.service.IverksettKlagebehandlingService]
            rammebehandlingRepo.lagre(rammevedtak.rammebehandling, tx)
            sakService.oppdaterSkalSendeMeldeperioderTilDatadelingOgSkalSendesTilMeldekortApi(
                sakId = sakOppdatertMedMeldeperioder.id,
                skalSendesTilMeldekortApi = true,
                skalSendeMeldeperioderTilDatadeling = true,
                sessionContext = tx,
            )
            rammevedtakRepo.lagre(rammevedtak, tx)
            statistikkSakRepo.lagre(sakStatistikk, tx)
            statistikkStønadRepo.lagre(stønadStatistikk, tx)
            meldeperiodeRepo.lagre(oppdaterteMeldeperioder, tx)
            // Merk at simuleringen vil nulles ut her. Gjelder kun åpne meldekortbehandlinger.
            oppdaterteMeldekort.forEach { meldekortBehandlingRepo.oppdater(it, null, tx) }

            tidligereVedtak.forEach {
                rammevedtakRepo.oppdaterOmgjortAv(
                    it.id,
                    it.omgjortAvRammevedtak,
                    tx,
                )
            }
            // TODO jah: Å gjøre om withTransactionContext til suspend function er målet, men krever noen dagers arbeid
            runBlocking {
                tx.onSuccess { MetricRegister.IVERKSATT_BEHANDLING.inc() }
            }
        }
        return sakOppdatertMedMeldekortbehandlinger
    }
}
