package no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.jobb

import no.nav.tiltakspenger.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.repository.TiltaksdeltakerKafkaRepository

class EndretTiltaksdeltakerJobb(
    private val tiltaksdeltakerKafkaRepository: TiltaksdeltakerKafkaRepository,
    private val oppgaveGateway: OppgaveGateway,
) {
    suspend fun opprettOppgaveForEndredeDeltakere() {
        // sjekker diff mellom mottatt deltakelse og deltakelse fra siste iverksatte behandling:
        //   -> Hvis diff, opprett oppgave, lagre oppgaveid, hvis ikke diff, slett
        val endredeDeltakere = tiltaksdeltakerKafkaRepository.hentAlleUtenOppgave()

        endredeDeltakere.forEach {
//            oppgaveGateway.opprettOppgaveUtenDuplikatkontroll()
        }
    }
}
