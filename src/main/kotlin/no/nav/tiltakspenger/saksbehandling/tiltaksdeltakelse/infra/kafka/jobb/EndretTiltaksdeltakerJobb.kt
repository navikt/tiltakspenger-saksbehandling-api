package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository.TiltaksdeltakerKafkaDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository.TiltaksdeltakerKafkaRepository
import java.time.Clock
import java.time.LocalDate

class EndretTiltaksdeltakerJobb(
    private val tiltaksdeltakerKafkaRepository: TiltaksdeltakerKafkaRepository,
    private val sakRepo: SakRepo,
    private val oppgaveKlient: OppgaveKlient,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val clock: Clock,
    private val startRevurderingService: StartRevurderingService,

) {
    private val log = KotlinLogging.logger {}

    suspend fun opprettOppgaveEllerRevurderingForEndredeDeltakere() {
        Either.catch {
            val endredeDeltakere = tiltaksdeltakerKafkaRepository.hentAlleUtenOppgaveEllerBehandling(
                sistOppdatertTidligereEnn = nå(clock).minusMinutes(15),
            )

            endredeDeltakere.forEach { deltaker ->
                val sakId = deltaker.sakId
                val eksternDeltakerId = deltaker.id
                val tiltaksdeltakerId = deltaker.tiltaksdeltakerId

                Either.catch {
                    val sak = sakRepo.hentForSakId(sakId)!!

                    sak.oppdaterAutomatiskeSøknadsbehandlingerPåVent(tiltaksdeltakerId)

                    val endringer = sak.finnEndringer(deltaker)

                    if (endringer == null) {
                        log.info { "Fant ingen endringer for sakId $sakId og deltakerId $tiltaksdeltakerId / $eksternDeltakerId" }
                        tiltaksdeltakerKafkaRepository.slett(eksternDeltakerId)
                        return@forEach
                    }

                    val revurdering = sak.opprettRevurderingHvisRelevant(
                        tiltaksdeltakerId,
                        endringer,
                        eksternDeltakerId,
                    )

                    if (revurdering == null) {
                        log.info { "Tiltaksdeltakelse $eksternDeltakerId er endret uten å opprette revurdering, oppretter oppgave" }

                        val oppgaveId = oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                            sak.fnr,
                            Oppgavebehov.ENDRET_TILTAKDELTAKER,
                            endringer.getOppgaveTilleggstekst(),
                        )

                        tiltaksdeltakerKafkaRepository.lagreOppgaveId(eksternDeltakerId, oppgaveId)

                        log.info { "Lagret oppgaveId $oppgaveId for tiltaksdeltakelse $eksternDeltakerId" }
                    } else {
                        tiltaksdeltakerKafkaRepository.lagreBehandlingId(eksternDeltakerId, revurdering.id)

                        log.info { "Opprettet revurdering med id ${revurdering.id} / type ${revurdering.resultat::class.simpleName} for endret tiltaksdeltakelse $tiltaksdeltakerId / $eksternDeltakerId" }
                    }
                }.onLeft {
                    log.error(it) { "Feil ved opprettelse av oppgave/revurdering for endret tiltaksdeltakelse (deltakelseId $eksternDeltakerId - sakId $sakId)" }
                }
            }
        }.onLeft {
            log.error(it) { "Feil ved opprettelse av oppgaver/revurderinger for endret tiltaksdeltakelse" }
        }
    }

    // Antar at intensjonen her er at behandling skal tas av vent dersom tiltaksdeltakelsen endres
    private fun Sak.oppdaterAutomatiskeSøknadsbehandlingerPåVent(tiltaksdeltakerId: TiltaksdeltakerId) {
        rammebehandlinger.åpneSøknadsbehandlinger
            .filter { it.søknad.tiltak?.tiltaksdeltakerId == tiltaksdeltakerId && it.erUnderAutomatiskBehandling && it.ventestatus.erSattPåVent }
            .forEach {
                it.oppdaterVenterTil(
                    nyVenterTil = nå(clock).plusMinutes(15), // venter i 15 minutter i tilfelle det kommer flere endringer
                    clock = clock,
                ).let { behandling ->
                    rammebehandlingRepo.lagre(behandling)
                }
                log.info { "Har oppdatert venterTil for automatisk behandling med id ${it.id} pga endring på deltaker med intern id $tiltaksdeltakerId" }
            }
    }

    private fun Sak.finnEndringer(deltaker: TiltaksdeltakerKafkaDb): TiltaksdeltakerEndringer? {
        val tiltaksdeltakerId = deltaker.tiltaksdeltakerId

        val vedtatteBehandlingerMedRelevantTiltaksdeltakelse = rammevedtaksliste.innvilgetTidslinje.verdier
            .filter { vedtak ->
                vedtak.valgteTiltaksdeltakelser?.any {
                    it.verdi.internDeltakelseId == tiltaksdeltakerId
                } ?: false
            }
            .map { it.rammebehandling }

        val åpneBehandlingerMedRelevantTiltaksdeltakelse = rammebehandlinger.åpneBehandlinger
            .filter { !it.erUnderAutomatiskBehandling && it.getTiltaksdeltakelse(tiltaksdeltakerId) != null }

        val behandlingerMedRelevantTiltaksdeltakelse = vedtatteBehandlingerMedRelevantTiltaksdeltakelse
            .plus(åpneBehandlingerMedRelevantTiltaksdeltakelse)

        if (behandlingerMedRelevantTiltaksdeltakelse.isEmpty()) {
            return null
        }

        val sisteRelevanteTiltaksdeltakelse = behandlingerMedRelevantTiltaksdeltakelse
            .maxBy { it.sistEndret }
            .getTiltaksdeltakelse(tiltaksdeltakerId)!!

        return deltaker.finnEndringer(sisteRelevanteTiltaksdeltakelse, clock)
    }

    private suspend fun Sak.opprettRevurderingHvisRelevant(
        deltakerId: TiltaksdeltakerId,
        endringer: TiltaksdeltakerEndringer,
        hendelseId: String,
    ): Revurdering? {
        val grunn = AutomatiskOpprettetRevurderingGrunn(endringer = endringer, hendelseId = hendelseId)

        if (!this.harFørstegangsvedtak || this.rammebehandlinger.åpneBehandlinger.isNotEmpty()) {
            log.info {
                "Oppretter ikke revurdering hvis det finnes åpne behandlinger, eller førstegangsvedtak mangler - tiltaksdeltakelse $deltakerId, sakId $id"
            }
            return null
        }

        if (endringer.avbrutt != null) {
            return startRevurderingForAvbruddHvisRelevant(deltakerId, grunn)
        }

        if (endringer.forlengelse != null) {
            return startRevurderingForlengelseHvisRelevant(endringer.forlengelse!!, grunn)
        }

        return when {
            endringer.endretStartdato != null ||
                endringer.endretSluttdato != null ||
                endringer.endretDeltakelsesmengde != null -> startOmgjøringHvisRelevant(deltakerId, grunn)

            else -> null
        }
    }

    private suspend fun Sak.startRevurderingForAvbruddHvisRelevant(
        deltakerId: TiltaksdeltakerId,
        grunn: AutomatiskOpprettetRevurderingGrunn,
    ): Revurdering? {
        val idag = LocalDate.now(clock)

        val harRettFremover = rammevedtaksliste.sisteDagSomGirRett?.let { it >= idag } ?: false

        // Dersom det ikke finnes dager med rett i fremtiden, er sannsynligvis innvilgelsen stanset allerede
        if (!harRettFremover) {
            return null
        }

        val harAndreTiltaksdeltakelserFremover by lazy {
            rammevedtaksliste.valgteTiltaksdeltakelser.filter {
                it.periode.tilOgMed >= idag && it.verdi.internDeltakelseId != deltakerId
            }.verdier.isNotEmpty()
        }

        // Oppretter ikke stans dersom det også er innvilget for andre tiltaksdeltakelser
        if (harAndreTiltaksdeltakelserFremover) {
            return startOmgjøringHvisRelevant(deltakerId, grunn)
        }

        return startRevurdering(StartRevurderingType.STANS, automatiskOpprettetGrunn = grunn)
    }

    private suspend fun Sak.startRevurderingForlengelseHvisRelevant(
        endring: TiltaksdeltakerEndring.Forlengelse,
        grunn: AutomatiskOpprettetRevurderingGrunn,
    ): Revurdering? {
        // Dersom det allerede er rett frem til ny sluttdato, så har forlengelsen sannsynligvis allerede blitt iverksatt
        // TODO: vi kunne kanskje sjekke mot gjeldende vedtak i stedet for siste dag på hele saken, for de tilfellene
        // der det finnes flere vedtak, og et annet vedtak enn det siste forlenges. Dette skjer sannsynligvis veldig sjelden (aldri?)
        if (sisteDagSomGirRett != null && endring.nySluttdato <= sisteDagSomGirRett) {
            return null
        }

        return startRevurdering(StartRevurderingType.INNVILGELSE, automatiskOpprettetGrunn = grunn)
    }

    private suspend fun Sak.startOmgjøringHvisRelevant(
        deltakerId: TiltaksdeltakerId,
        grunn: AutomatiskOpprettetRevurderingGrunn,
    ): Revurdering? {
        val vedtakMedRelevantTiltaksdeltakelse = rammevedtaksliste.innvilgetTidslinje.filter {
            it.verdi.gjeldendeTiltaksdeltakelser.verdier.any { deltakelse -> deltakelse.internDeltakelseId == deltakerId }
        }.verdier

        if (vedtakMedRelevantTiltaksdeltakelse.isEmpty()) {
            log.error { "Forventet minst ett vedtak med deltakerId $deltakerId på sak $id" }
            return null
        }

        // Dersom det er flere vedtak med denne deltakelsen, må saksbehandler selv ta stilling til hvilke som evt skal omgjøres
        // TODO: Når vi støtter å omgjøre flere vedtak med en omgjøring, kan dette også gjøres automatisk her
        if (vedtakMedRelevantTiltaksdeltakelse.size != 1) {
            return null
        }

        return startRevurdering(StartRevurderingType.OMGJØRING, vedtakMedRelevantTiltaksdeltakelse.single().id, grunn)
    }

    private suspend fun Sak.startRevurdering(
        revurderingType: StartRevurderingType,
        vedtakIdSomOmgjøres: VedtakId? = null,
        automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn? = null,
    ): Revurdering {
        val kommando = StartRevurderingKommando(
            sakId = this.id,
            correlationId = CorrelationId.generate(),
            saksbehandler = null,
            revurderingType = revurderingType,
            vedtakIdSomOmgjøres = vedtakIdSomOmgjøres,
            klagebehandlingId = null,
            automatiskOpprettetGrunn = automatiskOpprettetGrunn,
        )

        return startRevurderingService.startRevurdering(kommando, this).second
    }
}
