package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nonEmptySetOf
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.AutomatiskStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.KanIkkeStanseAutomatisk
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.KanIkkeStanseAutomatisk.FeilVedAutomatiskBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.kanStanseAutomatisk
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.tilBeslutter.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendRammebehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.AutomatiskRevurderingAvEndring

/**
 * Forsøker å fylle ut en automatisk opprettet stans-revurdering og sende den til beslutning med [AUTOMATISK_SAKSBEHANDLER].
 *
 * Tenkt brukt av [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb] etter at den har opprettet en stans, men er foreløpig ikke koblet på jobben.
 *
 * Verdiene for utfyllingen tas inn som [AutomatiskRevurderingAvEndring.Stans], utledet da stansen ble opprettet.
 * Revurderingen blir liggende urørt som [no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BEHANDLING] dersom verdiene mangler, eller revurderingen ikke lenger kan behandles automatisk (se [kanStanseAutomatisk]).
 * Feiler et av stegene etter at [AUTOMATISK_SAKSBEHANDLER] har tatt revurderingen, legges den tilbake slik at en saksbehandler kan ta den.
 *
 * Gjenbruker de samme tjenestene som den manuelle flyten, slik at beregning, simulering og valideringene ved send til beslutning er de samme.
 * Merk at stegene ikke kjøres i én transaksjon.
 */
class DelautomatiskStansService(
    private val sakService: SakService,
    private val taRammebehandlingService: TaRammebehandlingService,
    private val oppdaterRammebehandlingService: OppdaterRammebehandlingService,
    private val sendRammebehandlingTilBeslutningService: SendRammebehandlingTilBeslutningService,
    private val leggTilbakeRammebehandlingService: LeggTilbakeRammebehandlingService,
) {
    private val log = KotlinLogging.logger {}

    suspend fun forsøkAutomatiskStans(
        sakId: SakId,
        revurderingId: RammebehandlingId,
        stans: AutomatiskRevurderingAvEndring.Stans,
        correlationId: CorrelationId,
    ): Either<KanIkkeStanseAutomatisk, Revurdering> {
        val logIder = "sakId $sakId / revurderingId $revurderingId / correlationId $correlationId"
        val saksbehandler = AUTOMATISK_SAKSBEHANDLER

        val automatiskStans = stans.utfylling ?: return KanIkkeStanseAutomatisk.ManglerUtfylling.also {
            log.info { "Kan ikke stanse automatisk, overlates til saksbehandler: ${it.loggkontekst.melding} ($logIder)" }
        }.left()

        val sak = sakService.hentForSakId(sakId)
        val revurdering = sak.hentRammebehandling(revurderingId) as? Revurdering
            ?: return KanIkkeStanseAutomatisk.ErIkkeAutomatiskOpprettetStans.left()

        sak.kanStanseAutomatisk(revurdering).onLeft {
            log.info { "Kan ikke stanse automatisk, overlates til saksbehandler: ${it.loggkontekst.melding} ($logIder)" }
            return it.left()
        }

        taRammebehandlingService.taRammebehandling(sakId, revurderingId, saksbehandler).getOrElse {
            log.warn { "Kunne ikke tildele revurderingen til ${saksbehandler.navIdent}: ${it.loggkontekst.melding} ($logIder)" }
            return FeilVedAutomatiskBehandling("tildeling", it::class.simpleName.orEmpty()).left()
        }

        val resultat = try {
            fyllUtOgSendTilBeslutning(sakId, revurderingId, automatiskStans, correlationId)
        } catch (e: Exception) {
            log.error(e) { "Uventet feil ved automatisk stans, legger tilbake revurderingen ($logIder)" }
            leggTilbake(sakId, revurderingId, logIder)
            throw e
        }

        return resultat
            .onLeft {
                log.warn { "Automatisk stans feilet, legger tilbake revurderingen: ${it.loggkontekst.melding} ($logIder)" }
                leggTilbake(sakId, revurderingId, logIder)
            }
            .onRight { log.info { "Automatisk stans er fylt ut og sendt til beslutning ($logIder)" } }
    }

    private suspend fun fyllUtOgSendTilBeslutning(
        sakId: SakId,
        revurderingId: RammebehandlingId,
        automatiskStans: AutomatiskStans,
        correlationId: CorrelationId,
    ): Either<KanIkkeStanseAutomatisk, Revurdering> {
        oppdaterRammebehandlingService.oppdater(
            OppdaterRevurderingKommando.Stans(
                sakId = sakId,
                behandlingId = revurderingId,
                saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                correlationId = correlationId,
                begrunnelseVilkårsvurdering = automatiskStans.begrunnelse,
                fritekstTilVedtaksbrev = null,
                valgteHjemler = nonEmptySetOf(automatiskStans.hjemmel),
                stansFraOgMed = automatiskStans.stansFraOgMed,
                skalSendeVedtaksbrev = true,
                skalJournalføreNotat = false,
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
            log.error { "Kunne ikke legge tilbake revurderingen etter feilet automatisk stans: ${it.loggkontekst.melding} ($logIder)" }
        }
    }
}
