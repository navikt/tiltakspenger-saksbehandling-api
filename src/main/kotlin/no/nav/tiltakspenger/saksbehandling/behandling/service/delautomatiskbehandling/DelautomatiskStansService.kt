package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SkalJournalføreNotat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.AutomatiskStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.AutomatiskStansEllerOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.MAKS_UTBETALTE_DAGER_FOR_AUTOMATISK_STANS
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.utledAutomatiskStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.vurderOpphørIStedenforStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.loggkontekst
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterOmgjøringKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.fyllUtAutomatiskOpprettetOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.oppdaterStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startAutomatiskOmgjøring
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startAutomatiskStans
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.ForberedtRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.beregnOpphør
import no.nav.tiltakspenger.saksbehandling.beregning.beregnRevurderingStans
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.Clock
import java.time.LocalDate

/**
 * Oppretter og behandler stanser og opphør som systemet starter når en tiltaksdeltakelse er avbrutt.
 *
 * [opprettStansEllerOpphør] bygger stansen under automatisk behandling, tildelt [AUTOMATISK_SAKSBEHANDLER].
 * [forsøkAutomatiskStans] utleder verdiene for stansen, fyller den ut og sender den til beslutning.
 * Stansen simuleres først fra sluttdatoen, og stansdatoen flyttes bare dersom simuleringen viser feilutbetaling, se [utledAutomatiskStans].
 * Stegene som er felles med andre automatiske revurderinger, ligger i [DelautomatiskRevurderingSteg].
 *
 * Har oppdragssystemet allerede utbetalt for mange dager etter at deltakelsen ble avbrutt, opprettes et opphør i stedet for stansen, se [opprettStansEllerOpphør].
 *
 * Kan stansen ikke behandles automatisk, overlates den til en saksbehandler med alle grunnene lagret på revurderingen.
 * Uventede feil kastes videre uten at noe er lagret.
 * Da forsøkes stansen på nytt neste gang jobben kjører.
 */
