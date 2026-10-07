package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import arrow.core.Either
import arrow.core.flatten
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.ForberedtRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.oppgave.EksternOppgave
import no.nav.tiltakspenger.saksbehandling.oppgave.EksternOppgaveRepo
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag.EndretTiltaksdeltakelse.Kilde
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltaker
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.loggFeil
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.tilLesbarNåtilstand
import java.time.Clock
import java.time.LocalDateTime

/**
 * Hendelsene tolkes ikke — consumerne setter bare en markør ([Tiltaksdeltaker.sisteUbehandletEndringTidspunkt]) på deltakeren, og jobben henter nå-tilstanden for deltakelsen ferskt fra tiltakshistorikk-tjenesten.
 * Nå-tilstanden sammenlignes med saken, og relevante endringer fører til at en revurdering opprettes automatisk.
 * Automatiske søknadsbehandlinger på vent får fremskyndet ny vurdering når deltakelsen endres.
 * Endringer som ikke kan revurderes automatisk fører til en Gosys-oppgave inntil erstatteren er på plass.
 * Referansen til oppgaven lagres som en [EksternOppgave] før markøren nullstilles.
 * Hver ferdig behandlede endring lagres i `tiltaksdeltaker_endring` for sporbarhet, i samme transaksjon som markøren nullstilles.
 * En automatisk revurdering lagres også i den transaksjonen, slik at revurderingen, sporingen og kvitteringen lagres samlet eller ikke i det hele tatt.
 */
