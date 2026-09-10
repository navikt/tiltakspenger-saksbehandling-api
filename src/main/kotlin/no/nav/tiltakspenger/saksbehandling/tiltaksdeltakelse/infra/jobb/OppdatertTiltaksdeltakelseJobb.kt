package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFraRegister
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.loggFeil
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerHendelsePostgresRepo

/**
 * Erstatter etter hvert [EndretTiltaksdeltakerJobb].
 * Hendelsene brukes kun som trigger og loggkontekst — i stedet for å tolke endringer fra hendelsene hentes nå-tilstanden for deltakelsen ferskt fra tiltakshistorikk-tjenesten.
 * Nå-tilstanden skal etter hvert brukes til å opprette en revurdering automatisk dersom det er relevante endringer.
 * Oppgaver til oppgavesystemet/gosys skal ikke lenger sendes — det erstattes av annen funksjonalitet.
 *
 * Foreløpig er jobben ikke skedulert, og hendelsene markeres ikke som behandlet — det eies fortsatt av [EndretTiltaksdeltakerJobb] så lenge begge finnes.
 */
class OppdatertTiltaksdeltakelseJobb(
    private val tiltaksdeltakerHendelsePostgresRepo: TiltaksdeltakerHendelsePostgresRepo,
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
    private val sakRepo: SakRepo,
    private val tiltaksdeltakelseKlient: TiltaksdeltakelseKlient,
) {
    private val log = KotlinLogging.logger {}

    suspend fun håndterEndretTiltaksdeltakerHendelser() {
        Either.catch {
            val deltakerIder = tiltaksdeltakerHendelsePostgresRepo
                .hentDeltakereMedUbehandledeHendelser(MINUTTER_FORSINKELSE)

            log.debug { "Fant ${deltakerIder.size} deltakere med hendelser for endret tiltaksdeltakelse som skal behandles" }

            deltakerIder.forEach { behandleHendelserForDeltaker(it) }
        }.onLeft {
            log.error(it) { "Feil ved henting av nå-tilstand fra tiltakshistorikk for endrede tiltaksdeltakelser" }
        }
    }

    suspend fun behandleHendelserForDeltaker(
        internDeltakerId: TiltaksdeltakerId,
        minutterForsinkelse: Long = MINUTTER_FORSINKELSE,
    ) {
        // Kun nyeste hendelse er relevant som trigger — den sier at noe er endret, ikke hva.
        val nyesteHendelse = tiltaksdeltakerHendelsePostgresRepo
            .hentUbehandledeForDeltaker(internDeltakerId, minutterForsinkelse)
            .lastOrNull() ?: return

        behandleHendelse(nyesteHendelse)
    }

    private suspend fun behandleHendelse(hendelse: TiltaksdeltakerHendelse) {
        val internDeltakerId = hendelse.internDeltakerId
        val logIder =
            "sakId ${hendelse.sakId} / intern deltakerId $internDeltakerId / ekstern deltakerId ${hendelse.eksternDeltakerId} / hendelseId ${hendelse.id}"

        Either.catch {
            val sak = sakRepo.hentForSakId(hendelse.sakId)!!

            // Ekstern id kan ha endret seg siden hendelsen — nåværende id hentes fra tiltaksdeltaker-tabellen.
            val eksternDeltakerId = tiltaksdeltakerRepo.hentEksternId(internDeltakerId, null)

            val nåtilstand = tiltaksdeltakelseKlient.hentTiltaksdeltakelse(
                fnr = sak.fnr,
                eksternDeltakerId = eksternDeltakerId,
                correlationId = CorrelationId.generate(),
            ).getOrElse { feil ->
                feil.loggFeil(log, "henting av nå-tilstand for tiltaksdeltakelse", logIder)
                return
            }

            if (nåtilstand == null) {
                log.info { "Fant ikke deltakelsen i tiltakshistorikken: $logIder" }
                return
            }

            vurderRelevanteEndringerOgOpprettRevurdering(nåtilstand, hendelse)
        }.onLeft {
            log.error(it) { "Feil ved henting av nå-tilstand for tiltaksdeltakelse ($logIder)" }
        }
    }

    // TODO: Placeholder — skal sammenligne nå-tilstanden med saken og opprette en revurdering automatisk dersom det er relevante endringer.
    //  Skal også markere hendelsen(e) som behandlet når denne jobben overtar for EndretTiltaksdeltakerJobb.
    private fun vurderRelevanteEndringerOgOpprettRevurdering(
        nåtilstand: TiltaksdeltakelseFraRegister,
        hendelse: TiltaksdeltakerHendelse,
    ) {
        log.info {
            "Hentet nå-tilstand fra tiltakshistorikk for deltaker ${hendelse.internDeltakerId} " +
                "(status ${nåtilstand.deltakelseStatus}, periode ${nåtilstand.deltakelseFraOgMed}–${nåtilstand.deltakelseTilOgMed}). " +
                "Vurdering av relevante endringer og automatisk revurdering er ikke implementert ennå."
        }
    }

    companion object {
        // Vi legger til en liten forsinkelse for behandling av hendelser i tilfelle det kommer flere hendelser for samme deltakelse i løpet av kort tid
        const val MINUTTER_FORSINKELSE: Long = 15L
    }
}
