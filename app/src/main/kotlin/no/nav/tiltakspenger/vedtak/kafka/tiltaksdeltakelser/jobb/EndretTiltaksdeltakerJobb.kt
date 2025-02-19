package no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.jobb

import mu.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.ports.SakRepo
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.repository.TiltaksdeltakerKafkaRepository

class EndretTiltaksdeltakerJobb(
    private val tiltaksdeltakerKafkaRepository: TiltaksdeltakerKafkaRepository,
    private val sakRepo: SakRepo,
    private val oppgaveGateway: OppgaveGateway,
) {
    private val log = KotlinLogging.logger {}

    suspend fun opprettOppgaveForEndredeDeltakere() {
        val endredeDeltakere = tiltaksdeltakerKafkaRepository.hentAlleUtenOppgave()

        endredeDeltakere.forEach {
            val sak = sakRepo.hentForSakId(it.sakId)
            if (sak == null) {
                log.error { "Fant ikke sak for sakId ${it.sakId}, skal ikke kunne skje" }
                throw IllegalStateException("Fant ikke sak for sakId ${it.sakId}")
            }
            val nyesteIverksatteBehandling = finnNyesteIverksatteBehandlingForDeltakelse(sak, it.id)
            if (nyesteIverksatteBehandling == null) {
                log.info { "Fant ingen iverksatt behandling for sakId ${it.sakId} og ekstern deltakerId ${it.id}, sletter deltakerinnslag" }
                tiltaksdeltakerKafkaRepository.slett(it.id)
                return@forEach
            }
            log.info { "Fant behandling ${nyesteIverksatteBehandling.id} for sakId ${it.sakId} og deltakerId ${it.id}" }

            val tiltaksdeltakelseFraBehandling = nyesteIverksatteBehandling.tiltaksdeltakelse
            if (it.tiltaksdeltakelseErEndret(tiltaksdeltakelseFraBehandling, nyFlyt = nyesteIverksatteBehandling.erNyFlyt)) {
                log.info { "Tiltaksdeltakelse ${it.id} er endret, oppretter oppgave" }
                val oppgaveId = oppgaveGateway.opprettOppgaveUtenDuplikatkontroll(sak.fnr, Oppgavebehov.ENDRET_TILTAKDELTAKER)
                tiltaksdeltakerKafkaRepository.lagreOppgaveId(it.id, oppgaveId)
                log.info { "Lagret oppgaveId $oppgaveId for tiltaksdeltakelse ${it.id}" }
            } else {
                log.info { "Tiltaksdeltakelse ${it.id} er ikke endret, sletter innslag" }
                tiltaksdeltakerKafkaRepository.slett(it.id)
            }
        }
    }

    suspend fun opprydning() {
        val deltakereMedOppgave = tiltaksdeltakerKafkaRepository.hentAlleMedOppgave()

        deltakereMedOppgave.forEach {
            if (it.oppgaveId != null) {
                val ferdigstilt = oppgaveGateway.erFerdigstilt(it.oppgaveId)
                if (ferdigstilt) {
                    log.info { "Oppgave med id ${it.oppgaveId} er ferdigstilt, sletter innslag for tiltaksdeltakelse ${it.id}" }
                    tiltaksdeltakerKafkaRepository.slett(it.id)
                } else {
                    log.info { "Oppgave med id ${it.oppgaveId} er ikke ferdigstilt, oppdaterer sist sjekket for tiltaksdeltakelse ${it.id}" }
                    tiltaksdeltakerKafkaRepository.oppdaterOppgaveSistSjekket(it.id)
                }
            }
        }
    }

    private fun finnNyesteIverksatteBehandlingForDeltakelse(sak: Sak, tiltaksdeltakerId: String): Behandling? {
        val iverksatteBehandlingerForDeltakelse =
            sak.behandlinger.behandlinger.filter { it.tiltaksid == tiltaksdeltakerId && it.iverksattTidspunkt != null }
        return iverksatteBehandlingerForDeltakelse.maxByOrNull { it.iverksattTidspunkt!! }
    }
}
