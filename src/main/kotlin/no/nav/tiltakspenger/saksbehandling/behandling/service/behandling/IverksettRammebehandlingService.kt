package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
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
import no.nav.tiltakspenger.saksbehandling.behandling.service.OppdaterBeregningOgSimuleringService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.opprettRammevedtak
import java.time.Clock

class IverksettRammebehandlingService(
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val rammevedtakRepo: RammevedtakRepo,
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val sessionFactory: SessionFactory,
    private val sakService: SakService,
    private val clock: Clock,
    private val oppdaterBeregningOgSimuleringService: OppdaterBeregningOgSimuleringService,
    private val statistikkService: StatistikkService,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun iverksettRammebehandling(
        rammebehandlingId: RammebehandlingId,
        beslutter: Saksbehandler,
        sakId: SakId,
        correlationId: CorrelationId,
    ): Either<KanIkkeIverksetteBehandling, Pair<Sak, Rammebehandling>> {
        val sak: Sak = sakService.hentForSakId(sakId)
        // TODO jah: Mye som kan flyttes ut av service her
        val behandling: Rammebehandling = sak.hentRammebehandling(rammebehandlingId)!!

        if (behandling.beslutter != beslutter.navIdent) {
            return KanIkkeIverksetteBehandling.BehandlingenEiesAvAnnenBeslutter(
                eiesAvBeslutter = behandling.beslutter,
            ).left()
        }

        val (_, behandlingMedUtbetalingskontroll) = oppdaterBeregningOgSimuleringService.oppdaterUtbetalingskontroll(
            sak,
            rammebehandlingId,
            beslutter,
        ).getOrElse {
            return KanIkkeIverksetteBehandling.SimuleringFeil(it).left()
        }

        behandlingMedUtbetalingskontroll.validerKanIverksetteUtbetaling().onLeft {
            logger.error { "Utbetaling på behandlingen har et resultat som vi ikke kan iverksette - $rammebehandlingId / $it" }
            rammebehandlingRepo.lagre(behandlingMedUtbetalingskontroll)
            val oppdaterSak = sak.oppdaterRammebehandling(behandlingMedUtbetalingskontroll)

            return KanIkkeIverksetteBehandling.UtbetalingFeil(
                it,
                oppdaterSak,
                behandlingMedUtbetalingskontroll,
            ).left()
        }

        val attestering = Attestering(
            status = Attesteringsstatus.GODKJENT,
            begrunnelse = null,
            beslutter = beslutter.navIdent,
            tidspunkt = nå(clock),
        )
        // Denne validerer saksbehandler
        val (iverksattRammebehandling, klagestatistikk) = behandlingMedUtbetalingskontroll.iverksett(
            utøvendeBeslutter = beslutter,
            attestering = attestering,
            correlationId = correlationId,
            clock = clock,
        )
        val (oppdatertSak, vedtak, rammevedtakstatistikk) = sak.opprettRammevedtak(iverksattRammebehandling, clock)
            .getOrElse {
                logger.error { "Kunne ikke opprette rammevedtak for behandling $rammebehandlingId: $it" }
                return KanIkkeIverksetteBehandling.OpprettVedtakFeil(it).left()
            }

        val doubleOppdatertSak = when (iverksattRammebehandling) {
            is Revurdering -> oppdatertSak.iverksettRammebehandling(
                rammevedtak = vedtak,
                statistikkhendelser = (klagestatistikk + rammevedtakstatistikk),
            )

            is Søknadsbehandling -> oppdatertSak.iverksettSøknadsbehandling(
                rammevedtak = vedtak,
                statistikkhendelser = klagestatistikk + rammevedtakstatistikk,
            )
        }

        return (doubleOppdatertSak to iverksattRammebehandling).right()
    }

    private suspend fun Sak.iverksettSøknadsbehandling(
        rammevedtak: Rammevedtak,
        statistikkhendelser: Statistikkhendelser,
    ): Sak {
        return when (rammevedtak.rammebehandlingsresultat) {
            is Søknadsbehandlingsresultat.Innvilgelse -> this.iverksettRammebehandling(
                rammevedtak,
                statistikkhendelser,
            )

            is Søknadsbehandlingsresultat.Avslag -> {
                val statistikkDTO = statistikkService.generer(statistikkhendelser)
                // journalføring og dokumentdistribusjon skjer i egen jobb
                sessionFactory.withTransactionContext { tx ->
                    // Obs: Dersom du endrer eller legger til noe her som angår klage, merk at du må gjøre tilsvarende i [no.nav.tiltakspenger.saksbehandling.klage.service.IverksettAvvistKlagebehandlingService]
                    rammebehandlingRepo.lagre(rammevedtak.rammebehandling, tx)

                    sakService.markerSkalSendesTilMeldekortApi(
                        sakId = this.id,
                        sessionContext = tx,
                    )
                    rammevedtakRepo.lagre(rammevedtak, tx)
                    statistikkService.lagre(statistikkDTO, tx)
                    runBlocking {
                        tx.onSuccess { MetricRegister.IVERKSATT_BEHANDLING.inc() }
                    }
                }
                this
            }

            is Revurderingsresultat -> throw IllegalArgumentException("Kan ikke iverksette revurdering-resultat på en søknadsbehandling")
        }
    }

    private suspend fun Sak.iverksettRammebehandling(
        rammevedtak: Rammevedtak,
        statistikkhendelser: Statistikkhendelser,
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
            clock = clock,
        )
        val sakOppdatertMedMeldekortbehandlinger =
            sakOppdatertMedMeldeperioder.oppdaterMeldekortbehandlinger(oppdaterteMeldekortbehandlinger)

        val tidligereVedtak = sakOppdatertMedMeldekortbehandlinger.rammevedtaksliste
        val statistikkDTO = statistikkService.generer(statistikkhendelser)
        // journalføring og dokumentdistribusjon skjer i egen jobb
        sessionFactory.withTransactionContext { tx ->
            // Obs: Dersom du endrer eller legger til noe her som angår klage, merk at du må gjøre tilsvarende i [no.nav.tiltakspenger.saksbehandling.klage.service.IverksettAvvistKlagebehandlingService]
            rammebehandlingRepo.lagre(rammevedtak.rammebehandling, tx)
            sakService.oppdaterSkalSendeMeldeperioderTilDatadelingOgSkalSendesTilMeldekortApi(
                sakId = sakOppdatertMedMeldeperioder.id,
                skalSendesTilMeldekortApi = true,
                skalSendeMeldeperioderTilDatadeling = true,
                sessionContext = tx,
            )
            rammevedtakRepo.lagre(rammevedtak, tx)
            statistikkService.lagre(statistikkDTO, tx)
            meldeperiodeRepo.lagre(oppdaterteMeldeperioder, tx)
            // Merk at simuleringen vil nulles ut her. Gjelder kun åpne meldekortbehandlinger.
            oppdaterteMeldekort.forEach { meldekortbehandlingRepo.oppdater(it, null, tx) }

            tidligereVedtak.forEach {
                rammevedtakRepo.oppdaterOmgjortAv(
                    it.id,
                    it.omgjortAvRammevedtak,
                    tx,
                )
            }
            runBlocking {
                tx.onSuccess {
                    MetricRegister.IVERKSATT_BEHANDLING.inc()

                    if (rammevedtak.rammebehandling.utbetaling?.harFeilutbetaling() == true) {
                        logger.warn { "Rammebehandling med feilutbetaling har blitt iverksatt - Behandling-id ${rammevedtak.rammebehandling.id} - vedtak-id: ${rammevedtak.id} - sak-id: ${rammevedtak.sakId}" }
                        MetricRegister.IVERKSATT_RAMMEBEHANDLING_MED_FEILUTBETALING.inc()
                    }
                }
            }
        }

        return sakOppdatertMedMeldekortbehandlinger
    }
}
