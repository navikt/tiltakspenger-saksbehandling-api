package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.jobb

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.repository.TiltaksdeltakerKafkaRepository
import java.time.LocalDateTime

class EndretTiltaksdeltakerJobb(
    private val tiltaksdeltakerKafkaRepository: TiltaksdeltakerKafkaRepository,
    private val sakRepo: SakRepo,
    private val oppgaveKlient: OppgaveKlient,
) {
    private val log = KotlinLogging.logger {}

    suspend fun opprettOppgaveForEndredeDeltakere() {
        Either.catch {
            val endredeDeltakere = tiltaksdeltakerKafkaRepository.hentAlleUtenOppgave(
                sistOppdatertTidligereEnn = LocalDateTime.now().minusMinutes(15),
            )

            endredeDeltakere.forEach { deltaker ->
                val deltagelseId = deltaker.id
                val sakId = deltaker.sakId

                Either.catch {
                    val sak = sakRepo.hentForSakId(sakId)!!
                    val nyesteIverksatteBehandling = finnNyesteIverksatteBehandlingForDeltakelse(sak, deltagelseId)
                    if (nyesteIverksatteBehandling == null) {
                        log.info { "Fant ingen iverksatt behandling for sakId $sakId og ekstern deltakerId $deltagelseId, sletter deltakerinnslag" }
                        tiltaksdeltakerKafkaRepository.slett(deltagelseId)
                        return@forEach
                    }
                    log.info { "Fant behandling ${nyesteIverksatteBehandling.id} for sakId $sakId og deltakerId $deltagelseId" }

                    val tiltaksdeltakelseFraBehandling = nyesteIverksatteBehandling.getTiltaksdeltagelse(deltagelseId)
                        ?: throw IllegalStateException("Fant ikke deltaker med id $deltagelseId på behandling ${nyesteIverksatteBehandling.id}, skal ikke kunne skje")
                    val endringer = deltaker.tiltaksdeltakelseErEndret(tiltaksdeltakelseFraBehandling)
                    if (endringer.isNotEmpty()) {
                        log.info { "Tiltaksdeltakelse $deltagelseId er endret, oppretter oppgave" }
                        val oppgaveId =
                            oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                                sak.fnr,
                                Oppgavebehov.ENDRET_TILTAKDELTAKER,
                                endringer.getOppgaveTilleggstekst(),
                            )
                        tiltaksdeltakerKafkaRepository.lagreOppgaveId(deltagelseId, oppgaveId)
                        log.info { "Lagret oppgaveId $oppgaveId for tiltaksdeltakelse $deltagelseId" }
                    } else {
                        log.info { "Tiltaksdeltakelse $deltagelseId er ikke endret, sletter innslag" }
                        tiltaksdeltakerKafkaRepository.slett(deltagelseId)
                    }
                }.onLeft {
                    log.error(it) { "Feil ved opprettelse av oppgave for endret tiltaksdeltagelse (deltagelseId $deltagelseId - sakId $sakId" }
                }
            }
        }.onLeft {
            log.error(it) { "Feil ved opprettelse av oppgaver for endret tiltaksdeltagelse" }
        }
    }

    suspend fun opprydning() {
        Either.catch {
            val deltakereMedOppgave = tiltaksdeltakerKafkaRepository.hentAlleMedOppgave()

            deltakereMedOppgave.forEach {
                val deltagelseId = it.id
                val oppgaveId = it.oppgaveId

                Either.catch {
                    if (oppgaveId != null) {
                        val ferdigstilt = oppgaveKlient.erFerdigstilt(oppgaveId)
                        if (ferdigstilt) {
                            log.info { "Oppgave med id $oppgaveId er ferdigstilt, sletter innslag for tiltaksdeltakelse $deltagelseId" }
                            tiltaksdeltakerKafkaRepository.slett(deltagelseId)
                        } else {
                            log.info { "Oppgave med id $oppgaveId er ikke ferdigstilt, oppdaterer sist sjekket for tiltaksdeltakelse $deltagelseId" }
                            tiltaksdeltakerKafkaRepository.oppdaterOppgaveSistSjekket(deltagelseId)
                        }
                    }
                }.onLeft {
                    log.error(it) { "Feil ved opprydning av oppgave for tiltaksdeltagelse (deltagelseId $deltagelseId - oppgaveId $oppgaveId)" }
                }
            }
        }.onLeft {
            log.error(it) { "Feil ved opprydning av oppgaver for tiltaksdeltagelse" }
        }
    }

    private fun finnNyesteIverksatteBehandlingForDeltakelse(sak: Sak, tiltaksdeltakerId: String): Behandling? {
        val iverksatteBehandlingerForDeltakelse: Periodisering<Behandling> = sak.vedtaksliste.innvilgetTidslinje
            .filter { it.verdi.behandling.inneholderEksternDeltagelseId(tiltaksdeltakerId) }
            .map { it.verdi.behandling }

        return iverksatteBehandlingerForDeltakelse.verdier.maxByOrNull { it.iverksattTidspunkt!! }
    }
}
