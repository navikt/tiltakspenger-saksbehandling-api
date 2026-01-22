package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository.TiltaksdeltakerKafkaDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository.TiltaksdeltakerKafkaRepository
import java.time.Clock

class EndretTiltaksdeltakerJobb(
    private val tiltaksdeltakerKafkaRepository: TiltaksdeltakerKafkaRepository,
    private val sakRepo: SakRepo,
    private val oppgaveKlient: OppgaveKlient,
    private val behandlingRepo: BehandlingRepo,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    suspend fun opprettOppgaveForEndredeDeltakere() {
        Either.catch {
            val endredeDeltakere = tiltaksdeltakerKafkaRepository.hentAlleUtenOppgave(
                sistOppdatertTidligereEnn = nå(clock).minusMinutes(15),
            )

            endredeDeltakere.forEach { deltaker ->
                val deltakerId = deltaker.id
                val sakId = deltaker.sakId
                val internTiltaksdeltakerId = deltaker.tiltaksdeltakerId

                Either.catch {
                    val sak = sakRepo.hentForSakId(sakId)!!
                    val apneBehandlingerForDeltakelse = finnApneBehandlingerForDeltakelse(sak, internTiltaksdeltakerId)
                    val automatiskeBehandlingerPaVent =
                        apneBehandlingerForDeltakelse.filter { it.erUnderAutomatiskBehandling && it.ventestatus.erSattPåVent }

                    behandleAutomatiskeBehandlingerPaVentPaNytt(automatiskeBehandlingerPaVent, internTiltaksdeltakerId)

                    val apneManuelleBehandlinger =
                        apneBehandlingerForDeltakelse.filter { !it.erUnderAutomatiskBehandling }
                    val nyesteIverksatteBehandling = finnNyesteIverksatteBehandlingForDeltakelse(sak, internTiltaksdeltakerId)

                    if (nyesteIverksatteBehandling == null && apneManuelleBehandlinger.isEmpty()) {
                        log.info { "Fant ingen åpen manuell behandling eller iverksatt behandling for sakId $sakId og ekstern deltakerId $deltakerId, sletter deltakerinnslag" }
                        tiltaksdeltakerKafkaRepository.slett(deltakerId)
                        return@forEach
                    }

                    val endringer = finnEndringer(apneManuelleBehandlinger, nyesteIverksatteBehandling, deltaker)
                    if (endringer.isNotEmpty()) {
                        log.info { "Tiltaksdeltakelse $deltakerId er endret, oppretter oppgave" }
                        val oppgaveId =
                            oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                                sak.fnr,
                                Oppgavebehov.ENDRET_TILTAKDELTAKER,
                                endringer.getOppgaveTilleggstekst(),
                            )
                        tiltaksdeltakerKafkaRepository.lagreOppgaveId(deltakerId, oppgaveId)
                        log.info { "Lagret oppgaveId $oppgaveId for tiltaksdeltakelse $deltakerId" }
                    } else {
                        log.info { "Tiltaksdeltakelse $deltakerId er ikke endret, sletter innslag" }
                        tiltaksdeltakerKafkaRepository.slett(deltakerId)
                    }
                }.onLeft {
                    log.error(it) { "Feil ved opprettelse av oppgave for endret tiltaksdeltakelse (deltakelseId $deltakerId - sakId $sakId" }
                }
            }
        }.onLeft {
            log.error(it) { "Feil ved opprettelse av oppgaver for endret tiltaksdeltakelse" }
        }
    }

    suspend fun opprydning() {
        Either.catch {
            val deltakereMedOppgave = tiltaksdeltakerKafkaRepository.hentAlleMedOppgave()

            deltakereMedOppgave.forEach {
                val deltakerId = it.id
                val oppgaveId = it.oppgaveId

                Either.catch {
                    if (oppgaveId != null) {
                        val ferdigstilt = oppgaveKlient.erFerdigstilt(oppgaveId)
                        if (ferdigstilt) {
                            log.info { "Oppgave med id $oppgaveId er ferdigstilt, sletter innslag for tiltaksdeltakelse $deltakerId" }
                            tiltaksdeltakerKafkaRepository.slett(deltakerId)
                        } else {
                            log.info { "Oppgave med id $oppgaveId er ikke ferdigstilt, oppdaterer sist sjekket for tiltaksdeltakelse $deltakerId" }
                            tiltaksdeltakerKafkaRepository.oppdaterOppgaveSistSjekket(deltakerId)
                        }
                    }
                }.onLeft {
                    log.error(it) { "Feil ved opprydning av oppgave for tiltaksdeltakelse (deltakerId $deltakerId - oppgaveId $oppgaveId)" }
                }
            }
        }.onLeft {
            log.error(it) { "Feil ved opprydning av oppgaver for tiltaksdeltakelse" }
        }
    }

    private fun finnApneBehandlingerForDeltakelse(sak: Sak, tiltaksdeltakerId: TiltaksdeltakerId): List<Søknadsbehandling> {
        return sak.apneSoknadsbehandlinger.filter { it.søknad.tiltak?.tiltaksdeltakerId == tiltaksdeltakerId }
    }

    private fun finnNyesteIverksatteBehandlingForDeltakelse(sak: Sak, tiltaksdeltakerId: TiltaksdeltakerId): Rammebehandling? {
        val iverksatteBehandlingerForDeltakelse: Periodisering<Rammebehandling> =
            sak.rammevedtaksliste.innvilgetTidslinje
                .filter { it.verdi.behandling.inneholderSaksopplysningerInternDeltakelseId(tiltaksdeltakerId) }
                .map { it.verdi.behandling }

        return iverksatteBehandlingerForDeltakelse.verdier.maxByOrNull { it.iverksattTidspunkt!! }
    }

    private fun behandleAutomatiskeBehandlingerPaVentPaNytt(
        automatiskeBehandlingerPaVent: List<Søknadsbehandling>,
        tiltaksdeltakerId: TiltaksdeltakerId,
    ) {
        automatiskeBehandlingerPaVent.forEach {
            it.oppdaterVenterTil(
                nyVenterTil = nå(clock).plusMinutes(15), // venter i 15 minutter i tilfelle det kommer flere endringer
                clock = clock,
            ).let { behandling ->
                behandlingRepo.lagre(behandling)
            }
            log.info { "Har oppdatert venterTil for automatisk behandling med id ${it.id} pga endring på deltaker med intern id $tiltaksdeltakerId" }
        }
    }

    private fun finnEndringer(
        apneManuelleBehandlinger: List<Søknadsbehandling>,
        nyesteIverksatteBehandling: Rammebehandling?,
        deltaker: TiltaksdeltakerKafkaDb,
    ): List<TiltaksdeltakerEndring> {
        val tidligstEndredeApneBehandling = apneManuelleBehandlinger.minByOrNull { it.sistEndret }

        return if (nyesteIverksatteBehandling != null) {
            finnEndringerForBehandling(nyesteIverksatteBehandling, deltaker)
        } else if (tidligstEndredeApneBehandling != null) {
            finnEndringerForBehandling(tidligstEndredeApneBehandling, deltaker)
        } else {
            throw IllegalStateException("Skal ikke komme hit hvis det ikke finnes åpne eller iverksatte behandlinger")
        }
    }

    private fun finnEndringerForBehandling(
        behandling: Rammebehandling,
        deltaker: TiltaksdeltakerKafkaDb,
    ): List<TiltaksdeltakerEndring> {
        log.info { "Fant behandling ${behandling.id} for sakId ${behandling.sakId} og deltakerId ${deltaker.id}" }

        val tiltaksdeltakelseFraBehandling = behandling.getTiltaksdeltakelse(
            internDeltakelseId = deltaker.tiltaksdeltakerId,
        ) ?: throw IllegalStateException("Fant ikke deltaker med id ${deltaker.id} på behandling ${behandling.id}, skal ikke kunne skje")
        return deltaker.tiltaksdeltakelseErEndret(tiltaksdeltakelseFraBehandling, clock = clock)
    }
}
