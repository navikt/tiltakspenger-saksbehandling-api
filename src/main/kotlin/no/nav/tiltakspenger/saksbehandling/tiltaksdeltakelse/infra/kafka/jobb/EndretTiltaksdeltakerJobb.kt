package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
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
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository.TiltaksdeltakerHendelsePostgresRepo
import java.time.Clock
import java.time.LocalDate

class EndretTiltaksdeltakerJobb(
    private val tiltaksdeltakerHendelsePostgresRepo: TiltaksdeltakerHendelsePostgresRepo,
    private val sakRepo: SakRepo,
    private val oppgaveKlient: OppgaveKlient,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val clock: Clock,
    private val startRevurderingService: StartRevurderingService,
) {
    private val log = KotlinLogging.logger {}

    suspend fun håndterEndretTiltaksdeltakerHendelser() {
        Either.catch {
            val hendelserPerDeltaker = tiltaksdeltakerHendelsePostgresRepo
                .hentUbehandlede(MINUTTER_FORSINKELSE)
                .groupBy { it.internDeltakerId }

            log.debug { "Fant ${hendelserPerDeltaker.size} hendelser for endret tiltaksdeltakelse som skal behandles" }

            hendelserPerDeltaker.forEach { (_, hendelser) ->
                val nyesteHendelse = hendelser.last()

                // Marker eldre hendelser for samme deltaker som behandlet uten videre håndtering
                hendelser.dropLast(1).forEach { eldreHendelse ->
                    log.info {
                        "Hendelse ${eldreHendelse.id} er erstattet av en nyere hendelse for deltaker ${eldreHendelse.internDeltakerId}"
                    }
                    tiltaksdeltakerHendelsePostgresRepo.markerSomBehandletOgIgnorert(eldreHendelse.id)
                }

                behandleHendelse(nyesteHendelse)
            }
        }.onLeft {
            log.error(it) { "Feil ved opprettelse av oppgaver/revurderinger for endret tiltaksdeltakelse" }
        }
    }

    private suspend fun behandleHendelse(deltakerHendelse: TiltaksdeltakerHendelse) {
        val sakId = deltakerHendelse.sakId
        val hendelseId = deltakerHendelse.id
        val eksternDeltakerId = deltakerHendelse.eksternDeltakerId
        val internDeltakerId = deltakerHendelse.internDeltakerId

        val logIder =
            "sakId $sakId / intern deltakerId $internDeltakerId / ekstern deltakerId $eksternDeltakerId / hendelseId $hendelseId"

        Either.catch {
            val sak = sakRepo.hentForSakId(sakId)!!

            sak.oppdaterAutomatiskeSøknadsbehandlingerPåVent(internDeltakerId)

            if (!sak.harVedtakEllerÅpenManuellBehandlingForDeltakelse(internDeltakerId)) {
                log.info { "Fant ingen vedtak eller åpne manuelle behandlinger for deltaker: $logIder" }
                tiltaksdeltakerHendelsePostgresRepo.markerSomBehandletOgIgnorert(hendelseId)
                return
            }

            val endringer = sak.finnEndringer(deltakerHendelse)

            if (endringer == null) {
                log.info { "Fant ingen endringer for $logIder" }
                tiltaksdeltakerHendelsePostgresRepo.markerSomBehandletOgIgnorert(hendelseId)
                return
            }

            val revurderingSomSkalOpprettes = sak.vurderRevurdering(internDeltakerId, endringer)

            if (revurderingSomSkalOpprettes != null) {
                val kommando = StartRevurderingKommando(
                    sakId = sak.id,
                    correlationId = CorrelationId.generate(),
                    saksbehandler = null,
                    revurderingType = revurderingSomSkalOpprettes.type,
                    vedtakIdSomOmgjøres = revurderingSomSkalOpprettes.vedtakIdSomOmgjøres,
                    klagebehandlingId = null,
                    automatiskOpprettetGrunn = AutomatiskOpprettetRevurderingGrunn(
                        endringer = endringer,
                        hendelseId = hendelseId.toString(),
                    ),
                )

                val (_, revurdering) = startRevurderingService.startRevurdering(kommando, sak)

                tiltaksdeltakerHendelsePostgresRepo.markerSomBehandletMedRevurdering(hendelseId, revurdering.id)

                log.info { "Opprettet revurdering med id ${revurdering.id} / type ${revurdering.resultat::class.simpleName} for endret tiltaksdeltakelse: $logIder" }
            } else {
                log.info { "Tiltaksdeltakelse er endret uten å opprette revurdering, oppretter oppgave ($logIder)" }

                val oppgaveId = oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                    sak.fnr,
                    Oppgavebehov.ENDRET_TILTAKDELTAKER,
                    endringer.getOppgaveTilleggstekst(),
                )

                tiltaksdeltakerHendelsePostgresRepo.markerSomBehandletMedOppgave(hendelseId, oppgaveId)

                log.info { "Lagret oppgaveId $oppgaveId for tiltaksdeltakelse: $logIder" }
            }
        }.onLeft {
            log.error(it) { "Feil ved opprettelse av oppgave/revurdering for endret tiltaksdeltakelse ($logIder)" }
        }
    }

    // Antar at intensjonen her er at behandling skal tas av vent dersom tiltaksdeltakelsen endres
    private fun Sak.oppdaterAutomatiskeSøknadsbehandlingerPåVent(tiltaksdeltakerId: TiltaksdeltakerId) {
        rammebehandlinger.åpneSøknadsbehandlinger
            .filter { it.søknad.tiltak?.tiltaksdeltakerId == tiltaksdeltakerId && it.erUnderAutomatiskBehandling && it.ventestatus.erSattPåVent }
            .forEach {
                it.oppdaterVenterTil(
                    nyVenterTil = nå(clock).plusMinutes(MINUTTER_FORSINKELSE), // venter i tilfelle det kommer flere endringer
                    clock = clock,
                ).let { behandling ->
                    rammebehandlingRepo.lagre(behandling)
                }
                log.info { "Har oppdatert venterTil for automatisk behandling med id ${it.id} pga endring på deltaker med intern id $tiltaksdeltakerId" }
            }
    }

    private fun Sak.harVedtakEllerÅpenManuellBehandlingForDeltakelse(tiltaksdeltakerId: TiltaksdeltakerId): Boolean {
        val harÅpenManuellBehandling = rammebehandlinger.åpneSøknadsbehandlinger
            .any { it.søknad.tiltak?.tiltaksdeltakerId == tiltaksdeltakerId && !it.erUnderAutomatiskBehandling }

        val harVedtakMedDeltakelse by lazy {
            rammevedtaksliste.valgteTiltaksdeltakelser.any { it.verdi.internDeltakelseId == tiltaksdeltakerId }
        }

        return harÅpenManuellBehandling || harVedtakMedDeltakelse
    }

    private fun Sak.finnEndringer(deltaker: TiltaksdeltakerHendelse): TiltaksdeltakerEndringer? {
        val tiltaksdeltakerId = deltaker.internDeltakerId

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

    private fun Sak.vurderRevurdering(
        deltakerId: TiltaksdeltakerId,
        endringer: TiltaksdeltakerEndringer,
    ): RevurderingSomSkalOpprettes? {
        if (!this.harFørstegangsvedtak || this.rammebehandlinger.åpneBehandlinger.isNotEmpty()) {
            log.info {
                "Oppretter ikke revurdering hvis det finnes åpne behandlinger, eller førstegangsvedtak mangler - tiltaksdeltakelse $deltakerId, sakId $id"
            }
            return null
        }

        if (endringer.avbrutt != null) {
            return vurderRevurderingForAvbrudd(deltakerId)
        }

        if (endringer.forlengelse != null) {
            val forlengelse = vurderRevurderingForForlengelse(endringer.forlengelse!!)
            if (forlengelse != null) return forlengelse
        }

        return when {
            endringer.endretStartdato != null ||
                endringer.endretSluttdato != null ||
                endringer.endretDeltakelsesmengde != null -> vurderOmgjøring(deltakerId)

            else -> null
        }
    }

    private fun Sak.vurderRevurderingForAvbrudd(
        deltakerId: TiltaksdeltakerId,
    ): RevurderingSomSkalOpprettes? {
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
            return vurderOmgjøring(deltakerId)
        }

        return RevurderingSomSkalOpprettes(StartRevurderingType.STANS)
    }

    private fun Sak.vurderRevurderingForForlengelse(
        endring: TiltaksdeltakerEndring.Forlengelse,
    ): RevurderingSomSkalOpprettes? {
        // Dersom det allerede er rett frem til ny sluttdato, så har forlengelsen sannsynligvis allerede blitt iverksatt
        // TODO: vi kunne kanskje sjekke mot gjeldende vedtak i stedet for siste dag på hele saken, for de tilfellene
        // der det finnes flere vedtak, og et annet vedtak enn det siste forlenges. Dette skjer sannsynligvis veldig sjelden (aldri?)
        if (sisteDagSomGirRett != null && endring.nySluttdato <= sisteDagSomGirRett) {
            return null
        }

        return RevurderingSomSkalOpprettes(StartRevurderingType.INNVILGELSE)
    }

    private fun Sak.vurderOmgjøring(
        deltakerId: TiltaksdeltakerId,
    ): RevurderingSomSkalOpprettes? {
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

        return RevurderingSomSkalOpprettes(
            type = StartRevurderingType.OMGJØRING,
            vedtakIdSomOmgjøres = vedtakMedRelevantTiltaksdeltakelse.single().id,
        )
    }

    companion object {
        // Vi legger til en liten forsinkelse for behandling av hendelser
        // i tilfelle det kommer flere hendelser for samme deltakelse i løpet av kort tid
        private const val MINUTTER_FORSINKELSE: Long = 15L
    }
}

private data class RevurderingSomSkalOpprettes(
    val type: StartRevurderingType,
    val vedtakIdSomOmgjøres: VedtakId? = null,
) {

    init {
        require(type != StartRevurderingType.OMGJØRING || vedtakIdSomOmgjøres != null) {
            "Ved omgjøring må vedtakIdSomOmgjøres være satt"
        }
    }
}
