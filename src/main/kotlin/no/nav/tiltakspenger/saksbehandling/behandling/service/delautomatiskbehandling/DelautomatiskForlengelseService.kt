package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskForlengelse.AutomatiskForlengelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskForlengelse.KanIkkeForlengeAutomatisk
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskForlengelse.KanIkkeForlengeAutomatisk.FeilVedAutomatiskBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskForlengelse.utledAutomatiskForlengelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.tilBeslutter.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendRammebehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/**
 * Forsøker å fylle ut en automatisk opprettet forlengelse (revurdering til innvilgelse) og sende den til beslutning med [AUTOMATISK_SAKSBEHANDLER].
 *
 * Tenkt brukt av [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb] etter at den har opprettet en revurdering til innvilgelse.
 * Den er foreløpig ikke koblet på jobben.
 *
 * Revurderingen blir liggende urørt som [no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BEHANDLING] dersom systemet ikke har nok informasjon (se [utledAutomatiskForlengelse]).
 * Feiler et av stegene etter at [AUTOMATISK_SAKSBEHANDLER] har tatt revurderingen, legges den tilbake slik at en saksbehandler kan ta den.
 *
 * Gjenbruker de samme tjenestene som den manuelle flyten, slik at beregning, simulering og valideringene ved send til beslutning er de samme.
 * Merk at stegene ikke kjøres i én transaksjon.
 */
class DelautomatiskForlengelseService(
    private val sakService: SakService,
    private val taRammebehandlingService: TaRammebehandlingService,
    private val oppdaterRammebehandlingService: OppdaterRammebehandlingService,
    private val sendRammebehandlingTilBeslutningService: SendRammebehandlingTilBeslutningService,
    private val leggTilbakeRammebehandlingService: LeggTilbakeRammebehandlingService,
) {
    private val log = KotlinLogging.logger {}

    suspend fun forsøkAutomatiskForlengelse(
        sakId: SakId,
        revurderingId: RammebehandlingId,
        tiltaksdeltakerId: TiltaksdeltakerId,
        correlationId: CorrelationId,
    ): Either<KanIkkeForlengeAutomatisk, Revurdering> {
        val logIder = "sakId $sakId / revurderingId $revurderingId / intern deltakerId $tiltaksdeltakerId / correlationId $correlationId"

        val sak = sakService.hentForSakId(sakId)
        val revurdering = sak.hentRammebehandling(revurderingId) as? Revurdering
            ?: return KanIkkeForlengeAutomatisk.ErIkkeAutomatiskOpprettetInnvilgelse.left()

        val automatiskForlengelse = sak.utledAutomatiskForlengelse(revurdering, tiltaksdeltakerId).getOrElse {
            log.info { "Kan ikke forlenge automatisk, overlates til saksbehandler: ${it.loggkontekst.melding} ($logIder)" }
            return it.left()
        }

        taRammebehandlingService.taRammebehandling(sakId, revurderingId, AUTOMATISK_SAKSBEHANDLER).getOrElse {
            log.warn { "Kunne ikke tildele revurderingen til ${AUTOMATISK_SAKSBEHANDLER.navIdent}: ${it.loggkontekst.melding} ($logIder)" }
            return FeilVedAutomatiskBehandling("tildeling", it::class.simpleName.orEmpty()).left()
        }

        return Either.catch { fyllUtOgSendTilBeslutning(sakId, revurderingId, automatiskForlengelse, correlationId) }
            .mapLeft {
                log.error(it) { "Uventet feil ved automatisk forlengelse ($logIder)" }
                FeilVedAutomatiskBehandling("automatisk forlengelse", it::class.simpleName.orEmpty())
            }
            .flatMap { it }
            .onLeft {
                log.warn { "Automatisk forlengelse feilet, legger tilbake revurderingen: ${it.loggkontekst.melding} ($logIder)" }
                leggTilbake(sakId, revurderingId, logIder)
            }
            .onRight { log.info { "Automatisk forlengelse er fylt ut og sendt til beslutning ($logIder)" } }
    }

    private suspend fun fyllUtOgSendTilBeslutning(
        sakId: SakId,
        revurderingId: RammebehandlingId,
        automatiskForlengelse: AutomatiskForlengelse,
        correlationId: CorrelationId,
    ): Either<KanIkkeForlengeAutomatisk, Revurdering> {
        oppdaterRammebehandlingService.oppdater(
            OppdaterRevurderingKommando.Innvilgelse(
                sakId = sakId,
                behandlingId = revurderingId,
                saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                correlationId = correlationId,
                begrunnelseVilkårsvurdering = automatiskForlengelse.begrunnelse,
                fritekstTilVedtaksbrev = null,
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
            ),
        ).getOrElse {
            return FeilVedAutomatiskBehandling("utfylling", it::class.simpleName.orEmpty()).left()
        }

        return sendRammebehandlingTilBeslutningService.sendTilBeslutning(
            SendBehandlingTilBeslutningKommando(
                sakId = sakId,
                behandlingId = revurderingId,
                saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                correlationId = correlationId,
            ),
        ).mapLeft {
            FeilVedAutomatiskBehandling("send til beslutning", it::class.simpleName.orEmpty())
        }.map { (_, behandling) -> behandling as Revurdering }
    }

    private suspend fun leggTilbake(
        sakId: SakId,
        revurderingId: RammebehandlingId,
        logIder: String,
    ) {
        leggTilbakeRammebehandlingService.leggTilbakeRammebehandling(sakId, revurderingId, AUTOMATISK_SAKSBEHANDLER).onLeft {
            log.error { "Kunne ikke legge tilbake revurderingen etter feilet automatisk forlengelse: ${it.loggkontekst.melding} ($logIder)" }
        }
    }
}
