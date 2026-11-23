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
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.ForberedtRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.DelautomatiskForlengelseService
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.DelautomatiskOmgjøringService
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.DelautomatiskRevurderingJobb
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.DelautomatiskStansService
import no.nav.tiltakspenger.saksbehandling.felles.Korrelasjon
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
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.TiltakshistorikkId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.TiltakshistorikkRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.Tiltakshistorikkinnslag
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.erEndretFra
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.loggFeil
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.tilLesbarNåtilstand
import java.time.Clock
import java.time.LocalDateTime

/**
 * Hendelsene tolkes ikke — consumerne setter bare en markør ([Tiltaksdeltaker.sisteUbehandletEndringTidspunkt]) på deltakeren, og jobben henter nå-tilstanden for deltakelsen ferskt fra tiltakshistorikk-tjenesten.
 * Nå-tilstanden sammenlignes med saken, og relevante endringer fører til at en revurdering opprettes automatisk.
 * Automatiske søknadsbehandlinger og forlengelser på vent får fremskyndet ny vurdering når deltakelsen endres.
 * Endringer som ikke kan revurderes automatisk fører til en Gosys-oppgave inntil erstatteren er på plass.
 * Har saken en åpen ubestemt revurdering, legges endringen i stedet til som en ny grunn i den.
 * Er den ubestemte revurderingen ikke tildelt en saksbehandler, hentes også saksopplysningene på nytt.
 * Referansen til oppgaven lagres som en [EksternOppgave] før markøren nullstilles.
 * Nå-tilstanden lagres i tiltakshistorikken når den er endret siden forrige innslag for deltakeren, eller når den førte til en revurdering eller en oppgave.
 * Den lagres i samme transaksjon som markøren nullstilles.
 * En automatisk revurdering eller en ny grunn i en ubestemt revurdering lagres også i den transaksjonen, slik at revurderingen, tiltakshistorikken og kvitteringen lagres samlet eller ikke i det hele tatt.
 * En automatisk stans, forlengelse eller omgjøring ved avkortet sluttdato opprettes under automatisk behandling og behandles videre av [DelautomatiskRevurderingJobb].
 */
