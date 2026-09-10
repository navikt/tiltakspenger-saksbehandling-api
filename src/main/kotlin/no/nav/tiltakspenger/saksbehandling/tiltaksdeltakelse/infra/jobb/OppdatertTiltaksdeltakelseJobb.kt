package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltaker
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFraRegister
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.loggFeil
import java.time.Clock

/**
 * Erstatter etter hvert [EndretTiltaksdeltakerJobb].
 * Hendelsene tolkes ikke — consumerne setter bare en markør ([Tiltaksdeltaker.sisteUbehandletEndring]) på deltakeren, og jobben henter nå-tilstanden for deltakelsen ferskt fra tiltakshistorikk-tjenesten.
 * Nå-tilstanden skal etter hvert brukes til å opprette en revurdering automatisk dersom det er relevante endringer.
 * Oppgaver til oppgavesystemet/gosys skal ikke lenger sendes — det erstattes av annen funksjonalitet.
 *
 * Foreløpig er jobben ikke skedulert — se Jobber.kt.
 */
class OppdatertTiltaksdeltakelseJobb(
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
    private val sakRepo: SakRepo,
    private val tiltaksdeltakelseKlient: TiltaksdeltakelseKlient,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    suspend fun håndterUbehandledeEndringer() {
        Either.catch {
            val deltakere = tiltaksdeltakerRepo
                .hentMedUbehandledeEndringer(nå(clock).minusMinutes(MINUTTER_FORSINKELSE))

            log.debug { "Fant ${deltakere.size} tiltaksdeltakere med ubehandlede endringer" }

            deltakere.forEach { behandleDeltaker(it) }
        }.onLeft {
            log.error(it) { "Feil ved henting av nå-tilstand fra tiltakshistorikk for endrede tiltaksdeltakelser" }
        }
    }

    suspend fun behandleDeltaker(deltaker: Tiltaksdeltaker) {
        val logIder =
            "sakId ${deltaker.sakId} / intern deltakerId ${deltaker.id} / ekstern deltakerId ${deltaker.eksternId}"

        Either.catch {
            val markør = deltaker.sisteUbehandletEndring
            if (markør == null) {
                log.info { "Tiltaksdeltaker har ingen ubehandlet endring: $logIder" }
                return
            }

            val sak = sakRepo.hentForSakId(deltaker.sakId)!!

            val nåtilstand = tiltaksdeltakelseKlient.hentTiltaksdeltakelse(
                fnr = sak.fnr,
                eksternDeltakerId = deltaker.eksternId,
                correlationId = CorrelationId.generate(),
            ).getOrElse { feil ->
                // Markøren står igjen, slik at endringen prøves på nytt ved neste kjøring.
                feil.loggFeil(log, "henting av nå-tilstand for tiltaksdeltakelse", logIder)
                return
            }

            if (nåtilstand == null) {
                log.info { "Fant ikke deltakelsen i tiltakshistorikken: $logIder" }
            } else {
                vurderRelevanteEndringerOgOpprettRevurdering(nåtilstand, deltaker)
            }

            tiltaksdeltakerRepo.markerEndringSomBehandlet(deltaker.id, markør)
        }.onLeft {
            log.error(it) { "Feil ved henting av nå-tilstand for tiltaksdeltakelse ($logIder)" }
        }
    }

    // TODO: Placeholder — skal sammenligne nå-tilstanden med saken og opprette en revurdering automatisk dersom det er relevante endringer.
    private fun vurderRelevanteEndringerOgOpprettRevurdering(
        nåtilstand: TiltaksdeltakelseFraRegister,
        deltaker: Tiltaksdeltaker,
    ) {
        log.info {
            "Hentet nå-tilstand fra tiltakshistorikk for deltaker ${deltaker.id} " +
                "(status ${nåtilstand.deltakelseStatus}, periode ${nåtilstand.deltakelseFraOgMed}–${nåtilstand.deltakelseTilOgMed}). " +
                "Vurdering av relevante endringer og automatisk revurdering er ikke implementert ennå."
        }
    }

    companion object {
        // Vi legger til en liten forsinkelse for behandling av hendelser i tilfelle det kommer flere hendelser for samme deltakelse i løpet av kort tid
        const val MINUTTER_FORSINKELSE: Long = 15L
    }
}
