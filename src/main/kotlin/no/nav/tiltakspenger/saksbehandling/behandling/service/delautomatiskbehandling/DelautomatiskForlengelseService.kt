package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SkalJournalføreNotat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskForlengelse.utledAutomatiskForlengelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.loggkontekst
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.oppdaterInnvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startAutomatiskForlengelse
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.ForberedtRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.beregnInnvilgelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.Clock

/**
 * Oppretter og behandler forlengelser (revurderinger til innvilgelse) som systemet starter når en tiltaksdeltakelse er forlenget.
 *
 * [opprett] bygger forlengelsen under automatisk behandling, tildelt [AUTOMATISK_SAKSBEHANDLER].
 * [forsøkAutomatiskForlengelse] utleder verdiene for forlengelsen, fyller den ut og sender den til beslutning.
 * Stegene som er felles med andre automatiske revurderinger, ligger i [DelautomatiskRevurderingSteg].
 *
 * Kan forlengelsen ikke behandles automatisk, overlates den til en saksbehandler med alle grunnene lagret på revurderingen.
 * Uventede feil kastes videre uten at noe er lagret.
 * Da forsøkes forlengelsen på nytt neste gang jobben kjører.
 */
class DelautomatiskForlengelseService(
    private val sakService: SakService,
    private val steg: DelautomatiskRevurderingSteg,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    /**
     * Bygger forlengelsen og statistikken uten å lagre noe.
     * Kalleren lagrer med [no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService.lagre], i samme transaksjon som egne endringer.
     */
    suspend fun opprett(
        sak: Sak,
        automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn,
        correlationId: CorrelationId,
    ): ForberedtRevurdering {
        val (oppdatertSak, revurdering) = sak.startAutomatiskForlengelse(
            automatiskOpprettetGrunn = automatiskOpprettetGrunn,
            correlationId = correlationId,
            clock = clock,
            hentSaksopplysninger = steg.hentSaksopplysninger(sak),
        )
        return steg.forbered(oppdatertSak, revurdering)
    }

    suspend fun forsøkAutomatiskForlengelse(
        sakId: SakId,
        revurderingId: RammebehandlingId,
        correlationId: CorrelationId,
    ) {
        val sak = sakService.hentForSakId(sakId)
        val revurdering = steg.hentRevurderingUnderAutomatiskBehandling(sak, revurderingId, correlationId) ?: return

        val (oppdatertSak, oppdatertRevurdering) = steg.oppdaterSaksopplysninger(sak, revurdering, correlationId)

        oppdatertSak.fyllUtOgSendTilBeslutning(oppdatertRevurdering, correlationId).fold(
            ifLeft = { grunner ->
                log.info { "Kan ikke forlenge automatisk ($grunner), overlates til saksbehandler - ${revurdering.loggkontekst(correlationId)}" }
                steg.lagreTilManuellBehandling(oppdatertRevurdering, grunner)
            },
            ifRight = { (sendtTilBeslutning, simuleringMedMetadata) ->
                steg.lagreSendtTilBeslutning(sendtTilBeslutning, simuleringMedMetadata)
                log.info { "Automatisk forlengelse er fylt ut og sendt til beslutning - ${revurdering.loggkontekst(correlationId)}" }
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
                nonEmptyListOf(ManueltBehandlesGrunn.FORLENGELSE_FANT_IKKE_TILTAKSDELTAKELSE).left(),
            )

        val automatiskForlengelse = steg.medAnnenÅpenBehandling(
            this,
            revurdering,
            utledAutomatiskForlengelse(
                tiltaksdeltakerId = tiltaksdeltakerId,
                saksopplysninger = revurdering.saksopplysninger,
            ),
        ).getOrElse { return it.left() }

        val kommando = OppdaterRevurderingKommando.Innvilgelse(
            sakId = id,
            behandlingId = revurdering.id,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
            correlationId = correlationId,
            begrunnelseVilkårsvurdering = automatiskForlengelse.begrunnelse,
            fritekstTilVedtaksbrev = automatiskForlengelse.fritekstTilVedtaksbrev,
            innvilgelsesperioder = listOf(
                PeriodeMedVerdi(
                    verdi = InnvilgelsesperiodeKommando(
                        periode = automatiskForlengelse.innvilgelsesperiode,
                        antallDagerPerMeldeperiode = automatiskForlengelse.antallDagerPerMeldeperiode,
                        internDeltakelseId = automatiskForlengelse.tiltaksdeltakerId,
                    ),
                    periode = automatiskForlengelse.innvilgelsesperiode,
                ),
            ).tilIkkeTomPeriodisering(),
            barnetillegg = automatiskForlengelse.barnetillegg,
            skalSendeVedtaksbrev = true,
            skalJournalføreNotat = SkalJournalføreNotat.INGEN,
            prioritering = revurdering.prioritering,
        )

        val simulertUtbetaling = beregnInnvilgelse(
            behandlingId = revurdering.id,
            vedtaksperiode = kommando.innvilgelsesperioder.totalPeriode,
            innvilgelsesperioder = kommando.tilInnvilgelseperioder(revurdering),
            barnetilleggsperioder = kommando.barnetillegg.periodisering,
            beregningstidspunkt = nå(clock),
        )?.let { steg.simuler(this, revurdering, it) }

        val utfylt = revurdering.oppdaterInnvilgelse(
            kommando = kommando,
            utbetaling = simulertUtbetaling?.utbetaling,
            omgjørRammevedtak = vedtaksliste.finnRammevedtakSomOmgjøres(kommando.innvilgelsesperioder.totalPeriode),
            clock = clock,
        ).getOrElse {
            log.warn { "Kunne ikke fylle ut automatisk forlengelse: $it - ${revurdering.loggkontekst(correlationId)}" }
            return nonEmptyListOf(ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_FYLLES_UT).left()
        }.oppdaterUtbetalingskontroll(
            oppdatertKontroll = simulertUtbetaling?.utbetalingskontroll,
            clock = clock,
        )

        val sendtTilBeslutning = steg.sendTilBeslutning(this, utfylt, correlationId, GrunnerVedSendTilBeslutning.forlengelse)
            .getOrElse { return it.left() }

        return (sendtTilBeslutning to simulertUtbetaling?.simuleringMedMetadata).right()
    }
}