class OppdatertTiltaksdeltakelseJobb(
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
    private val sakRepo: SakRepo,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val tiltaksdeltakelseKlient: TiltaksdeltakelseKlient,
    private val startRevurderingService: StartRevurderingService,
    private val delautomatiskStansService: DelautomatiskStansService,
    private val delautomatiskForlengelseService: DelautomatiskForlengelseService,
    private val delautomatiskOmgjøringService: DelautomatiskOmgjøringService,
    private val oppgaveKlient: OppgaveKlient,
    private val eksternOppgaveRepo: EksternOppgaveRepo,
    private val tiltakshistorikkRepo: TiltakshistorikkRepo,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
    private val korrelasjon: Korrelasjon,
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
        korrelasjon.medNyCorrelationId { correlationId ->
            Either.catch {
                // Tas før nå-tilstanden hentes, slik at innslaget ikke ser nyere ut enn saksopplysninger som hentes etter den.
                val innhentet = nå(clock)
                vurderEndring(deltaker, correlationId).onRight { resultat ->
                    lagreSomBehandlet(deltaker, resultat, innhentet)
                }
            }.mapLeft { TiltaksdeltakelseEndringKunneIkkeBehandles.UventetFeil(it) }
                .flatten()
        }

    private fun lagreSomBehandlet(
        deltaker: Tiltaksdeltaker,
        resultat: TiltaksdeltakelseEndringBehandlet,
        innhentet: LocalDateTime,
    ) {
        sessionFactory.withTransactionContext { tx ->
            when (resultat) {
                is TiltaksdeltakelseEndringBehandlet.RevurderingOpprettet -> startRevurderingService.lagre(resultat.forberedtRevurdering, tx)

                // Markøren står igjen dersom revurderingen er konvertert eller avsluttet siden saken ble hentet, slik at endringen vurderes på nytt ved neste kjøring.
                is TiltaksdeltakelseEndringBehandlet.GrunnLagtTilUbestemtRevurdering -> check(rammebehandlingRepo.lagreAutomatiskOpprettedeGrunner(resultat.revurdering, tx)) {
                    "Revurdering ${resultat.revurderingId} er ikke lenger en åpen ubestemt revurdering, prøver igjen ved neste kjøring"
                }

                is TiltaksdeltakelseEndringBehandlet.IngenLesbarNåtilstand,
                is TiltaksdeltakelseEndringBehandlet.IngenRelevantEndring,
                is TiltaksdeltakelseEndringBehandlet.AvsluttetSomForventet,
                is TiltaksdeltakelseEndringBehandlet.OppgaveOpprettet,
                -> Unit
            }
            lagreTiltakshistorikk(deltaker, resultat, innhentet, tx)
            tiltaksdeltakerRepo.markerEndringSomBehandlet(
                deltaker.id,
                deltaker.sisteUbehandletEndringTidspunkt!!,
                tx,
            )
        }
    }

    /**
     * Et innslag uten nå-tilstand lagres aldri, siden det ikke har noen verdi å vise eller sammenligne med.
     * Uendret nå-tilstand lagres bare når den likevel førte til en revurdering eller en oppgave.
     * [innhentet] er tidspunktet før nå-tilstanden ble hentet, og ikke når innslaget lagres.
     * Ellers ville saksopplysninger hentet i samme kjøring, f.eks. for en ubestemt revurdering, sett utdaterte ut.
     */
    private fun lagreTiltakshistorikk(
        deltaker: Tiltaksdeltaker,
        resultat: TiltaksdeltakelseEndringBehandlet,
        innhentet: LocalDateTime,
        tx: TransactionContext,
    ) {
        val nåtilstand = resultat.nåtilstand ?: return
        val innslag = Tiltakshistorikkinnslag(
            id = TiltakshistorikkId.random(),
            sakId = deltaker.sakId,
            tiltaksdeltakerId = deltaker.id,
            eksternDeltakerId = deltaker.eksternId,
            innhentet = innhentet,
            verdi = nåtilstand,
            endring = resultat.endring,
            behandlingId = when (resultat) {
                is TiltaksdeltakelseEndringBehandlet.RevurderingOpprettet -> resultat.revurderingId
                is TiltaksdeltakelseEndringBehandlet.GrunnLagtTilUbestemtRevurdering -> resultat.revurderingId
                else -> null
            },
            oppgaveId = (resultat as? TiltaksdeltakelseEndringBehandlet.OppgaveOpprettet)?.oppgaveId,
        )
        val forrigeVerdi = tiltakshistorikkRepo.hentSisteVerdi(deltaker.id, tx)
        if (innslag.førteTilBehandlingEllerOppgave || nåtilstand.erEndretFra(forrigeVerdi)) {
            tiltakshistorikkRepo.lagre(innslag, tx)
        }
    }

    /**
     * Vurderer endringen mot saken.
     * Gosys-oppgaver opprettes her, mens en revurdering bare bygges og lagres sammen med kvitteringen i [lagreSomBehandlet].
     */
    private suspend fun vurderEndring(
        deltaker: Tiltaksdeltaker,
        correlationId: CorrelationId,
    ): Either<TiltaksdeltakelseEndringKunneIkkeBehandles, TiltaksdeltakelseEndringBehandlet> {
        val logIder =
            "sakId ${deltaker.sakId} / intern deltakerId ${deltaker.id} / ekstern deltakerId ${deltaker.eksternId}"

        val sisteUbehandletEndringTidspunkt = deltaker.sisteUbehandletEndringTidspunkt!!
        val sak = sakRepo.hentForSakId(deltaker.sakId)!!

        sak.oppdaterAutomatiskeBehandlingerPåVent(deltaker.id)

        val oppdatertDeltakelse = tiltaksdeltakelseKlient.hentTiltaksdeltakelse(
            fnr = sak.fnr,
            eksternDeltakerId = deltaker.eksternId,
            correlationId = correlationId,
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

        val vurdertEndring = when (val vurdering = sak.finnEndringerForDeltakelse(deltaker.id, oppdatertDeltakelse, clock)) {
            VurdertTiltaksdeltakerEndring.IngenEndring -> {
                log.info { "Fant ingen relevante endringer for $logIder" }
                return TiltaksdeltakelseEndringBehandlet.IngenRelevantEndring(oppdatertDeltakelse).right()
            }

            is VurdertTiltaksdeltakerEndring.EndretMedÅpenUbestemtRevurdering -> {
                log.info { "Tiltaksdeltakelse er endret mens saken har en åpen ubestemt revurdering, legger endringen til som grunn i revurdering ${vurdering.revurdering.id} ($logIder)" }
                return TiltaksdeltakelseEndringBehandlet.GrunnLagtTilUbestemtRevurdering(
                    revurdering = vurdering.revurdering.leggTilAutomatiskOpprettetGrunn(
                        grunn = AutomatiskOpprettetRevurderingGrunn(endring = vurdering.endring, tiltaksdeltakerId = deltaker.id, lagtTil = nå(clock)),
                        oppdaterteSaksopplysninger = when (vurdering.revurdering.saksbehandler) {
                            null -> hentSaksopplysingerService.hentSaksopplysningerFraRegistre(
                                fnr = sak.fnr,
                                correlationId = correlationId,
                                tiltaksdeltakelserDetErSøktTiltakspengerFor = sak.tiltaksdeltakelserDetErSøktTiltakspengerFor,
                                aktuelleTiltaksdeltakelserForBehandlingen = sak.tiltaksdeltakelserDetErSøktTiltakspengerFor.map { it.søknadstiltak.tiltaksdeltakerId }.distinct(),
                                inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = false,
                                sakId = sak.id,
                                saksnummer = sak.saksnummer,
                                behandlingId = vurdering.revurdering.id,
                            )

                            // Saksbehandler oppdaterer selv saksopplysningene på en revurdering hen har tatt.
                            else -> null
                        },
                    ),
                    nåtilstand = oppdatertDeltakelse,
                    endring = vurdering.endring,
                ).right()
            }

            is VurdertTiltaksdeltakerEndring.Endret -> vurdering
        }

        val endring = vurdertEndring.endring

        if (endring is TiltaksdeltakerEndring.AvsluttetSomForventet) {
            log.info { "Tiltaksdeltakelsen er avsluttet som forventet, gjør ingenting for $logIder" }
            return TiltaksdeltakelseEndringBehandlet.AvsluttetSomForventet(oppdatertDeltakelse).right()
        }

        val revurderingSomSkalOpprettes = vurdertEndring.automatiskRevurdering

        if (revurderingSomSkalOpprettes != null) {
            log.info { "Tiltaksdeltakelse er endret, oppretter revurdering ($logIder)" }
            forberedRevurdering(
                sak,
                revurderingSomSkalOpprettes,
                AutomatiskOpprettetRevurderingGrunn(endring = endring, tiltaksdeltakerId = deltaker.id, lagtTil = nå(clock)),
                oppdatertDeltakelse,
                logIder,
                correlationId,
            )?.let { return it.right() }
            log.info { "Kunne ikke opprette revurdering for endringen, oppretter oppgave ($logIder)" }
        } else {
            log.info { "Tiltaksdeltakelse er endret uten å opprette revurdering, oppretter oppgave ($logIder)" }
        }
        return opprettOppgave(sak, endring, oppdatertDeltakelse, sisteUbehandletEndringTidspunkt, logIder)
    }

    /**
     * Oppdaterer venterTil for alle åpne søknadsbehandlinger og forlengelser for deltakeren som er under automatisk behandling og satt på vent.
     * Slik vurderes de på nytt med den oppdaterte deltakelsen, siden saksopplysningene hentes på nytt når de vekkes.
     * Det opprettes ingen ny revurdering for endringen så lenge forlengelsen er åpen, se [finnEndringerForDeltakelse].
     */
    private fun Sak.oppdaterAutomatiskeBehandlingerPåVent(tiltaksdeltakerId: TiltaksdeltakerId) {
        val nyVenterTil = nå(clock).plusMinutes(MINUTTER_FORSINKELSE)
        val søknadsbehandlinger = rammebehandlinger.åpneSøknadsbehandlinger
            .filter { it.søknad.tiltak?.tiltaksdeltakerId == tiltaksdeltakerId && it.erUnderAutomatiskBehandling && it.ventestatus.erSattPåVent }
            .map { it.oppdaterVenterTil(nyVenterTil = nyVenterTil, clock = clock) }
        val forlengelser = rammebehandlinger.åpneBehandlinger
            .filterIsInstance<Revurdering>()
            .filter {
                it.automatiskOpprettetGrunn?.tiltaksdeltakerId == tiltaksdeltakerId &&
                    it.resultat is Revurderingsresultat.Innvilgelse &&
                    it.erUnderAutomatiskBehandling &&
                    it.ventestatus.erSattPåVent
            }
            .map { it.oppdaterVenterTil(nyVenterTil = nyVenterTil, clock = clock) }
        (søknadsbehandlinger + forlengelser).forEach {
            rammebehandlingRepo.lagre(it)
            log.info { "Har oppdatert venterTil for automatisk behandling med id ${it.id} pga endring på deltaker med intern id $tiltaksdeltakerId" }
        }
    }

    /**
     * En stans, forlengelse eller omgjøring ved avkortet sluttdato opprettes under automatisk behandling og behandles videre av [DelautomatiskRevurderingJobb].
     * Andre omgjøringer og ubestemte revurderinger opprettes klare til behandling for en saksbehandler.
     * En stans kan i stedet bli en omgjøring klar til behandling, der saksbehandler fyller ut opphøret, se [DelautomatiskStansService.opprettStansEllerOpphør].
     *
     * @return null dersom revurderingen ikke kan opprettes, og endringen må følges opp med en oppgave.
     */
    private suspend fun forberedRevurdering(
        sak: Sak,
        revurderingSomSkalOpprettes: AutomatiskRevurderingAvEndring,
        automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn,
        oppdatertTiltaksdeltakelse: Tiltaksdeltakelse.GirRett,
        logIder: String,
        correlationId: CorrelationId,
    ): TiltaksdeltakelseEndringBehandlet.RevurderingOpprettet? {
        val forberedtRevurdering = when (revurderingSomSkalOpprettes) {
            AutomatiskRevurderingAvEndring.StansEllerOpphør -> delautomatiskStansService.opprettStansEllerOpphør(sak, automatiskOpprettetGrunn, correlationId)
                ?: return null

            AutomatiskRevurderingAvEndring.Innvilgelse -> delautomatiskForlengelseService.opprett(sak, automatiskOpprettetGrunn, correlationId)

            is AutomatiskRevurderingAvEndring.OmgjøringAvAvkortetSluttdato -> delautomatiskOmgjøringService.opprett(
                sak = sak,
                automatiskOpprettetGrunn = automatiskOpprettetGrunn,
                vedtakIdSomOmgjøres = revurderingSomSkalOpprettes.vedtakIdSomOmgjøres,
                correlationId = correlationId,
            )

            is AutomatiskRevurderingAvEndring.Omgjøring -> startRevurderingService.forberedRevurdering(
                StartRevurderingKommando.Omgjøring(
                    sakId = sak.id,
                    correlationId = correlationId,
                    saksbehandler = null,
                    vedtakIdSomOmgjøres = revurderingSomSkalOpprettes.vedtakIdSomOmgjøres,
                    klagebehandlingId = null,
                    automatiskOpprettetGrunn = automatiskOpprettetGrunn,
                ),
                sak,
            ).getOrElse { feil ->
                throw IllegalStateException(
                    "Uventet feil ved automatisk start av revurdering: ${feil.loggkontekst.melding} " +
                        "(saksnummer ${sak.saksnummer} / correlationId $correlationId / $logIder)",
                )
            }

            AutomatiskRevurderingAvEndring.Ubestemt -> startRevurderingService.forberedRevurdering(
                StartRevurderingKommando.Ubestemt(
                    sakId = sak.id,
                    correlationId = correlationId,
                    automatiskOpprettetGrunn = automatiskOpprettetGrunn,
                ),
                sak,
            ).getOrElse { feil ->
                throw IllegalStateException(
                    "Uventet feil ved automatisk start av ubestemt revurdering: ${feil.loggkontekst.melding} " +
                        "(saksnummer ${sak.saksnummer} / correlationId $correlationId / $logIder)",
                )
            }
        }

        val revurdering = forberedtRevurdering.revurdering

        log.info { "Bygget revurdering med id ${revurdering.id} / type ${revurdering.resultat::class.simpleName} for endret tiltaksdeltakelse, lagres sammen med kvitteringen: $logIder" }
        return TiltaksdeltakelseEndringBehandlet.RevurderingOpprettet(
            forberedtRevurdering = forberedtRevurdering,
            nåtilstand = oppdatertTiltaksdeltakelse,
            endring = automatiskOpprettetGrunn.endring,
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
 * [nåtilstand] og [endring] er det jobben vurderte, og lagres i tiltakshistorikken.
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

    /** Revurderingen i [revurdering] har fått endringen som ny grunn, og grunnene lagres i samme transaksjon som markøren nullstilles. */
    data class GrunnLagtTilUbestemtRevurdering(
        val revurdering: Revurdering,
        override val nåtilstand: Tiltaksdeltakelse.GirRett,
        override val endring: TiltaksdeltakerEndring,
    ) : TiltaksdeltakelseEndringBehandlet {
        val revurderingId: RammebehandlingId get() = revurdering.id
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
