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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.utledAutomatiskStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.loggkontekst
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.oppdaterStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startAutomatiskStans
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.ForberedtRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.beregnRevurderingStans
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.Clock
import java.time.LocalDate

/**
 * Oppretter og behandler stanser som systemet starter når en tiltaksdeltakelse er avsluttet.
 *
 * [opprett] bygger stansen under automatisk behandling, tildelt [AUTOMATISK_SAKSBEHANDLER].
 * [forsøkAutomatiskStans] utleder verdiene for stansen, fyller den ut og sender den til beslutning.
 * Stegene som er felles med andre automatiske revurderinger, ligger i [DelautomatiskRevurderingSteg].
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
            hentSaksopplysninger = steg.hentSaksopplysninger(sak),
        )
        return steg.forbered(oppdatertSak, revurdering)
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

        val automatiskStans = steg.medAnnenÅpenBehandling(
            this,
            revurdering,
            utledAutomatiskStans(
                tiltaksdeltakerId = tiltaksdeltakerId,
                nåtilstand = revurdering.getTiltaksdeltakelse(tiltaksdeltakerId),
                iDag = LocalDate.now(clock),
            ),
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
            prioritering = revurdering.prioritering,
        )
        // Saken har innvilgede perioder, ellers kunne ikke stansen utledes.
        val førsteDagSomGirRett = førsteDagSomGirRett!!
        val sisteDagSomGirRett = sisteDagSomGirRett!!
        val stansperiode = kommando.utledStansperiode(førsteDagSomGirRett, sisteDagSomGirRett)

        val simulertUtbetaling = beregnRevurderingStans(
            behandlingId = revurdering.id,
            stansperiode = stansperiode,
            beregningstidspunkt = nå(clock),
        )?.let { steg.simuler(this, revurdering, it) }

        val utfylt = revurdering.oppdaterStans(
            kommando = kommando,
            førsteDagSomGirRett = førsteDagSomGirRett,
            sisteDagSomGirRett = sisteDagSomGirRett,
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
}