class OppdatertTiltaksdeltakelseJobb(
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
    private val sakRepo: SakRepo,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val tiltaksdeltakelseKlient: TiltaksdeltakelseKlient,
    private val startRevurderingService: StartRevurderingService,
    private val oppgaveKlient: OppgaveKlient,
    private val eksternOppgaveRepo: EksternOppgaveRepo,
    private val tiltaksdeltakerHendelseRepo: TiltaksdeltakerHendelseRepo,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    suspend fun håndterUbehandledeEndringer() {
        Either.catch {
            val deltakere = tiltaksdeltakerRepo
                .hentMedUbehandledeEndringer(nå(clock).minusMinutes(MINUTTER_FORSINKELSE))

            log.debug { "Fant ${deltakere.size} tiltaksdeltakere med ubehandlede endringer" }

            deltakere.forEach { deltaker ->
                val deltakerId = deltaker.id

                behandleDeltaker(deltaker)
                    .onRight { resultat ->
                        log.info { "Behandlet endret tiltaksdeltakelse for deltaker $deltakerId: ${resultat::class.simpleName}" }
                    }
                    .onLeft { feil ->
                        // Markøren står igjen, slik at endringen prøves på nytt ved neste kjøring.
                        when (feil) {
                            is TiltaksdeltakelseEndringKunneIkkeBehandles.UventetFeil ->
                                log.error(feil.throwable) { "Feil ved behandling av endret tiltaksdeltakelse for deltaker $deltakerId" }

                            TiltaksdeltakelseEndringKunneIkkeBehandles.KunneIkkeHenteNåtilstand,
                            TiltaksdeltakelseEndringKunneIkkeBehandles.KunneIkkeOppretteOppgave,
                            -> log.warn { "Kunne ikke behandle endret tiltaksdeltakelse for deltaker $deltakerId (${feil::class.simpleName}), prøver igjen ved neste kjøring" }
                        }
                    }
            }
        }.onLeft {
            log.error(it) { "Feil ved behandling av endrede tiltaksdeltakelser" }
        }
    }

    /**
     * Vurderer endringen for deltakeren og lagrer utfallet.
     * Kaster ikke; uventede feil gis som [TiltaksdeltakelseEndringKunneIkkeBehandles.UventetFeil].
     */
    suspend fun behandleDeltaker(deltaker: Tiltaksdeltaker): Either<TiltaksdeltakelseEndringKunneIkkeBehandles, TiltaksdeltakelseEndringBehandlet> =
        Either.catch {
            vurderEndring(deltaker).onRight { resultat ->
                lagreSomBehandlet(deltaker, resultat)
            }
        }.mapLeft { TiltaksdeltakelseEndringKunneIkkeBehandles.UventetFeil(it) }
            .flatten()

    private fun lagreSomBehandlet(deltaker: Tiltaksdeltaker, resultat: TiltaksdeltakelseEndringBehandlet) {
        sessionFactory.withTransactionContext { tx ->
            if (resultat is TiltaksdeltakelseEndringBehandlet.RevurderingOpprettet) {
                startRevurderingService.lagre(resultat.forberedtRevurdering, tx)
            }
            tiltaksdeltakerHendelseRepo.lagreBehandletEndring(
                tiltaksdeltakerHendelse = TiltaksdeltakerHendelse(
                    id = TiltaksdeltakerHendelseId.random(),
                    internDeltakerId = deltaker.id,
                    eksternDeltakerId = deltaker.eksternId,
                    sakId = deltaker.sakId,
                ),
                nåtilstand = resultat.nåtilstand,
                endring = resultat.endring,
                behandlingId = (resultat as? TiltaksdeltakelseEndringBehandlet.RevurderingOpprettet)?.revurderingId,
                oppgaveId = (resultat as? TiltaksdeltakelseEndringBehandlet.OppgaveOpprettet)?.oppgaveId,
                sessionContext = tx,
            )
            tiltaksdeltakerRepo.markerEndringSomBehandlet(
                deltaker.id,
                deltaker.sisteUbehandletEndringTidspunkt!!,
                tx,
            )
        }
    }

    /**
     * Vurderer endringen mot saken.
     * Gosys-oppgaver opprettes her, mens en revurdering bare bygges og lagres sammen med kvitteringen i [lagreSomBehandlet].
     */
    private suspend fun vurderEndring(deltaker: Tiltaksdeltaker): Either<TiltaksdeltakelseEndringKunneIkkeBehandles, TiltaksdeltakelseEndringBehandlet> {
        val logIder =
            "sakId ${deltaker.sakId} / intern deltakerId ${deltaker.id} / ekstern deltakerId ${deltaker.eksternId}"

        val sisteUbehandletEndringTidspunkt = deltaker.sisteUbehandletEndringTidspunkt!!
        val sak = sakRepo.hentForSakId(deltaker.sakId)!!

        sak.oppdaterAutomatiskeSøknadsbehandlingerPåVent(deltaker.id)

        val oppdatertDeltakelse = tiltaksdeltakelseKlient.hentTiltaksdeltakelse(
            fnr = sak.fnr,
            eksternDeltakerId = deltaker.eksternId,
            correlationId = CorrelationId.generate(),
        ).getOrElse { feil ->
            // Markøren står igjen, slik at endringen prøves på nytt ved neste kjøring.
            feil.loggFeil(log, "henting av nå-tilstand for tiltaksdeltakelse", logIder)
            return TiltaksdeltakelseEndringKunneIkkeBehandles.KunneIkkeHenteNåtilstand.left()
        }?.tilLesbarNåtilstand(clock)

        if (oppdatertDeltakelse == null) {
            // Enten finnes ikke deltakelsen i historikken, eller den har ukjent tiltakstype/kildestatus og kan ikke tolkes.
            log.info { "Fant ingen lesbar nå-tilstand for deltakelsen i tiltakshistorikken: $logIder" }
            return TiltaksdeltakelseEndringBehandlet.IngenLesbarNåtilstand.right()
        }

        val vurdertEndring = when (val vurdering = sak.finnEndringerMotGjeldendeVedtak(deltaker.id, oppdatertDeltakelse, clock)) {
            VurdertTiltaksdeltakerEndring.IngenEndring -> {
                log.info { "Fant ingen relevante endringer for $logIder" }
                return TiltaksdeltakelseEndringBehandlet.IngenRelevantEndring(oppdatertDeltakelse).right()
            }

            is VurdertTiltaksdeltakerEndring.Endret -> vurdering
        }

        val endring = vurdertEndring.endring

        if (endring is TiltaksdeltakerEndring.AvsluttetSomForventet) {
            log.info { "Tiltaksdeltakelsen er avsluttet som forventet, gjør ingenting for $logIder" }
            return TiltaksdeltakelseEndringBehandlet.AvsluttetSomForventet(oppdatertDeltakelse).right()
        }

        val revurderingSomSkalOpprettes = vurdertEndring.automatiskRevurdering

        return if (revurderingSomSkalOpprettes != null) {
            log.info { "Tiltaksdeltakelse er endret, oppretter revurdering ($logIder)" }
            forberedRevurdering(
                sak,
                revurderingSomSkalOpprettes,
                endring,
                oppdatertDeltakelse,
                logIder,
            ).right()
        } else {
            log.info { "Tiltaksdeltakelse er endret uten å opprette revurdering, oppretter oppgave ($logIder)" }
            opprettOppgave(sak, endring, oppdatertDeltakelse, sisteUbehandletEndringTidspunkt, logIder)
        }
    }

    // Oppdaterer venterTil for alle åpne søknadsbehandlinger som er under automatisk behandling og satt på vent, slik at de vurderes på nytt etter endring på deltaker
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

    private suspend fun forberedRevurdering(
        sak: Sak,
        revurderingSomSkalOpprettes: AutomatiskRevurderingAvEndring,
        endring: TiltaksdeltakerEndring,
        oppdatertTiltaksdeltakelse: Tiltaksdeltakelse.GirRett,
        logIder: String,
    ): TiltaksdeltakelseEndringBehandlet.RevurderingOpprettet {
        val kommando = StartRevurderingKommando(
            sakId = sak.id,
            correlationId = CorrelationId.generate(),
            saksbehandler = null,
            revurderingType = revurderingSomSkalOpprettes.type,
            vedtakIdSomOmgjøres = (revurderingSomSkalOpprettes as? AutomatiskRevurderingAvEndring.Omgjøring)?.vedtakIdSomOmgjøres,
            klagebehandlingId = null,
            automatiskOpprettetGrunn = AutomatiskOpprettetRevurderingGrunn(endring = endring),
        )

        val forberedtRevurdering = startRevurderingService.forberedRevurdering(kommando, sak).getOrElse { feil ->
            throw IllegalStateException(
                "Uventet feil ved automatisk start av revurdering: ${feil.loggkontekst.melding} " +
                    "(saksnummer ${sak.saksnummer} / correlationId ${kommando.correlationId} / $logIder)",
            )
        }

        val revurdering = forberedtRevurdering.revurdering

        log.info { "Bygget revurdering med id ${revurdering.id} / type ${revurdering.resultat::class.simpleName} for endret tiltaksdeltakelse, lagres sammen med kvitteringen: $logIder" }
        return TiltaksdeltakelseEndringBehandlet.RevurderingOpprettet(
            forberedtRevurdering = forberedtRevurdering,
            nåtilstand = oppdatertTiltaksdeltakelse,
            endring = endring,
        )
    }

    private suspend fun opprettOppgave(
        sak: Sak,
        endring: TiltaksdeltakerEndring,
        oppdatertTiltaksdeltakelse: Tiltaksdeltakelse.GirRett,
        sisteEndring: LocalDateTime,
        logIder: String,
    ): Either<TiltaksdeltakelseEndringKunneIkkeBehandles.KunneIkkeOppretteOppgave, TiltaksdeltakelseEndringBehandlet.OppgaveOpprettet> {
        val tilleggstekst = endring.getOppgaveTilleggstekst()

        return oppgaveKlient.opprettOppgaveUtenDuplikatkontroll(
            fnr = sak.fnr,
            oppgavebehov = Oppgavebehov.ENDRET_TILTAKDELTAKER,
            tilleggstekst = tilleggstekst,
        ).map { oppgaveId ->
            log.info { "Opprettet Gosys-oppgave med id $oppgaveId for endret tiltaksdeltakelse: $logIder" }
            val oppgave = EksternOppgave(
                oppgaveId = oppgaveId,
                sakId = sak.id,
                opprettet = nå(clock),
                grunnlag = Oppgavegrunnlag.EndretTiltaksdeltakelse(
                    kilde = Kilde.Tiltakshistorikk(sisteUbehandletEndring = sisteEndring),
                    verdi = oppdatertTiltaksdeltakelse,
                ),
                tilleggstekst = tilleggstekst,
            )

            // Vi lagrer oppgaven umiddelbart utenfor transaksjonen som markerer endringen som behandlet, slik at vi ikke mister referansen til oppgaven dersom transaksjonen feiler.
            eksternOppgaveRepo.lagre(oppgave)
            log.info { "Lagret oppgaveId $oppgaveId for tiltaksdeltakelse: $logIder" }
            TiltaksdeltakelseEndringBehandlet.OppgaveOpprettet(
                oppgaveId = oppgaveId,
                nåtilstand = oppdatertTiltaksdeltakelse,
                endring = endring,
            )
        }.mapLeft { feil ->
            feil.loggFeil(log, "opprettelse av gosysoppgave for endret tiltaksdeltakelse", logIder)
            TiltaksdeltakelseEndringKunneIkkeBehandles.KunneIkkeOppretteOppgave
        }
    }

    companion object {
        // Venter på eventuelle flere hendelser for samme deltakelse før nå-tilstanden hentes.
        const val MINUTTER_FORSINKELSE: Long = 15L
    }
}

