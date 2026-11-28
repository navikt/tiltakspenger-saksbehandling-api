package no.nav.tiltakspenger.saksbehandling.meldekort.infra.jobb

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.backoff.shouldRetry
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Task
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.loggkontekst
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.felles.Korrelasjon
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.felles.plusVirkedagstid
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.opprettAutomatiskMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.utledPrioriteringAvKorrigering
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.tilStatistikkMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.MeldekortvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Åpningstider.Åpningsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Åpningstider.økonomisystemetsÅpningsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class AutomatiskMeldekortbehandlingJobb(
    private val brukersMeldekortRepo: BrukersMeldekortRepo,
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakRepo: SakRepo,
    private val meldekortvedtakRepo: MeldekortvedtakRepo,
    private val navkontorService: NavkontorService,
    private val sessionFactory: SessionFactory,
    private val simulerService: SimulerService,
    private val sakService: SakService,
    private val oppgaveKlient: OppgaveKlient,
    private val statistikkService: StatistikkService,
    private val korrelasjon: Korrelasjon,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Settes når simuleringen svarer at økonomisystemet er stengt i mulig åpningstid, slik at vi ikke simulerer på nytt hvert minutt.
     * Pausen blir lengre for hvert stengt-svar på rad, etter [PAUSER_NÅR_ØKONOMISYSTEMET_ER_STENGT].
     * Den nullstilles når en kjøring går gjennom alle meldekortene uten at simuleringen svarer stengt.
     * Pausen ligger i minnet, så hver pod har sin egen pause, og den nullstilles ved omstart.
     */
    @Volatile
    private var pause: Forsøkshistorikk? = null

    suspend fun behandleBrukersMeldekort() = behandleBrukersMeldekort(clock)

    /** Tar egen [clock] for tester som må hoppe fram og tilbake i tid. */
    suspend fun behandleBrukersMeldekort(clock: Clock) {
        if (pause?.let { nå(clock) < it.nesteForsøk } == true) {
            return
        }
        val åpningsstatus = økonomisystemetsÅpningsstatus(clock)
        if (åpningsstatus == Åpningsstatus.STENGT) {
            return
        }
        // Utenfor antatt åpningstid vet vi ikke om økonomisystemet er åpent, så da setter et stengt-svar fra simuleringen jobben på pause.
        val pauseVedStengt = åpningsstatus == Åpningsstatus.MULIG_ÅPENT

        val meldekortListe = Either.catch {
            brukersMeldekortRepo.hentMeldekortSomSkalBehandlesAutomatisk()
        }.getOrElse {
            logger.error(it) { "Feil ved henting av meldekort til automatisk behandling - ${it.message}" }
            return
        }

        logger.debug { "Fant ${meldekortListe.size} meldekort som skal behandles automatisk" }

        meldekortListe.forEach { meldekort ->
            val økonomisystemetErStengt = korrelasjon.medNyCorrelationId { correlationId ->
                val id = meldekort.id
                val sakId = meldekort.sakId

                // Hentes utenfor catch-blokken, slik at feilhåndteringen kan prioritere meldekortet også ved ukjente feil.
                val sak: Lazy<Sak> = lazy {
                    requireNotNull(sakRepo.hentForSakId(sakId)) {
                        "Fant ikke sak for mottatt meldekort med id $id, sakId $sakId"
                    }
                }

                Either.catch {
                    if (skalHoppeOverMeldekort(meldekort, clock)) {
                        return@catch false
                    }

                    opprettMeldekortbehandling(meldekort, sak.value, clock).fold(
                        ifLeft = { ikkeBehandlet ->
                            when (ikkeBehandlet) {
                                // Forsøket lagres ikke, slik at det ikke teller mot maksgrensen for nye forsøk.
                                is IkkeBehandlet.ØkonomisystemetErStengt if pauseVedStengt -> true

                                // I antatt åpningstid regnes stengt-svaret som et vanlig feilet forsøk.
                                is IkkeBehandlet.ØkonomisystemetErStengt -> {
                                    håndterOpprettBehandlingFeil(ikkeBehandlet.status, meldekort, sak, clock, correlationId)
                                    false
                                }

                                is IkkeBehandlet.Feilet -> {
                                    håndterOpprettBehandlingFeil(
                                        status = ikkeBehandlet.status,
                                        meldekort = meldekort,
                                        sak = sak,
                                        clock = clock,
                                        correlationId = correlationId,
                                        simuleringViserTidligereUtbetaling = ikkeBehandlet.simuleringViserTidligereUtbetaling,
                                    )
                                    false
                                }

                                IkkeBehandlet.SakenMigreres -> {
                                    logger.info { "Venter med automatisk behandling av meldekort $id til saken $sakId er migrert til oppdragsversjon 3" }
                                    false
                                }
                            }
                        },
                        ifRight = { behandling ->
                            logger.info { "Opprettet automatisk behandling ${behandling.id} for brukers meldekort $id på sak $sakId" }
                            false
                        },
                    )
                }.getOrElse {
                    logger.error(it) { "Ukjent feil ved automatisk behandling av meldekort fra bruker ${meldekort.id} - ${it.message}" }
                    håndterOpprettBehandlingFeil(MeldekortBehandletAutomatiskStatus.UKJENT_FEIL, meldekort, sak, clock, correlationId)
                    false
                }
            }

            // Simuleringen vil feile for resten av meldekortene også, så vi setter jobben på pause.
            if (økonomisystemetErStengt) {
                val nyPause = (pause ?: Forsøkshistorikk.opprett(clock = clock))
                    .inkrementer(delays = PAUSER_NÅR_ØKONOMISYSTEMET_ER_STENGT, clock = clock)
                pause = nyPause
                logger.info { "Økonomisystemet er stengt utenfor antatt åpningstid - setter automatisk behandling av meldekort på pause til ${nyPause.nesteForsøk} (pause nr. ${nyPause.antallForsøk} på rad)" }
                return
            }
        }

        pause?.let {
            pause = null
            logger.info { "Simuleringen svarte ikke stengt etter ${it.antallForsøk} pauser på rad - nullstiller pausen for automatisk behandling av meldekort" }
        }
    }

    /**
     * @return true dersom meldekortet ikke skal behandles automatisk ennå
     */
    private fun skalHoppeOverMeldekort(meldekort: BrukersMeldekort, clock: Clock): Boolean {
        val (forrigeForsøk, _, antallForsøk) = meldekort.behandletAutomatiskForsøkshistorikk
        if (forrigeForsøk == null) {
            return false
        }

        val (kanPrøvePåNyttNå) = forrigeForsøk.shouldRetry(
            antallForsøk,
            clock,
            venteIntervallerMap,
            venteIntervaller.last(),
        )

        return !kanPrøvePåNyttNå
    }

    /**
     * Når meldekortet gis opp og går til manuell behandling, prioriteres det i benken dersom det er en korrigering som må behandles raskt for å rekke utbetalingen.
     * @param sak hentes bare dersom meldekortet gis opp.
     * Kan ikke saken hentes, prioriteres ikke meldekortet.
     */
    private suspend fun håndterOpprettBehandlingFeil(
        status: MeldekortBehandletAutomatiskStatus,
        meldekort: BrukersMeldekort,
        sak: Lazy<Sak>,
        clock: Clock,
        correlationId: CorrelationId,
        simuleringViserTidligereUtbetaling: Boolean = false,
    ) {
        val logMsg =
            "Kunne ikke opprette automatisk behandling for brukers meldekort ${meldekort.id} på sak ${meldekort.sakId} - Status: $status"

        val skalPrøvePåNytt = meldekort.skalPrøvePåNytt(status, clock)
        val prioritering = if (skalPrøvePåNytt) {
            null
        } else {
            Either.catch { sak.value }
                .getOrElse {
                    logger.warn(it) { "Kunne ikke hente sak for å vurdere prioritering av brukers meldekort ${meldekort.id} på sak ${meldekort.sakId}" }
                    null
                }
                ?.utledPrioriteringAvKorrigering(meldekort, status, simuleringViserTidligereUtbetaling)
        }

        if (skalPrøvePåNytt) {
            logger.info { "$logMsg - Denne vil prøves på nytt" }
        } else {
            logger.at(status.loggnivå) { message = logMsg + if (prioritering != null) " - Prioriteres i benken" else "" }

            // Ved Left hopper vi over statusoppdateringen under, slik at meldekortet (og dermed gosysoppgaven) prøves på nytt neste kjøring.
            // Oppgaveopprettelsen har allerede logget.
            meldekort.opprettOppgaveHvisAdressebeskyttetEllerSkjermetBruker(correlationId)
                .getOrElse { return }
        }

        brukersMeldekortRepo.oppdaterAutomatiskBehandletStatus(
            meldekortId = meldekort.id,
            status = status,
            behandlesAutomatisk = skalPrøvePåNytt,
            metadata = meldekort.behandletAutomatiskForsøkshistorikk.inkrementer(
                delays = venteIntervaller,
                clock = clock,
            ),
            prioritering = prioritering,
        )
    }

    private suspend fun opprettMeldekortbehandling(
        meldekort: BrukersMeldekort,
        sak: Sak,
        clock: Clock,
    ): Either<IkkeBehandlet, MeldekortBehandletAutomatisk> {
        val meldekortId = meldekort.id
        var simuleringsfeil: KunneIkkeSimulere? = null

        val (meldekortbehandling, simulering) = sak.opprettAutomatiskMeldekortbehandling(
            brukersMeldekort = meldekort,
            hentNavkontor = { fnr ->
                navkontorService.hentNavkontor(
                    fnr = fnr,
                    loggkontekst = "sakId: ${sak.id}, saksnummer: ${sak.saksnummer.verdi}",
                )
            },
            clock = clock,
            simuler = { behandling, navkontor ->
                simulerService.simulerMeldekort(
                    behandling,
                    sak.utbetalinger.lastOrNull(),
                    sak.meldeperiodeKjeder,
                    sak.kanSendeInnHelgForMeldekort,
                ) { navkontor }.onLeft { simuleringsfeil = it }
            },
        ).getOrElse { status ->
            if (simuleringsfeil is KunneIkkeSimulere.Stengt) {
                return IkkeBehandlet.ØkonomisystemetErStengt(status).left()
            }
            if (simuleringsfeil == KunneIkkeSimulere.SakenMigreres) {
                return IkkeBehandlet.SakenMigreres.left()
            }
            return IkkeBehandlet.Feilet(status).left()
        }

        meldekortbehandling.validerKanIverksetteUtbetaling().onLeft {
            when (it) {
                KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeFeilutbetaling -> {
                    val status = MeldekortBehandletAutomatiskStatus.HAR_FEILUTBETALING
                    logger.at(status.loggnivå) {
                        message = "Behandling av brukers meldekort $meldekortId viser feilutbetaling i simuleringen - ${meldekortbehandling.loggkontekst()}"
                    }
                    return IkkeBehandlet.Feilet(status, meldekortbehandling.simuleringViserTidligereUtbetaling()).left()
                }

                is KanIkkeIverksetteUtbetaling.JusteringStøttesIkke,
                KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeJustering,
                -> {
                    val status = MeldekortBehandletAutomatiskStatus.HAR_JUSTERING
                    logger.at(status.loggnivå) {
                        message = "Behandling av brukers meldekort $meldekortId viser justeringer i simuleringen - ${meldekortbehandling.loggkontekst()}"
                    }
                    return IkkeBehandlet.Feilet(status, meldekortbehandling.simuleringViserTidligereUtbetaling()).left()
                }

                // Simuleringen er påkrevd, og håndteres i Sak.opprettAutomatiskMeldekortbehandling().
                KanIkkeIverksetteUtbetaling.SimuleringMangler,
                is KanIkkeIverksetteUtbetaling.KontrollSimuleringHarEndringer,
                -> Unit
            }
        }

        Either.catch {
            sak.leggTilMeldekortbehandling(meldekortbehandling)
        }.onLeft {
            logger.error(it) { "Automatisk behandling av brukers meldekort $meldekortId kunne ikke legges til sak ${sak.id}" }
            return IkkeBehandlet.Feilet(MeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK).left()
        }

        val meldekortvedtak = meldekortbehandling.opprettVedtak(
            forrigeUtbetaling = sak.utbetalinger.lastOrNull(),
            clock = clock,
        )

        Either.catch {
            sak.leggTilMeldekortvedtak(meldekortvedtak)
        }.onLeft {
            logger.error(it) { "Vedtak for automatisk behandling av brukers meldekort $meldekortId kunne ikke legges til sak ${sak.id}" }
            return IkkeBehandlet.Feilet(MeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK).left()
        }

        val statistikkDTO = statistikkService.generer(
            Statistikkhendelser(meldekortbehandling.tilStatistikkMeldekortDTO(clock)),
        )

        sessionFactory.withTransactionContext { tx ->
            meldekortbehandlingRepo.lagre(meldekortbehandling, simulering, tx)
            meldekortvedtakRepo.lagre(meldekortvedtak, tx)
            brukersMeldekortRepo.markerSomAutomatiskBehandlet(
                meldekortId = meldekortId,
                metadata = meldekort.behandletAutomatiskForsøkshistorikk.inkrementer(
                    delays = venteIntervaller,
                    clock = clock,
                ),
                tx,
            )
            statistikkService.lagre(statistikkDTO, tx)
            sakService.markerSkalSendesTilMeldekortApi(sakId = sak.id, sessionContext = tx)
        }

        return meldekortbehandling.right()
    }

    /**
     * Returnerer Left dersom gosysoppgaven ikke kunne opprettes.
     * Logger selv ved feil - kalleren skal ikke logge i tillegg.
     */
    private suspend fun BrukersMeldekort.opprettOppgaveHvisAdressebeskyttetEllerSkjermetBruker(correlationId: CorrelationId): Either<HttpKlientError, Unit> {
        val pdlPerson = sakService
            .hentEnkelPersonMedSkjermingForSakId(this.sakId, correlationId)
            .getOrThrow()

        if (pdlPerson.harAdressebeskyttelseEllerSkjerming) {
            logger.info { "Person har adressebeskyttelse eller er skjermet, oppretter oppgave i Gosys" }
            return oppgaveKlient.opprettOppgave(
                fnr = pdlPerson.fnr,
                journalpostId = this.journalpostId,
                oppgavebehov = Oppgavebehov.NYTT_MELDEKORT,
            ).onLeft { feil ->
                feil.loggFeil(logger, "opprettelse av gosysoppgave for meldekort", "sakId: ${this.sakId}, meldekortId: ${this.id}")
            }.map { }
        }
        return Unit.right()
    }

    private fun BrukersMeldekort.skalPrøvePåNytt(nyStatus: MeldekortBehandletAutomatiskStatus, clock: Clock): Boolean {
        if (nå(clock) > mottatt.plusVirkedagstid(MAKS_DELAY_FOR_AUTOMATISK_BEHANDLING)) {
            return false
        }

        return nyStatus.kanPrøvesPåNytt
    }

    private fun MeldekortBehandletAutomatisk.simuleringViserTidligereUtbetaling(): Boolean =
        simulering?.harTidligereUtbetaling == true

    private sealed interface IkkeBehandlet {
        /**
         * @param simuleringViserTidligereUtbetaling om simuleringen viste at meldeperioden allerede er utbetalt, selv om statusen sier noe annet.
         * Det skjer for en korrigering etter endret rett, som ikke stoppes av at meldeperioden er utbetalt, men kan stoppes av feilutbetaling eller justering.
         */
        data class Feilet(
            val status: MeldekortBehandletAutomatiskStatus,
            val simuleringViserTidligereUtbetaling: Boolean = false,
        ) : IkkeBehandlet

        /**
         * Simuleringen svarte at økonomisystemet er stengt.
         * Kalleren avgjør ut fra åpningsstatusen om det skal gi en pause eller regnes som et feilet forsøk med [status].
         */
        data class ØkonomisystemetErStengt(val status: MeldekortBehandletAutomatiskStatus) : IkkeBehandlet

        /** Saken migreres til oppdragsversjon 3, så meldekortet prøves på nytt uten at forsøket teller. */
        data object SakenMigreres : IkkeBehandlet
    }

    companion object {
        private val venteIntervaller: List<Duration> = listOf(
            1.minutes,
            1.minutes,
            1.minutes,
            1.minutes,
            1.minutes,
            5.minutes,
            15.minutes,
        )

        private val venteIntervallerMap: Map<Long, Duration> =
            venteIntervaller.withIndex().associate { (index, duration) -> index.toLong() + 1 to duration }

        /** Hvor lenge vi prøver på nytt etter at meldekortet ble mottatt, der bare tid på virkedager telles med. */
        val MAKS_DELAY_FOR_AUTOMATISK_BEHANDLING = 2.days.toJavaDuration()

        /** Pausen etter første, andre osv. stengt-svar på rad; etter det siste varer hver pause like lenge som den siste. */
        private val PAUSER_NÅR_ØKONOMISYSTEMET_ER_STENGT: List<Duration> = listOf(
            5.minutes,
            15.minutes,
            30.minutes,
            45.minutes,
            60.minutes,
        )
    }
}

fun (() -> AutomatiskMeldekortbehandlingJobb).tasks(): List<Task> {
    return listOf(
        Task(
            navn = "saksbehandling-jobb-automatisk-meldekortbehandling",
            utfør = { _ ->
                this().behandleBrukersMeldekort()
                TaskResultat.Ferdig
            },
        ),
    )
}
