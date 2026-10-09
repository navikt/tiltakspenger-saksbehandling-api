package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nonEmptySetOf
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.utledAutomatiskStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.loggkontekst
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.oppdaterSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.oppdaterStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startAutomatiskStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.tilBeslutter.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.tilBeslutter.tilBeslutning
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.ForberedtRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.Utbetalingskontroll
import no.nav.tiltakspenger.saksbehandling.beregning.beregnRevurderingStans
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.meldeperioderErGyldigeForHelg
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock
import java.time.LocalDate

/**
 * Oppretter og behandler stanser som systemet starter når en tiltaksdeltakelse er avsluttet.
 *
 * [opprett] bygger stansen under automatisk behandling, tildelt [AUTOMATISK_SAKSBEHANDLER].
 * [forsøkAutomatiskStans] utleder verdiene for stansen, fyller den ut og sender den til beslutning.
 * Stegene gjøres direkte på domeneobjektene, og resultatet lagres i én transaksjon til slutt.
 *
 * Kan stansen ikke behandles automatisk, overlates den til en saksbehandler med grunnen lagret på revurderingen.
 * Uventede feil, f.eks. når simulering eller oppslag mot Nav-kontor feiler, kastes videre uten at noe er lagret.
 * Da forsøkes stansen på nytt neste gang jobben kjører.
 */
