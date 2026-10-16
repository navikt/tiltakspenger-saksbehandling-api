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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenoppta.GjenopptaRammebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenoppta.gjenoppta
import no.nav.tiltakspenger.saksbehandling.behandling.domene.loggkontekst
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.oppdaterInnvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startAutomatiskForlengelse
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.ForberedtRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.beregnInnvilgelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

/**
 * Oppretter og behandler forlengelser (revurderinger til innvilgelse) som systemet starter når en tiltaksdeltakelse er forlenget.
 *
 * [opprett] bygger forlengelsen under automatisk behandling, tildelt [AUTOMATISK_SAKSBEHANDLER].
 * [forsøkAutomatiskForlengelse] utleder verdiene for forlengelsen, fyller den ut og sender den til beslutning.
 * Starter forlengelsen [DAGER_FØR_STARTDATO] dager eller mer frem i tid, fylles den ut og settes på vent til det er færre dager igjen, og vurderes på nytt da.
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

        val venterTil = revurdering.venterTil
        if (venterTil != null && venterTil >= nå(clock)) {
            log.info { "Forlengelsen venter til $venterTil, hopper over - ${revurdering.loggkontekst(correlationId)}" }
            return
        }

        val (oppdatertSak, oppdatertRevurdering) = steg.oppdaterSaksopplysninger(sak, revurdering, correlationId)

        oppdatertSak.startdatoForLangtFremITid(oppdatertRevurdering)?.let { startdato ->
            oppdatertSak.fyllUtOgSettPåVent(oppdatertRevurdering, startdato, correlationId)
            return
        }

        val (gjenopptattRevurdering, statistikkhendelser) = gjenopptaHvisPåVent(oppdatertRevurdering, correlationId)

        oppdatertSak.oppdaterRammebehandling(gjenopptattRevurdering).fyllUtOgSendTilBeslutning(gjenopptattRevurdering, correlationId).fold(
            ifLeft = { grunner ->
                log.info { "Kan ikke forlenge automatisk ($grunner), overlates til saksbehandler - ${revurdering.loggkontekst(correlationId)}" }
                steg.lagreTilManuellBehandling(gjenopptattRevurdering, grunner, statistikkhendelser)
            },
            ifRight = { (sendtTilBeslutning, simuleringMedMetadata) ->
                steg.lagreSendtTilBeslutning(sendtTilBeslutning, simuleringMedMetadata, statistikkhendelser)
                log.info { "Automatisk forlengelse er fylt ut og sendt til beslutning - ${revurdering.loggkontekst(correlationId)}" }
            },
        )
    }

    /**
     * Gir startdatoen for forlengelsen dersom den er [DAGER_FØR_STARTDATO] dager eller mer frem i tid.
     * Da venter vi med å behandle forlengelsen til det er færre dager igjen, slik at senere endringer i deltakelsen kommer med.
     * Kan ikke forlengelsen utledes, gis null, siden den uansett skal overlates til saksbehandler.
     */
    private fun Sak.startdatoForLangtFremITid(revurdering: Revurdering): LocalDate? {
        val tiltaksdeltakerId = revurdering.automatiskOpprettetGrunn?.tiltaksdeltakerId ?: return null
        val startdato = utledAutomatiskForlengelse(
            tiltaksdeltakerId = tiltaksdeltakerId,
            saksopplysninger = revurdering.saksopplysninger,
        ).getOrNull()?.innvilgelsesperiode?.fraOgMed ?: return null
        return startdato.takeIf { tidligsteBehandlingsdato(it).isAfter(LocalDate.now(clock)) }
    }

    /**
     * Første dag det er færre enn [DAGER_FØR_STARTDATO] dager til [startdato].
     */
    private fun tidligsteBehandlingsdato(startdato: LocalDate): LocalDate = startdato.minusDays(DAGER_FØR_STARTDATO - 1)

    /**
     * Fyller ut forlengelsen og setter den på vent til det er færre enn [DAGER_FØR_STARTDATO] dager til [startdato], eller flytter ventetiden dersom den allerede er på vent.
     * Slik ser saksbehandler hva forlengelsen blir mens den venter.
     * Den fylles ut på nytt med ferske saksopplysninger når den tas av vent.
     * Feiler utfyllingen, settes den på vent uten utfylling, og vente-begrunnelsen sier fra om det.
     */
    private suspend fun Sak.fyllUtOgSettPåVent(
        revurdering: Revurdering,
        startdato: LocalDate,
        correlationId: CorrelationId,
    ) {
        val utfylling = fyllUtFørVent(revurdering, correlationId)
        val (utfylt, simuleringMedMetadata) = utfylling ?: (revurdering to null)
        val begrunnelse = if (utfylling == null) BEGRUNNELSE_UTFYLLING_FEILET else BEGRUNNELSE_STARTER_FREM_I_TID
        val venterTil = tidligsteBehandlingsdato(startdato).atTime(VENTER_TIL_KLOKKESLETT)

        val (påVent, statistikkhendelser) = if (utfylt.ventestatus.erSattPåVent && utfylt.ventestatus.sattPåVentBegrunnelse == begrunnelse) {
            // Siden vi ikke sender ventestatusen til statistikk, trenger vi ikke sende ny statistikk her.
            utfylt.oppdaterVenterTil(nyVenterTil = venterTil, clock = clock) to Statistikkhendelser.empty()
        } else {
            utfylt.settPåVent(
                kommando = SettRammebehandlingPåVentKommando(
                    sakId = revurdering.sakId,
                    rammebehandlingId = revurdering.id,
                    begrunnelse = begrunnelse,
                    oppdaterPåVent = utfylt.ventestatus.erSattPåVent,
                    saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                    venterTil = venterTil,
                    frist = null,
                ),
                clock = clock,
            ).getOrElse {
                throw IllegalStateException("Kunne ikke sette automatisk forlengelse på vent: $it - ${revurdering.loggkontekst(correlationId)}")
            }.let { (påVent, statistikkhendelser) -> (påVent as Revurdering) to statistikkhendelser }
        }

        if (utfylling == null) {
            steg.lagre(påVent, statistikkhendelser)
        } else {
            steg.lagreUtfylt(påVent, simuleringMedMetadata, statistikkhendelser)
        }
        log.info { "Forlengelsen starter $startdato, venter til $venterTil (${if (utfylling == null) "ikke utfylt" else "utfylt"}) - ${revurdering.loggkontekst(correlationId)}" }
    }

    /**
     * Gir den utfylte forlengelsen, eller null dersom den ikke kan fylles ut nå.
     * Feil, også tekniske, svelges her, siden forlengelsen uansett fylles ut på nytt når den tas av vent.
     */
    private suspend fun Sak.fyllUtFørVent(
        revurdering: Revurdering,
        correlationId: CorrelationId,
    ): Pair<Revurdering, SimuleringMedMetadata?>? = Either.catch { fyllUt(revurdering, correlationId) }.fold(
        ifLeft = {
            log.warn(it) { "Utfyllingen av forlengelsen feilet før vent, prøves på nytt når den tas av vent - ${revurdering.loggkontekst(correlationId)}" }
            null
        },
        ifRight = { utfylling ->
            utfylling.onLeft {
                log.info { "Kunne ikke fylle ut forlengelsen før vent ($it), prøves på nytt når den tas av vent - ${revurdering.loggkontekst(correlationId)}" }
            }.getOrNull()
        },
    )

    /**
     * Gjenopptar forlengelsen dersom den er på vent, slik at den kan fylles ut og sendes til beslutning eller overlates til saksbehandler.
     * Statistikken for gjenopptakelsen lagres sammen med resultatet.
     */
    private suspend fun gjenopptaHvisPåVent(
        revurdering: Revurdering,
        correlationId: CorrelationId,
    ): Pair<Revurdering, Statistikkhendelser> {
        if (!revurdering.ventestatus.erSattPåVent) {
            return revurdering to Statistikkhendelser.empty()
        }
        val (gjenopptatt, statistikkhendelser) = revurdering.gjenoppta(
            kommando = GjenopptaRammebehandlingKommando(
                sakId = revurdering.sakId,
                rammebehandlingId = revurdering.id,
                saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                correlationId = correlationId,
            ),
            clock = clock,
            // Saksopplysningene er allerede oppdatert.
            hentSaksopplysninger = null,
        ).getOrElse {
            throw IllegalStateException("Kunne ikke gjenoppta automatisk forlengelse: $it - ${revurdering.loggkontekst(correlationId)}")
        }
        log.info { "Gjenopptar forlengelsen fra vent - ${revurdering.loggkontekst(correlationId)}" }
        return (gjenopptatt as Revurdering) to statistikkhendelser
    }

    /**
     * Gir revurderingen klar for lagring som sendt til beslutning, eller alle grunnene til at den må behandles manuelt.
     */
    private suspend fun Sak.fyllUtOgSendTilBeslutning(
        revurdering: Revurdering,
        correlationId: CorrelationId,
    ): Either<NonEmptyList<ManueltBehandlesGrunn>, Pair<Revurdering, SimuleringMedMetadata?>> {
        val (utfylt, simuleringMedMetadata) = fyllUt(revurdering, correlationId).getOrElse { return it.left() }

        val sendtTilBeslutning = steg.sendTilBeslutning(this, utfylt, correlationId, GrunnerVedSendTilBeslutning.forlengelse)
            .getOrElse { return it.left() }

        return (sendtTilBeslutning to simuleringMedMetadata).right()
    }

    /**
     * Fyller ut forlengelsen med de utledede verdiene og simulerer utbetalingen, uten å lagre noe.
     * Gir alle grunnene til at forlengelsen må behandles manuelt dersom den ikke kan fylles ut.
     */
    private suspend fun Sak.fyllUt(
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
        ) as Revurdering

        return (utfylt to simulertUtbetaling?.simuleringMedMetadata).right()
    }

    companion object {
        private val VENTER_TIL_KLOKKESLETT: LocalTime = LocalTime.of(6, 0)

        /**
         * Forlengelser som starter færre enn så mange dager frem i tid, behandles med en gang.
         * Øvrige settes på vent til det er færre dager igjen til startdatoen.
         */
        private const val DAGER_FØR_STARTDATO: Long = 5

        const val BEGRUNNELSE_STARTER_FREM_I_TID = "Forlengelsen starter frem i tid"
        const val BEGRUNNELSE_UTFYLLING_FEILET =
            "Forlengelsen starter frem i tid. Utfyllingen feilet, og prøves på nytt når forlengelsen tas av vent."
    }
}
