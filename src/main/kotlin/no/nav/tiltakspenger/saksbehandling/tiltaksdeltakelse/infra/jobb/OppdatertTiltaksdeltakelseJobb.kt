package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.oppgave.EksternOppgave
import no.nav.tiltakspenger.saksbehandling.oppgave.EksternOppgaveRepo
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag.EndretTiltaksdeltakelse.Kilde
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltaker
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFraRegister
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.loggFeil
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.tilTiltaksdeltakelseFraRegister
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Hendelsene tolkes ikke — consumerne setter bare en markør ([Tiltaksdeltaker.sisteUbehandletEndringTidspunkt]) på deltakeren, og jobben henter nå-tilstanden for deltakelsen ferskt fra tiltakshistorikk-tjenesten.
 * Nå-tilstanden sammenlignes med saken, og relevante endringer fører til at en revurdering opprettes automatisk.
 * Automatiske søknadsbehandlinger på vent får fremskyndet ny vurdering når deltakelsen endres.
 * Endringer som ikke kan revurderes automatisk fører til en Gosys-oppgave inntil erstatteren er på plass.
 * Referansen til oppgaven lagres som en [EksternOppgave] i samme transaksjon som markøren nullstilles.
 */
class OppdatertTiltaksdeltakelseJobb(
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
    private val sakRepo: SakRepo,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val tiltaksdeltakelseKlient: TiltaksdeltakelseKlient,
    private val startRevurderingService: StartRevurderingService,
    private val oppgaveKlient: OppgaveKlient,
    private val eksternOppgaveRepo: EksternOppgaveRepo,
    private val sessionFactory: SessionFactory,
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
            log.error(it) { "Feil ved behandling av endrede tiltaksdeltakelser" }
        }
    }

    suspend fun behandleDeltaker(deltaker: Tiltaksdeltaker) {
        val logIder =
            "sakId ${deltaker.sakId} / intern deltakerId ${deltaker.id} / ekstern deltakerId ${deltaker.eksternId}"

        Either.catch {
            val markør = deltaker.sisteUbehandletEndringTidspunkt
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
            }?.tilTiltaksdeltakelseFraRegister(clock)

            sak.oppdaterAutomatiskeSøknadsbehandlingerPåVent(deltaker.id)

            val eksternOppgave = if (nåtilstand == null) {
                // Enten finnes ikke deltakelsen i historikken, eller den har ukjent tiltakstype/kildestatus og kan ikke tolkes.
                log.info { "Fant ingen lesbar nå-tilstand for deltakelsen i tiltakshistorikken: $logIder" }
                null
            } else {
                vurderEndringerOgOpprettRevurderingEllerOppgave(sak, deltaker, nåtilstand, markør, logIder).getOrElse { feil ->
                    // Markøren står igjen, slik at oppgaveopprettelsen prøves på nytt ved neste kjøring.
                    feil.loggFeil(log, "opprettelse av gosysoppgave for endret tiltaksdeltakelse", logIder)
                    return
                }
            }

            sessionFactory.withTransactionContext { tx ->
                eksternOppgave?.let { eksternOppgaveRepo.lagre(it, tx) }
                tiltaksdeltakerRepo.markerEndringSomBehandlet(deltaker.id, markør, tx)
            }
            eksternOppgave?.let { log.info { "Lagret oppgaveId ${it.oppgaveId} for tiltaksdeltakelse: $logIder" } }
        }.onLeft {
            log.error(it) { "Feil ved behandling av endret tiltaksdeltakelse ($logIder)" }
        }
    }

    private fun Sak.oppdaterAutomatiskeSøknadsbehandlingerPåVent(tiltaksdeltakerId: TiltaksdeltakerId) {
        rammebehandlinger.åpneSøknadsbehandlinger
            .filter { it.søknad.tiltak?.tiltaksdeltakerId == tiltaksdeltakerId && it.erUnderAutomatiskBehandling && it.ventestatus.erSattPåVent }
            .forEach {
                it.oppdaterVenterTil(
                    nyVenterTil = nå(clock).plusMinutes(MINUTTER_FORSINKELSE),
                    clock = clock,
                ).let { behandling ->
                    rammebehandlingRepo.lagre(behandling)
                }
                log.info { "Har oppdatert venterTil for automatisk behandling med id ${it.id} pga endring på deltaker med intern id $tiltaksdeltakerId" }
            }
    }

    private suspend fun vurderEndringerOgOpprettRevurderingEllerOppgave(
        sak: Sak,
        deltaker: Tiltaksdeltaker,
        nåtilstand: TiltaksdeltakelseFraRegister,
        markør: LocalDateTime,
        logIder: String,
    ): Either<HttpKlientError, EksternOppgave?> {
        val endringer = sak.finnEndringer(deltaker.id, nåtilstand, clock)
        if (endringer == null) {
            log.info { "Fant ingen relevante endringer for $logIder" }
            return null.right()
        }

        val revurderingSomSkalOpprettes = sak.vurderRevurdering(deltaker.id, endringer)
        if (revurderingSomSkalOpprettes == null) {
            log.info { "Tiltaksdeltakelse er endret uten å opprette revurdering, oppretter oppgave ($logIder)" }
            val tilleggstekst = endringer.getOppgaveTilleggstekst()
            return oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
                fnr = sak.fnr,
                oppgavebehov = Oppgavebehov.ENDRET_TILTAKDELTAKER,
                tilleggstekst = tilleggstekst,
            ).map { oppgaveId ->
                log.info { "Opprettet Gosys-oppgave med id $oppgaveId for endret tiltaksdeltakelse: $logIder" }
                EksternOppgave(
                    oppgaveId = oppgaveId,
                    sakId = sak.id,
                    opprettet = nå(clock),
                    grunnlag = Oppgavegrunnlag.EndretTiltaksdeltakelse(
                        kilde = Kilde.Tiltakshistorikk(sisteUbehandletEndring = markør),
                        tiltaksdeltakerId = deltaker.id,
                        eksternDeltakerId = deltaker.eksternId,
                        deltakelseFraOgMed = nåtilstand.deltakelseFraOgMed,
                        deltakelseTilOgMed = nåtilstand.deltakelseTilOgMed,
                        dagerPerUke = nåtilstand.antallDagerPerUke,
                        deltakelsesprosent = nåtilstand.deltakelseProsent,
                        deltakerstatus = nåtilstand.deltakelseStatus,
                    ),
                    tilleggstekst = tilleggstekst,
                )
            }
        }

        val kommando = StartRevurderingKommando(
            sakId = sak.id,
            correlationId = CorrelationId.generate(),
            saksbehandler = null,
            revurderingType = revurderingSomSkalOpprettes.type,
            vedtakIdSomOmgjøres = revurderingSomSkalOpprettes.vedtakIdSomOmgjøres,
            klagebehandlingId = null,
            automatiskOpprettetGrunn = AutomatiskOpprettetRevurderingGrunn(
                endringer = endringer,
                hendelseId = null,
            ),
        )

        val (_, revurdering) = startRevurderingService.startRevurdering(kommando, sak).getOrElse { feil ->
            throw IllegalStateException(
                "Uventet feil ved automatisk start av revurdering: ${feil.loggkontekst.melding} " +
                    "(saksnummer ${sak.saksnummer} / correlationId ${kommando.correlationId} / $logIder)",
            )
        }

        log.info { "Opprettet revurdering med id ${revurdering.id} / type ${revurdering.resultat::class.simpleName} for endret tiltaksdeltakelse: $logIder" }
        return null.right()
    }

    private fun Sak.vurderRevurdering(
        deltakerId: TiltaksdeltakerId,
        endringer: TiltaksdeltakerEndringer,
    ): AutomatiskRevurdering? {
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
    ): AutomatiskRevurdering? {
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

        return AutomatiskRevurdering(StartRevurderingType.STANS)
    }

    private fun Sak.vurderRevurderingForForlengelse(
        endring: TiltaksdeltakerEndring.Forlengelse,
    ): AutomatiskRevurdering? {
        // Dersom det allerede er rett frem til ny sluttdato, så har forlengelsen sannsynligvis allerede blitt iverksatt
        // TODO: vi kunne kanskje sjekke mot gjeldende vedtak i stedet for siste dag på hele saken, for de tilfellene der det finnes flere vedtak, og et annet vedtak enn det siste forlenges.
        // Dette skjer sannsynligvis veldig sjelden (aldri?)
        if (sisteDagSomGirRett != null && endring.nySluttdato <= sisteDagSomGirRett) {
            return null
        }

        return AutomatiskRevurdering(StartRevurderingType.INNVILGELSE)
    }

    private fun Sak.vurderOmgjøring(
        deltakerId: TiltaksdeltakerId,
    ): AutomatiskRevurdering? {
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

        return AutomatiskRevurdering(
            type = StartRevurderingType.OMGJØRING,
            vedtakIdSomOmgjøres = vedtakMedRelevantTiltaksdeltakelse.single().id,
        )
    }

    companion object {
        // Venter på eventuelle flere hendelser for samme deltakelse før nå-tilstanden hentes.
        const val MINUTTER_FORSINKELSE: Long = 15L
    }
}

private data class AutomatiskRevurdering(
    val type: StartRevurderingType,
    val vedtakIdSomOmgjøres: VedtakId? = null,
) {

    init {
        require(type != StartRevurderingType.OMGJØRING || vedtakIdSomOmgjøres != null) {
            "Ved omgjøring må vedtakIdSomOmgjøres være satt"
        }
    }
}