class DelautomatiskStansService(
    private val sakService: SakService,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val navkontorService: NavkontorService,
    private val simulerService: SimulerService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    /**
     * Bygger stansen og statistikken uten å lagre noe.
     * Kalleren lagrer med [no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService.lagre], i samme transaksjon som egne endringer.
     */
    suspend fun opprett(
        sak: Sak,
        automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn,
        correlationId: CorrelationId,
    ): ForberedtRevurdering {
        val (oppdatertSak, revurdering) = sak.startAutomatiskStans(
            automatiskOpprettetGrunn = automatiskOpprettetGrunn,
            correlationId = correlationId,
            clock = clock,
            hentSaksopplysninger = { fnr, correlationId, tiltaksdeltakelserDetErSøktTiltakspengerFor, aktuelleTiltaksdeltakelserForBehandlingen, inkluderOverlappendeTiltaksdeltakelserDetErSøktOm, sakId ->
                hentSaksopplysingerService.hentSaksopplysningerFraRegistre(
                    fnr = fnr,
                    correlationId = correlationId,
                    tiltaksdeltakelserDetErSøktTiltakspengerFor = tiltaksdeltakelserDetErSøktTiltakspengerFor,
                    aktuelleTiltaksdeltakelserForBehandlingen = aktuelleTiltaksdeltakelserForBehandlingen,
                    inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = inkluderOverlappendeTiltaksdeltakelserDetErSøktOm,
                    sakId = sakId,
                    saksnummer = sak.saksnummer,
                )
            },
        )

        val statistikk = statistikkService.generer(
            Statistikkhendelser(revurdering.genererSaksstatistikk(StatistikkhendelseType.OPPRETTET_REVURDERING)),
        )

        return ForberedtRevurdering(oppdatertSak, revurdering, statistikk)
    }

    suspend fun forsøkAutomatiskStans(
        sakId: SakId,
        revurderingId: RammebehandlingId,
        correlationId: CorrelationId,
    ) {
        val sak = sakService.hentForSakId(sakId)
        val revurdering = sak.hentRammebehandling(revurderingId)
        if (revurdering !is Revurdering || !revurdering.erUnderAutomatiskBehandling) {
            log.info { "Behandlingen er ikke lenger en revurdering under automatisk behandling, hopper over (sakId $sakId / revurderingId $revurderingId / correlationId $correlationId)" }
            return
        }

        val (oppdatertSak, oppdatertRevurdering) = sak.oppdaterSaksopplysninger(revurdering, correlationId)

        oppdatertSak.fyllUtOgSendTilBeslutning(oppdatertRevurdering, correlationId).fold(
            ifLeft = { grunn ->
                log.info { "Kan ikke stanse automatisk ($grunn), overlates til saksbehandler - ${revurdering.loggkontekst(correlationId)}" }
                lagreTilManuellBehandling(oppdatertRevurdering, grunn)
            },
            ifRight = { (sendtTilBeslutning, simuleringMedMetadata) ->
                lagreSendtTilBeslutning(sendtTilBeslutning, simuleringMedMetadata)
                log.info { "Automatisk stans er fylt ut og sendt til beslutning - ${revurdering.loggkontekst(correlationId)}" }
            },
        )
    }

    private suspend fun Sak.oppdaterSaksopplysninger(
        revurdering: Revurdering,
        correlationId: CorrelationId,
    ): Pair<Sak, Revurdering> {
        val oppdaterteSaksopplysninger = hentSaksopplysingerService.hentSaksopplysningerFraRegistre(
            fnr = fnr,
            correlationId = correlationId,
            tiltaksdeltakelserDetErSøktTiltakspengerFor = tiltaksdeltakelserDetErSøktTiltakspengerFor,
            aktuelleTiltaksdeltakelserForBehandlingen = tiltaksdeltakelserDetErSøktTiltakspengerFor.map { it.søknadstiltak.tiltaksdeltakerId }.distinct(),
            inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = false,
            sakId = id,
            saksnummer = saksnummer,
            behandlingId = revurdering.id,
        )
        val oppdatert = revurdering.oppdaterSaksopplysninger(AUTOMATISK_SAKSBEHANDLER, oppdaterteSaksopplysninger, clock).getOrElse {
            throw IllegalStateException("Kunne ikke oppdatere saksopplysninger på automatisk stans: $it - ${revurdering.loggkontekst(correlationId)}")
        } as Revurdering
        return oppdaterRammebehandling(oppdatert) to oppdatert
    }

    /**
     * Gir revurderingen klar for lagring som sendt til beslutning, eller grunnen til at den må behandles manuelt.
     */
    private suspend fun Sak.fyllUtOgSendTilBeslutning(
        revurdering: Revurdering,
        correlationId: CorrelationId,
    ): Either<ManueltBehandlesGrunn, Pair<Revurdering, SimuleringMedMetadata?>> {
        if (rammebehandlinger.åpneBehandlinger.any { it.id != revurdering.id }) {
            return ManueltBehandlesGrunn.ANNET_APEN_BEHANDLING.left()
        }

        val tiltaksdeltakerId = revurdering.automatiskOpprettetGrunn?.tiltaksdeltakerId
            ?: return ManueltBehandlesGrunn.STANS_FANT_IKKE_TILTAKSDELTAKELSE.left()

        val automatiskStans = utledAutomatiskStans(
            tiltaksdeltakerId = tiltaksdeltakerId,
            nåtilstand = revurdering.getTiltaksdeltakelse(tiltaksdeltakerId),
            iDag = LocalDate.now(clock),
        ).getOrElse { return it.left() }

        val kommando = OppdaterRevurderingKommando.Stans(
            sakId = id,
            behandlingId = revurdering.id,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
            correlationId = correlationId,
            begrunnelseVilkårsvurdering = automatiskStans.begrunnelse,
            fritekstTilVedtaksbrev = null,
            valgteHjemler = nonEmptySetOf(automatiskStans.hjemmel),
            stansFraOgMed = automatiskStans.stansFraOgMed,
            skalSendeVedtaksbrev = true,
            skalJournalføreNotat = false,
        )
        // Saken har innvilgede perioder, ellers kunne ikke stansen utledes.
        val førsteDagSomGirRett = førsteDagSomGirRett!!
        val sisteDagSomGirRett = sisteDagSomGirRett!!
        val stansperiode = kommando.utledStansperiode(førsteDagSomGirRett, sisteDagSomGirRett)

        val simulertUtbetaling = beregnOgSimuler(revurdering, stansperiode)

        val utfylt = revurdering.oppdaterStans(
            kommando = kommando,
            førsteDagSomGirRett = førsteDagSomGirRett,
            sisteDagSomGirRett = sisteDagSomGirRett,
            utbetaling = simulertUtbetaling?.utbetaling,
            omgjørRammevedtak = vedtaksliste.finnRammevedtakSomOmgjøres(stansperiode),
            clock = clock,
        ).getOrElse {
            log.warn { "Kunne ikke fylle ut automatisk stans: $it - ${revurdering.loggkontekst(correlationId)}" }
            return ManueltBehandlesGrunn.STANS_KAN_IKKE_SENDES_TIL_BESLUTNING.left()
        }.oppdaterUtbetalingskontroll(
            oppdatertKontroll = simulertUtbetaling?.utbetalingskontroll,
            clock = clock,
        )

        utfylt.validerKanIverksetteUtbetaling().onLeft {
            log.info { "Utbetalingen på automatisk stans kan ikke iverksettes: ${it::class.simpleName} - ${revurdering.loggkontekst(correlationId)}" }
            return ManueltBehandlesGrunn.STANS_UTBETALING_KAN_IKKE_IVERKSETTES.left()
        }

        if (!oppdaterRammebehandling(utfylt).meldeperioderErGyldigeForHelg(utfylt.id, clock)) {
            log.warn { "Automatisk stans gir ugyldige meldeperioder for helg - ${revurdering.loggkontekst(correlationId)}" }
            return ManueltBehandlesGrunn.STANS_KAN_IKKE_SENDES_TIL_BESLUTNING.left()
        }

        val sendtTilBeslutning = utfylt.tilBeslutning(
            kommando = SendBehandlingTilBeslutningKommando(
                sakId = id,
                behandlingId = revurdering.id,
                saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                correlationId = correlationId,
            ),
            clock = clock,
        ).getOrElse {
            log.warn { "Kunne ikke sende automatisk stans til beslutning: $it - ${revurdering.loggkontekst(correlationId)}" }
            return ManueltBehandlesGrunn.STANS_KAN_IKKE_SENDES_TIL_BESLUTNING.left()
        } as Revurdering

        return (sendtTilBeslutning to simulertUtbetaling?.simuleringMedMetadata).right()
    }

    /**
     * Kaster dersom simuleringen feiler, siden det er en teknisk feil som skal forsøkes på nytt.
     */
    private suspend fun Sak.beregnOgSimuler(
        revurdering: Revurdering,
        stansperiode: Periode,
    ): SimulertUtbetaling? {
        val beregning = beregnRevurderingStans(
            behandlingId = revurdering.id,
            stansperiode = stansperiode,
            beregningstidspunkt = nå(clock),
        ) ?: return null

        val navkontor = navkontorService.hentNavkontor(fnr = fnr, loggkontekst = revurdering.loggkontekst())
        val simuleringMedMetadata = simulerService.simulerSøknadsbehandlingEllerRevurdering(
            behandling = revurdering,
            beregning = beregning,
            forrigeUtbetaling = utbetalinger.lastOrNull(),
            meldeperiodeKjeder = meldeperiodeKjeder,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER.navIdent,
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
        ) { navkontor }.getOrElse {
            throw IllegalStateException("Simulering av automatisk stans feilet: ${it::class.simpleName} - ${revurdering.loggkontekst()}")
        }

        return SimulertUtbetaling(
            utbetaling = BehandlingUtbetaling(
                beregning = beregning,
                navkontor = navkontor,
                simulering = simuleringMedMetadata.simulering,
            ),
            utbetalingskontroll = Utbetalingskontroll(beregning = beregning, simulering = simuleringMedMetadata.simulering),
            simuleringMedMetadata = simuleringMedMetadata,
        )
    }

    /**
     * Stansen beregnes og simuleres én gang, og den samme simuleringen brukes både på utbetalingen og som kontroll ved send til beslutning.
     */
    private data class SimulertUtbetaling(
        val utbetaling: BehandlingUtbetaling,
        val utbetalingskontroll: Utbetalingskontroll,
        val simuleringMedMetadata: SimuleringMedMetadata,
    )

    private suspend fun lagreSendtTilBeslutning(
        revurdering: Revurdering,
        simuleringMedMetadata: SimuleringMedMetadata?,
    ) {
        val statistikk = statistikkService.generer(
            Statistikkhendelser(revurdering.genererSaksstatistikk(StatistikkhendelseType.SENDT_TIL_BESLUTTER)),
        )
        sessionFactory.withTransactionContext { tx ->
            rammebehandlingRepo.lagre(revurdering, tx)
            rammebehandlingRepo.oppdaterSimuleringMetadata(revurdering.id, simuleringMedMetadata?.originalResponseBody, tx)
            statistikkService.lagre(statistikk, tx)
        }
    }

    private suspend fun lagreTilManuellBehandling(
        revurdering: Revurdering,
        grunn: ManueltBehandlesGrunn,
    ) {
        val tilManuellBehandling = revurdering.tilManuellBehandling(listOf(grunn), clock)
        val statistikk = statistikkService.generer(
            Statistikkhendelser(tilManuellBehandling.genererSaksstatistikk(StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER)),
        )
        sessionFactory.withTransactionContext { tx ->
            rammebehandlingRepo.lagre(tilManuellBehandling, tx)
            statistikkService.lagre(statistikk, tx)
        }
    }
}