/**
 * Endringen er ferdig behandlet, og markøren kan nullstilles.
 * [nåtilstand] og [endring] er det jobben vurderte, og lagres for sporbarhet.
 */
sealed interface TiltaksdeltakelseEndringBehandlet {
    val nåtilstand: Tiltaksdeltakelse.GirRett?
    val endring: TiltaksdeltakerEndring?

    data object IngenLesbarNåtilstand : TiltaksdeltakelseEndringBehandlet {
        override val nåtilstand = null
        override val endring = null
    }

    data class IngenRelevantEndring(
        override val nåtilstand: Tiltaksdeltakelse.GirRett,
    ) : TiltaksdeltakelseEndringBehandlet {
        override val endring = null
    }

    data class AvsluttetSomForventet(
        override val nåtilstand: Tiltaksdeltakelse.GirRett,
    ) : TiltaksdeltakelseEndringBehandlet {
        override val endring = TiltaksdeltakerEndring.AvsluttetSomForventet
    }

    /** Revurderingen i [forberedtRevurdering] lagres i samme transaksjon som markøren nullstilles. */
    data class RevurderingOpprettet(
        val forberedtRevurdering: ForberedtRevurdering,
        override val nåtilstand: Tiltaksdeltakelse.GirRett,
        override val endring: TiltaksdeltakerEndring,
    ) : TiltaksdeltakelseEndringBehandlet {
        val revurderingId: RammebehandlingId get() = forberedtRevurdering.revurdering.id
    }

    data class OppgaveOpprettet(
        val oppgaveId: OppgaveId,
        override val nåtilstand: Tiltaksdeltakelse.GirRett,
        override val endring: TiltaksdeltakerEndring,
    ) : TiltaksdeltakelseEndringBehandlet
}

/** Endringen kunne ikke behandles, og markøren står igjen til neste kjøring. */
sealed interface TiltaksdeltakelseEndringKunneIkkeBehandles {
    data object KunneIkkeHenteNåtilstand : TiltaksdeltakelseEndringKunneIkkeBehandles

    data object KunneIkkeOppretteOppgave : TiltaksdeltakelseEndringKunneIkkeBehandles

    data class UventetFeil(val throwable: Throwable) : TiltaksdeltakelseEndringKunneIkkeBehandles
}
