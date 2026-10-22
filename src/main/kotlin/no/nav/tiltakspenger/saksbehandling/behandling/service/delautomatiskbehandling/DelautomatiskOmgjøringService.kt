package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SkalJournalføreNotat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskOmgjøring.startAutomatiskOmgjøringAvSluttdato
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskOmgjøring.utledAutomatiskOmgjøringAvSluttdato
import no.nav.tiltakspenger.saksbehandling.behandling.domene.loggkontekst
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterOmgjøringKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.oppdaterOmgjøring
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.ForberedtRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.beregnInnvilgelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.Clock
import java.time.LocalDate

/**
 * Oppretter og behandler omgjøringer som systemet starter når sluttdatoen for en tiltaksdeltakelse er flyttet bakover i tid, mens deltakelsen fortsatt har status deltar.
 * Vedtaket som innvilger deltakelsen, omgjøres slik at deltakelsen bare er innvilget til og med ny sluttdato.
 *
 * [opprett] bygger omgjøringen under automatisk behandling, tildelt [AUTOMATISK_SAKSBEHANDLER].
 * [forsøkAutomatiskOmgjøring] utleder verdiene for omgjøringen, fyller den ut og sender den til beslutning.
 * Stegene som er felles med andre automatiske revurderinger, ligger i [DelautomatiskRevurderingSteg].
 *
 * Kan omgjøringen ikke behandles automatisk, overlates den til en saksbehandler med alle grunnene lagret på revurderingen.
 * Uventede feil kastes videre uten at noe er lagret.
 * Da forsøkes omgjøringen på nytt neste gang jobben kjører.
 */
