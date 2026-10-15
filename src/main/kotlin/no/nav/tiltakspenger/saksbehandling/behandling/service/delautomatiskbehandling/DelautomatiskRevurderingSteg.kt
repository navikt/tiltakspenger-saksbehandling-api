package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.loggkontekst
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.oppdaterSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.HentSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.tilBeslutter.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.tilBeslutter.tilBeslutning
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.ForberedtRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.Utbetalingskontroll
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.meldeperioderErGyldigeForHelg
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.logg
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

/**
 * Stegene som er felles for revurderinger systemet oppretter og behandler automatisk, se [DelautomatiskStansService] og [DelautomatiskForlengelseService].
 * Stegene gjøres direkte på domeneobjektene, og resultatet lagres i én transaksjon til slutt.
 * Uventede feil, f.eks. når simulering eller oppslag mot Nav-kontor feiler, kastes videre uten at noe er lagret.
 */
class DelautomatiskRevurderingSteg(
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val navkontorService: NavkontorService,
    private val simulerService: SimulerService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    fun hentSaksopplysninger(sak: Sak): HentSaksopplysninger =
        { fnr, correlationId, tiltaksdeltakelserDetErSøktTiltakspengerFor, aktuelleTiltaksdeltakelserForBehandlingen, inkluderOverlappendeTiltaksdeltakelserDetErSøktOm, sakId ->
            hentSaksopplysingerService.hentSaksopplysningerFraRegistre(
                fnr = fnr,
                correlationId = correlationId,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = tiltaksdeltakelserDetErSøktTiltakspengerFor,
                aktuelleTiltaksdeltakelserForBehandlingen = aktuelleTiltaksdeltakelserForBehandlingen,
                inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = inkluderOverlappendeTiltaksdeltakelserDetErSøktOm,
                sakId = sakId,
                saksnummer = sak.saksnummer,
            )
        }

    /**
     * Bygger statistikken for den opprettede revurderingen uten å lagre noe.
     * Kalleren lagrer med [no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService.lagre], i samme transaksjon som egne endringer.
     */
    suspend fun forbered(sak: Sak, revurdering: Revurdering): ForberedtRevurdering {
        val statistikk = statistikkService.generer(
            Statistikkhendelser(revurdering.genererSaksstatistikk(StatistikkhendelseType.OPPRETTET_REVURDERING)),
        )
        return ForberedtRevurdering(sak, revurdering, statistikk)
    }

    /**
     * Gir revurderingen dersom den fortsatt er under automatisk behandling.
     * Den kan i mellomtiden f.eks. være overtatt av en saksbehandler.
     */
    fun hentRevurderingUnderAutomatiskBehandling(
        sak: Sak,
        revurderingId: RammebehandlingId,
        correlationId: CorrelationId,
    ): Revurdering? {
        val revurdering = sak.hentRammebehandling(revurderingId)
        if (revurdering !is Revurdering || !revurdering.erUnderAutomatiskBehandling) {
            log.info { "Behandlingen er ikke lenger en revurdering under automatisk behandling, hopper over (sakId ${sak.id} / revurderingId $revurderingId / correlationId $correlationId)" }
            return null
        }
        return revurdering
    }

    /**
     * Henter saksopplysningene på nytt, slik at endringer som har kommet etter at revurderingen ble opprettet, fanges opp.
     */
    suspend fun oppdaterSaksopplysninger(
        sak: Sak,
        revurdering: Revurdering,
        correlationId: CorrelationId,
    ): Pair<Sak, Revurdering> {
        val oppdaterteSaksopplysninger = hentSaksopplysingerService.hentSaksopplysningerFraRegistre(
            fnr = sak.fnr,
            correlationId = correlationId,
            tiltaksdeltakelserDetErSøktTiltakspengerFor = sak.tiltaksdeltakelserDetErSøktTiltakspengerFor,
            aktuelleTiltaksdeltakelserForBehandlingen = sak.tiltaksdeltakelserDetErSøktTiltakspengerFor.map { it.søknadstiltak.tiltaksdeltakerId }.distinct(),
            inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = false,
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            behandlingId = revurdering.id,
        )
        val oppdatert = revurdering.oppdaterSaksopplysninger(AUTOMATISK_SAKSBEHANDLER, oppdaterteSaksopplysninger, clock).getOrElse {
            throw IllegalStateException("Kunne ikke oppdatere saksopplysninger på automatisk revurdering: $it - ${revurdering.loggkontekst(correlationId)}")
        } as Revurdering
        return sak.oppdaterRammebehandling(oppdatert) to oppdatert
    }

    /**
     * Revurderingen simuleres én gang, og den samme simuleringen brukes både på utbetalingen og som kontroll ved send til beslutning.
     * Kaster dersom simuleringen feiler, siden det er en teknisk feil som skal forsøkes på nytt.
     */
    suspend fun simuler(
        sak: Sak,
        revurdering: Revurdering,
        beregning: Beregning,
    ): SimulertUtbetaling {
        val navkontor = navkontorService.hentNavkontor(fnr = sak.fnr, loggkontekst = revurdering.loggkontekst())
        val simuleringMedMetadata = simulerService.simulerSøknadsbehandlingEllerRevurdering(
            behandling = revurdering,
            beregning = beregning,
            forrigeUtbetaling = sak.utbetalinger.lastOrNull(),
            meldeperiodeKjeder = sak.meldeperiodeKjeder,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER.navIdent,
            kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort,
        ) { navkontor }.getOrElse {
            throw IllegalStateException("Simulering av automatisk revurdering feilet: ${it::class.simpleName} - ${revurdering.loggkontekst()}")
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
     * Validerer den utfylte revurderingen og sender den til beslutning, uten å lagre noe.
     * Gir grunnen til at revurderingen må behandles manuelt dersom den ikke kan sendes til beslutning.
     */
    fun sendTilBeslutning(
        sak: Sak,
        utfylt: Rammebehandling,
        correlationId: CorrelationId,
        grunner: GrunnerVedSendTilBeslutning,
    ): Either<ManueltBehandlesGrunn, Revurdering> {
        utfylt.validerKanIverksetteUtbetaling().onLeft {
            it.logg(log) { "Utbetalingen på automatisk revurdering kan ikke iverksettes - ${utfylt.loggkontekst(correlationId)}" }
            return grunner.forUtbetaling(it).left()
        }

        if (!sak.oppdaterRammebehandling(utfylt).meldeperioderErGyldigeForHelg(utfylt.id, clock)) {
            log.warn { "Automatisk revurdering gir ugyldige meldeperioder for helg - ${utfylt.loggkontekst(correlationId)}" }
            return grunner.ugyldigeMeldeperioderForHelg.left()
        }

        return (
            utfylt.tilBeslutning(
                kommando = SendBehandlingTilBeslutningKommando(
                    sakId = sak.id,
                    behandlingId = utfylt.id,
                    saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                    correlationId = correlationId,
                ),
                clock = clock,
            ).getOrElse {
                log.warn { "Kunne ikke sende automatisk revurdering til beslutning: $it - ${utfylt.loggkontekst(correlationId)}" }
                return grunner.kanIkkeSendesTilBeslutning.left()
            } as Revurdering
            ).right()
    }

    suspend fun lagreSendtTilBeslutning(
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

    suspend fun lagreTilManuellBehandling(
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

data class SimulertUtbetaling(
    val utbetaling: BehandlingUtbetaling,
    val utbetalingskontroll: Utbetalingskontroll,
    val simuleringMedMetadata: SimuleringMedMetadata,
)

/**
 * Grunnene som lagres når en automatisk revurdering ikke kan sendes til beslutning, slik at saksbehandler ser hvilken type revurdering det gjaldt.
 */
data class GrunnerVedSendTilBeslutning(
    val simuleringMangler: ManueltBehandlesGrunn,
    val støtterIkkeFeilutbetaling: ManueltBehandlesGrunn,
    val støtterIkkeJustering: ManueltBehandlesGrunn,
    val kontrollsimuleringHarEndringer: ManueltBehandlesGrunn,
    val ugyldigeMeldeperioderForHelg: ManueltBehandlesGrunn,
    val kanIkkeSendesTilBeslutning: ManueltBehandlesGrunn,
) {
    fun forUtbetaling(feil: KanIkkeIverksetteUtbetaling): ManueltBehandlesGrunn = when (feil) {
        KanIkkeIverksetteUtbetaling.SimuleringMangler -> simuleringMangler

        KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeFeilutbetaling -> støtterIkkeFeilutbetaling

        KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeJustering,
        is KanIkkeIverksetteUtbetaling.JusteringStøttesIkke,
        -> støtterIkkeJustering

        is KanIkkeIverksetteUtbetaling.KontrollSimuleringHarEndringer -> kontrollsimuleringHarEndringer
    }

    companion object {
        val stans = GrunnerVedSendTilBeslutning(
            simuleringMangler = ManueltBehandlesGrunn.STANS_UTBETALING_SIMULERING_MANGLER,
            støtterIkkeFeilutbetaling = ManueltBehandlesGrunn.STANS_UTBETALING_STOTTER_IKKE_FEILUTBETALING,
            støtterIkkeJustering = ManueltBehandlesGrunn.STANS_UTBETALING_STOTTER_IKKE_JUSTERING,
            kontrollsimuleringHarEndringer = ManueltBehandlesGrunn.STANS_UTBETALING_KONTROLLSIMULERING_HAR_ENDRINGER,
            ugyldigeMeldeperioderForHelg = ManueltBehandlesGrunn.STANS_UGYLDIGE_MELDEPERIODER_FOR_HELG,
            kanIkkeSendesTilBeslutning = ManueltBehandlesGrunn.STANS_KAN_IKKE_SENDES_TIL_BESLUTNING,
        )

        val forlengelse = GrunnerVedSendTilBeslutning(
            simuleringMangler = ManueltBehandlesGrunn.FORLENGELSE_UTBETALING_SIMULERING_MANGLER,
            støtterIkkeFeilutbetaling = ManueltBehandlesGrunn.FORLENGELSE_UTBETALING_STOTTER_IKKE_FEILUTBETALING,
            støtterIkkeJustering = ManueltBehandlesGrunn.FORLENGELSE_UTBETALING_STOTTER_IKKE_JUSTERING,
            kontrollsimuleringHarEndringer = ManueltBehandlesGrunn.FORLENGELSE_UTBETALING_KONTROLLSIMULERING_HAR_ENDRINGER,
            ugyldigeMeldeperioderForHelg = ManueltBehandlesGrunn.FORLENGELSE_UGYLDIGE_MELDEPERIODER_FOR_HELG,
            kanIkkeSendesTilBeslutning = ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_SENDES_TIL_BESLUTNING,
        )
    }
}