class DelautomatiskStansService(
    private val sakService: SakService,
    private val steg: DelautomatiskRevurderingSteg,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    /**
     * Bygger stansen, eller opphøret som erstatter den, og statistikken uten å lagre noe.
     * Kalleren lagrer med [no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService.lagre], i samme transaksjon som egne endringer.
     *
     * Stansen simuleres fra dagen etter sluttdatoen før den opprettes.
     * Har oppdragssystemet allerede utbetalt flere enn [MAKS_UTBETALTE_DAGER_FOR_AUTOMATISK_STANS] dager etter at deltakelsen ble avbrutt, opprettes i stedet et prioritert opphør, se [vurderOpphørIStedenforStans].
     * Opphøret fylles ut og overlates til en saksbehandler, og behandles aldri automatisk.
     *
     * @return null dersom opphøret ville omgjort flere vedtak eller ikke kan fylles ut, og må følges opp med en oppgave.
     */
    suspend fun opprettStansEllerOpphør(
        sak: Sak,
        automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn,
        correlationId: CorrelationId,
    ): ForberedtRevurdering? {
        val (sakMedStans, stans) = sak.startAutomatiskStans(
            automatiskOpprettetGrunn = automatiskOpprettetGrunn,
            correlationId = correlationId,
            clock = clock,
            hentSaksopplysninger = steg.hentSaksopplysninger(sak),
        )

        suspend fun forberedtStans() = steg.forbered(sakMedStans, stans)

        val tiltaksdeltakerId = automatiskOpprettetGrunn.tiltaksdeltakerId ?: return forberedtStans()
        // Kan ikke stansen utledes, behandles den uansett manuelt når den forsøkes behandlet.
        val utledetFraSluttdato = sakMedStans.utledAutomatiskStans(
            tiltaksdeltakerId = tiltaksdeltakerId,
            nåtilstand = stans.getTiltaksdeltakelse(tiltaksdeltakerId),
            iDag = LocalDate.now(clock),
        ).getOrElse { return forberedtStans() }
        val (_, stansperiode, simulertUtbetaling) = sakMedStans.simulerStans(stans, utledetFraSluttdato, correlationId)

        return when (
            val vurdering = sak.vurderOpphørIStedenforStans(
                stansperiode = stansperiode,
                sisteDagMedFeilutbetaling = simulertUtbetaling?.utbetaling?.simulering?.sisteDagMedFeilutbetaling,
            )
        ) {
            AutomatiskStansEllerOpphør.Stans -> forberedtStans()

            AutomatiskStansEllerOpphør.OpphørOmgjørFlereVedtak -> {
                log.info { "Utbetalt for mange dager til å stanse, men opphøret ville omgjort flere vedtak - ${stans.loggkontekst(correlationId)}" }
                null
            }

            is AutomatiskStansEllerOpphør.Opphør -> sak.opprettOpphør(
                opphør = vurdering,
                saksopplysninger = stans.saksopplysninger,
                automatiskOpprettetGrunn = automatiskOpprettetGrunn,
                correlationId = correlationId,
            )
        }
    }

    /**
     * Oppretter og fyller ut opphøret, og overlater det til en saksbehandler.
     */
    private suspend fun Sak.opprettOpphør(
        opphør: AutomatiskStansEllerOpphør.Opphør,
        saksopplysninger: Saksopplysninger,
        automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn,
        correlationId: CorrelationId,
    ): ForberedtRevurdering? {
        val (sakMedOmgjøring, omgjøring) = startAutomatiskOmgjøring(
            omgjørRammevedtak = opphør.omgjørRammevedtak,
            saksopplysninger = saksopplysninger,
            automatiskOpprettetGrunn = automatiskOpprettetGrunn,
            clock = clock,
        )
        val simulertUtbetaling = sakMedOmgjøring.beregnOpphør(
            behandlingId = omgjøring.id,
            opphørsperiode = opphør.opphørsperiode,
            beregningstidspunkt = nå(clock),
        )?.let { steg.simuler(sakMedOmgjøring, omgjøring, it) }

        val utfylt = omgjøring.fyllUtAutomatiskOpprettetOpphør(
            kommando = OppdaterOmgjøringKommando.OmgjøringOpphør(
                sakId = id,
                behandlingId = omgjøring.id,
                saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                correlationId = correlationId,
                begrunnelseVilkårsvurdering = opphør.begrunnelse,
                fritekstTilVedtaksbrev = null,
                vedtaksperiode = opphør.opphørsperiode,
                valgteHjemler = nonEmptySetOf(opphør.hjemmel),
                skalSendeVedtaksbrev = true,
                prioritering = opphør.prioritering,
                skalJournalføreNotat = SkalJournalføreNotat.INGEN,
            ),
            utbetaling = simulertUtbetaling?.utbetaling,
            finnRammevedtakSomOmgjøres = { vedtaksliste.finnRammevedtakSomOmgjøres(it) },
            rammevedtaksliste = rammevedtaksliste,
            clock = clock,
        ).getOrElse {
            log.warn { "Kunne ikke fylle ut automatisk opphør: $it - ${omgjøring.loggkontekst(correlationId)}" }
            return null
        }

        log.info { "Utbetalt for mange dager til å stanse, oppretter opphør fra og med ${opphør.opphørsperiode.fraOgMed} - ${omgjøring.loggkontekst(correlationId)}" }
        return steg.forbered(sakMedOmgjøring.oppdaterRammebehandling(utfylt), utfylt, simulertUtbetaling?.simuleringMedMetadata)
    }

    suspend fun forsøkAutomatiskStans(
        sakId: SakId,
        revurderingId: RammebehandlingId,
        correlationId: CorrelationId,
    ) {
        val sak = sakService.hentForSakId(sakId)
        val revurdering = steg.hentRevurderingUnderAutomatiskBehandling(sak, revurderingId, correlationId) ?: return

        val (oppdatertSak, oppdatertRevurdering) = steg.oppdaterSaksopplysninger(sak, revurdering, correlationId)

        oppdatertSak.fyllUtOgSendTilBeslutning(oppdatertRevurdering, correlationId).fold(
            ifLeft = { grunner ->
                log.info { "Kan ikke stanse automatisk ($grunner), overlates til saksbehandler - ${revurdering.loggkontekst(correlationId)}" }
                steg.lagreTilManuellBehandling(oppdatertRevurdering, grunner)
            },
            ifRight = { (sendtTilBeslutning, simuleringMedMetadata) ->
                steg.lagreSendtTilBeslutning(sendtTilBeslutning, simuleringMedMetadata)
                log.info { "Automatisk stans er fylt ut og sendt til beslutning - ${revurdering.loggkontekst(correlationId)}" }
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
        val tiltaksdeltakerId = revurdering.automatiskOpprettetGrunn?.tiltaksdeltakerId
            ?: return steg.medAnnenÅpenBehandling(
                this,
                revurdering,
                nonEmptyListOf(ManueltBehandlesGrunn.STANS_FANT_IKKE_TILTAKSDELTAKELSE).left(),
            )

        val nåtilstand = revurdering.getTiltaksdeltakelse(tiltaksdeltakerId)
        val iDag = LocalDate.now(clock)
        val utledetFraSluttdato = steg.medAnnenÅpenBehandling(
            this,
            revurdering,
            utledAutomatiskStans(tiltaksdeltakerId = tiltaksdeltakerId, nåtilstand = nåtilstand, iDag = iDag),
        ).getOrElse { return it.left() }
        val simulertFraSluttdato = simulerStans(revurdering, utledetFraSluttdato, correlationId)

        // En utbetaling vi har sendt kan stanses fram til oppdragssystemet har utbetalt den, og det er bare simuleringen som vet om det har skjedd.
        val sisteDagMedFeilutbetaling = simulertFraSluttdato.simulertUtbetaling?.utbetaling?.simulering?.sisteDagMedFeilutbetaling
        val simulertStans = if (sisteDagMedFeilutbetaling == null) {
            simulertFraSluttdato
        } else {
            val flyttetForbiFeilutbetaling = utledAutomatiskStans(
                tiltaksdeltakerId = tiltaksdeltakerId,
                nåtilstand = nåtilstand,
                iDag = iDag,
                sisteDagMedFeilutbetaling = sisteDagMedFeilutbetaling,
            ).getOrElse { return it.left() }
            log.info { "Simuleringen viser feilutbetaling til og med $sisteDagMedFeilutbetaling, stansdatoen flyttes til ${flyttetForbiFeilutbetaling.stansFraOgMed} - ${revurdering.loggkontekst(correlationId)}" }
            simulerStans(revurdering, flyttetForbiFeilutbetaling, correlationId)
        }
        val (kommando, stansperiode, simulertUtbetaling) = simulertStans

        val utfylt = revurdering.oppdaterStans(
            kommando = kommando,
            // Saken har innvilgede perioder, ellers kunne ikke stansen utledes.
            førsteDagSomGirRett = førsteDagSomGirRett!!,
            sisteDagSomGirRett = sisteDagSomGirRett!!,
            utbetaling = simulertUtbetaling?.utbetaling,
            omgjørRammevedtak = vedtaksliste.finnRammevedtakSomOmgjøres(stansperiode),
            clock = clock,
        ).getOrElse {
            log.warn { "Kunne ikke fylle ut automatisk stans: $it - ${revurdering.loggkontekst(correlationId)}" }
            return nonEmptyListOf(ManueltBehandlesGrunn.STANS_KAN_IKKE_FYLLES_UT).left()
        }.oppdaterUtbetalingskontroll(
            oppdatertKontroll = simulertUtbetaling?.utbetalingskontroll,
            clock = clock,
        )

        val sendtTilBeslutning = steg.sendTilBeslutning(this, utfylt, correlationId, GrunnerVedSendTilBeslutning.stans)
            .getOrElse { return it.left() }

        return (sendtTilBeslutning to simulertUtbetaling?.simuleringMedMetadata).right()
    }

    /**
     * Bygger kommandoen for [automatiskStans] og simulerer utbetalingen for stansperioden, uten å lagre noe.
     * Er det ingenting å beregne i stansperioden, er det heller ingenting å simulere.
     */
    private suspend fun Sak.simulerStans(
        revurdering: Revurdering,
        automatiskStans: AutomatiskStans,
        correlationId: CorrelationId,
    ): SimulertStans {
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
            prioritering = automatiskStans.prioritering ?: revurdering.prioritering,
            skalJournalføreNotat = SkalJournalføreNotat.INGEN,
        )
        val stansperiode = kommando.utledStansperiode(førsteDagSomGirRett!!, sisteDagSomGirRett!!)

        val simulertUtbetaling = beregnRevurderingStans(
            behandlingId = revurdering.id,
            stansperiode = stansperiode,
            beregningstidspunkt = nå(clock),
        )?.let { steg.simuler(this, revurdering, it) }

        return SimulertStans(kommando, stansperiode, simulertUtbetaling)
    }

    private data class SimulertStans(
        val kommando: OppdaterRevurderingKommando.Stans,
        val stansperiode: Periode,
        val simulertUtbetaling: SimulertUtbetaling?,
    )
}