class DelautomatiskOmgjøringService(
    private val sakService: SakService,
    private val steg: DelautomatiskRevurderingSteg,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    /**
     * Bygger omgjøringen og statistikken uten å lagre noe.
     * Kalleren lagrer med [no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService.lagre], i samme transaksjon som egne endringer.
     */
    suspend fun opprett(
        sak: Sak,
        automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn,
        vedtakIdSomOmgjøres: VedtakId,
        correlationId: CorrelationId,
    ): ForberedtRevurdering {
        val (oppdatertSak, revurdering) = sak.startAutomatiskOmgjøringAvSluttdato(
            omgjørVedtakId = vedtakIdSomOmgjøres,
            automatiskOpprettetGrunn = automatiskOpprettetGrunn,
            correlationId = correlationId,
            clock = clock,
            hentSaksopplysninger = steg.hentSaksopplysninger(sak),
        )
        return steg.forbered(oppdatertSak, revurdering)
    }

    suspend fun forsøkAutomatiskOmgjøring(
        sakId: SakId,
        revurderingId: RammebehandlingId,
        correlationId: CorrelationId,
    ) {
        val sak = sakService.hentForSakId(sakId)
        val revurdering = steg.hentRevurderingUnderAutomatiskBehandling(sak, revurderingId, correlationId) ?: return

        val (oppdatertSak, oppdatertRevurdering) = steg.oppdaterSaksopplysninger(sak, revurdering, correlationId)

        oppdatertSak.fyllUtOgSendTilBeslutning(oppdatertRevurdering, correlationId).fold(
            ifLeft = { grunner ->
                log.info { "Kan ikke omgjøre automatisk ($grunner), overlates til saksbehandler - ${revurdering.loggkontekst(correlationId)}" }
                steg.lagreTilManuellBehandling(oppdatertRevurdering, grunner)
            },
            ifRight = { (sendtTilBeslutning, simuleringMedMetadata) ->
                steg.lagreSendtTilBeslutning(sendtTilBeslutning, simuleringMedMetadata)
                log.info { "Automatisk omgjøring er fylt ut og sendt til beslutning - ${revurdering.loggkontekst(correlationId)}" }
            },
        )
    }

    /**
     * Gir revurderingen klar for lagring som sendt til beslutning, eller alle grunnene til at den må behandles manuelt.
     */
    private suspend fun Sak.fyllUtOgSendTilBeslutning(
        revurdering: Revurdering,
        correlationId: CorrelationId,
    ): Either<NonEmptyList<ManueltBehandlesGrunn>, Pair<Revurdering, SimuleringMedMetadata?>> {
        val (utfylt, simuleringMedMetadata) = fyllUt(revurdering, correlationId).getOrElse { return it.left() }

        val sendtTilBeslutning = steg.sendTilBeslutning(this, utfylt, correlationId, GrunnerVedSendTilBeslutning.omgjøring)
            .getOrElse { return it.left() }

        return (sendtTilBeslutning to simuleringMedMetadata).right()
    }

    /**
     * Fyller ut omgjøringen med de utledede verdiene og simulerer utbetalingen, uten å lagre noe.
     * Gir alle grunnene til at omgjøringen må behandles manuelt dersom den ikke kan fylles ut.
     */
    private suspend fun Sak.fyllUt(
        revurdering: Revurdering,
        correlationId: CorrelationId,
    ): Either<NonEmptyList<ManueltBehandlesGrunn>, Pair<Revurdering, SimuleringMedMetadata?>> {
        val tiltaksdeltakerId = revurdering.automatiskOpprettetGrunn?.tiltaksdeltakerId
            ?: return steg.medAnnenÅpenBehandling(
                this,
                revurdering,
                nonEmptyListOf(ManueltBehandlesGrunn.OMGJORING_FANT_IKKE_TILTAKSDELTAKELSE).left(),
            )

        // Omgjøringen opprettes for nøyaktig ett vedtak, se [startAutomatiskOmgjøringAvSluttdato].
        val omgjørVedtakId = (revurdering.resultat as Omgjøringsresultat).omgjørRammevedtak.rammevedtakIDer.single()

        val automatiskOmgjøring = steg.medAnnenÅpenBehandling(
            this,
            revurdering,
            utledAutomatiskOmgjøringAvSluttdato(
                tiltaksdeltakerId = tiltaksdeltakerId,
                omgjørVedtakId = omgjørVedtakId,
                saksopplysninger = revurdering.saksopplysninger,
                iDag = LocalDate.now(clock),
            ),
        ).getOrElse { return it.left() }

        val kommando = OppdaterOmgjøringKommando.OmgjøringInnvilgelse(
            sakId = id,
            behandlingId = revurdering.id,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
            correlationId = correlationId,
            begrunnelseVilkårsvurdering = automatiskOmgjøring.begrunnelse,
            fritekstTilVedtaksbrev = automatiskOmgjøring.fritekstTilVedtaksbrev,
            innvilgelsesperioder = automatiskOmgjøring.innvilgelsesperioder,
            barnetillegg = automatiskOmgjøring.barnetillegg,
            vedtaksperiode = automatiskOmgjøring.vedtaksperiode,
            skalSendeVedtaksbrev = true,
            prioritering = revurdering.prioritering,
            skalJournalføreNotat = SkalJournalføreNotat.INGEN,
        )

        val simulertUtbetaling = beregnInnvilgelse(
            behandlingId = revurdering.id,
            vedtaksperiode = kommando.vedtaksperiode,
            innvilgelsesperioder = kommando.tilInnvilgelseperioder(revurdering),
            barnetilleggsperioder = kommando.barnetillegg.periodisering,
            beregningstidspunkt = nå(clock),
        )?.let { steg.simuler(this, revurdering, it) }

        val utfylt = revurdering.oppdaterOmgjøring(
            kommando = kommando,
            utbetaling = simulertUtbetaling?.utbetaling,
            finnRammevedtakSomOmgjøres = { vedtaksliste.finnRammevedtakSomOmgjøres(it) },
            rammevedtaksliste = rammevedtaksliste,
            clock = clock,
        ).getOrElse {
            log.warn { "Kunne ikke fylle ut automatisk omgjøring: $it - ${revurdering.loggkontekst(correlationId)}" }
            return nonEmptyListOf(ManueltBehandlesGrunn.OMGJORING_KAN_IKKE_FYLLES_UT).left()
        }.oppdaterUtbetalingskontroll(
            oppdatertKontroll = simulertUtbetaling?.utbetalingskontroll,
            clock = clock,
        ) as Revurdering

        // Valideringen ved send til beslutning tillater feilutbetaling og justering på omgjøringer, siden saksbehandler da har vurdert dem.
        // En automatisk omgjøring har ingen som har vurdert dem, så de overlates til saksbehandler.
        val simulering = simulertUtbetaling?.utbetaling?.simulering
        listOfNotNull(
            ManueltBehandlesGrunn.OMGJORING_UTBETALING_STOTTER_IKKE_FEILUTBETALING.takeIf { simulering?.harFeilutbetaling == true },
            ManueltBehandlesGrunn.OMGJORING_UTBETALING_STOTTER_IKKE_JUSTERING.takeIf { simulering?.harJustering == true },
        ).toNonEmptyListOrNull()?.let {
            log.info { "Simuleringen av automatisk omgjøring har feilutbetaling eller justering - ${revurdering.loggkontekst(correlationId)}" }
            return it.left()
        }

        return (utfylt to simulertUtbetaling?.simuleringMedMetadata).right()
    }
}
