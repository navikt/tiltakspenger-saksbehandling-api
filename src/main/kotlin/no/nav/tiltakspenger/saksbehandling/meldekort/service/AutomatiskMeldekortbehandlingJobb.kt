package no.nav.tiltakspenger.saksbehandling.meldekort.service

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
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.loggkontekst
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.Korrelasjon
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.opprettAutomatiskMeldekortbehandling
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
) {
    private val logger = KotlinLogging.logger { }

    suspend fun behandleBrukersMeldekort(clock: Clock) {
        val åpningsstatus = økonomisystemetsÅpningsstatus(clock)
        if (åpningsstatus == Åpningsstatus.STENGT) {
            return
        }
        // Utenfor antatt åpningstid vet vi ikke om økonomisystemet er åpent, så da må simuleringen gå gjennom.
        val kreverSimulering = åpningsstatus == Åpningsstatus.MULIG_ÅPENT

        val meldekortListe = Either.catch {
            brukersMeldekortRepo.hentMeldekortSomSkalBehandlesAutomatisk()
        }.getOrElse {
            logger.error(it) { "Feil ved henting av meldekort til automatisk behandling - ${it.message}" }
            return
        }

        logger.debug { "Fant ${meldekortListe.size} meldekort som skal behandles automatisk" }

        meldekortListe.forEach { meldekort ->
            val økonomisystemetErStengt = korrelasjon.medNyCorrelationId { correlationId ->
                Either.catch {
                    if (skalHoppeOverMeldekort(meldekort, clock)) {
                        return@catch false
                    }

                    val id = meldekort.id
                    val sakId = meldekort.sakId

                    val sak = sakRepo.hentForSakId(sakId)

                    require(sak != null) {
                        "Fant ikke sak for mottatt meldekort med id $id, sakId $sakId"
                    }

                    opprettMeldekortbehandling(meldekort, sak, kreverSimulering, clock).fold(
                        ifLeft = { ikkeBehandlet ->
                            when (ikkeBehandlet) {
                                is IkkeBehandlet.Feilet -> {
                                    håndterOpprettBehandlingFeil(ikkeBehandlet.status, meldekort, clock, correlationId)
                                    false
                                }

                                IkkeBehandlet.ØkonomisystemetErStengt -> true
                            }
                        },
                        ifRight = { behandling ->
                            logger.info { "Opprettet automatisk behandling ${behandling.id} for brukers meldekort $id på sak $sakId" }
                            false
                        },
                    )
                }.getOrElse {
                    logger.error(it) { "Ukjent feil ved automatisk behandling av meldekort fra bruker ${meldekort.id} - ${it.message}" }
                    håndterOpprettBehandlingFeil(MeldekortBehandletAutomatiskStatus.UKJENT_FEIL, meldekort, clock, correlationId)
                    false
                }
            }

            // Simuleringen vil feile for resten av meldekortene også, så vi venter til neste kjøring.
            // Meldekortet oppdateres ikke, slik at forsøket ikke teller mot maksgrensen for nye forsøk.
            if (økonomisystemetErStengt) {
                logger.info { "Økonomisystemet er stengt utenfor antatt åpningstid - venter med automatisk behandling av meldekort til neste kjøring" }
                return
            }
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

    private suspend fun håndterOpprettBehandlingFeil(
        status: MeldekortBehandletAutomatiskStatus,
        meldekort: BrukersMeldekort,
        clock: Clock,
        correlationId: CorrelationId,
    ) {
        val logMsg =
            "Kunne ikke opprette automatisk behandling for brukers meldekort ${meldekort.id} på sak ${meldekort.sakId} - Status: $status"

        val skalPrøvePåNytt = meldekort.skalPrøvePåNytt(status, clock)

        if (skalPrøvePåNytt) {
            logger.info { "$logMsg - Denne vil prøves på nytt" }
        } else {
            logger.at(status.loggnivå) { message = logMsg }

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
        )
    }

    private suspend fun opprettMeldekortbehandling(
        meldekort: BrukersMeldekort,
        sak: Sak,
        kreverSimulering: Boolean,
        clock: Clock,
    ): Either<IkkeBehandlet, MeldekortBehandletAutomatisk> {
        val meldekortId = meldekort.id
        var simuleringenSvarteStengt = false

        val (meldekortbehandling, simulering) = sak.opprettAutomatiskMeldekortbehandling(
            brukersMeldekort = meldekort,
            hentNavkontor = { fnr ->
                navkontorService.hentNavkontor(
                    fnr = fnr,
                    loggkontekst = "sakId: ${sak.id}, saksnummer: ${sak.saksnummer.verdi}",
                )
            },
            clock = clock,
            kreverSimulering = kreverSimulering,
            simuler = { behandling, navkontor ->
                simulerService.simulerMeldekort(
                    behandling,
                    sak.utbetalinger.lastOrNull(),
                    sak.meldeperiodeKjeder,
                    sak.kanSendeInnHelgForMeldekort,
                ) { navkontor }.onLeft { simuleringenSvarteStengt = it is KunneIkkeSimulere.Stengt }
            },
        ).getOrElse { status ->
            if (kreverSimulering && simuleringenSvarteStengt) {
                return IkkeBehandlet.ØkonomisystemetErStengt.left()
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
                    return IkkeBehandlet.Feilet(status).left()
                }

                is KanIkkeIverksetteUtbetaling.JusteringStøttesIkke,
                KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeJustering,
                -> {
                    val status = MeldekortBehandletAutomatiskStatus.HAR_JUSTERING
                    logger.at(status.loggnivå) {
                        message = "Behandling av brukers meldekort $meldekortId viser justeringer i simuleringen - ${meldekortbehandling.loggkontekst()}"
                    }
                    return IkkeBehandlet.Feilet(status).left()
                }

                // For meldekort krever vi kun simulering i enkelte tilfeller.
                // Dette håndteres i Sak.opprettAutomatiskMeldekortbehandling()
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
        if (nå(clock) > mottatt + MAKS_DELAY_FOR_AUTOMATISK_BEHANDLING) {
            return false
        }

        return nyStatus.kanPrøvesPåNytt
    }

    private sealed interface IkkeBehandlet {
        data class Feilet(val status: MeldekortBehandletAutomatiskStatus) : IkkeBehandlet

        /** Simuleringen svarte at økonomisystemet er stengt, utenfor tidsrommet der det antas å være åpent. */
        data object ØkonomisystemetErStengt : IkkeBehandlet
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

        val MAKS_DELAY_FOR_AUTOMATISK_BEHANDLING = 2.days.toJavaDuration()
    }
}
