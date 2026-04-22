package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.backoff.shouldRetry
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.opprettAutomatiskMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.tilStatistikkMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Åpningstider.erInnenforØkonomisystemetsÅpningstider
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class AutomatiskMeldekortbehandlingService(
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
) {
    private val logger = KotlinLogging.logger { }

    suspend fun behandleBrukersMeldekort(clock: Clock) {
        if (!erInnenforØkonomisystemetsÅpningstider(clock)) {
            return
        }

        val meldekortListe = Either.catch {
            brukersMeldekortRepo.hentMeldekortSomSkalBehandlesAutomatisk()
        }.getOrElse {
            logger.error(it) { "Feil ved henting av meldekort til automatisk behandling - ${it.message}" }
            return
        }

        logger.debug { "Fant ${meldekortListe.size} meldekort som skal behandles automatisk" }

        meldekortListe.forEach { meldekort ->
            if (skalHoppeOverMeldekort(meldekort, clock)) {
                return@forEach
            }

            behandleMeldekort(meldekort, clock).onLeft {
                logger.error(it) {
                    "Ukjent feil ved automatisk behandling av meldekort fra bruker ${meldekort.id} - ${it.message}"
                }
            }
        }
    }

    /**
     * Logikk for å vente på å behandle brukers meldekort på nytt dersom meldekortet ikke kunne behandles grunnet en ukjent feil.
     * Dette er for at ved en feilsituasjon på et perfekt tidspunkt ikke skal føre til at det plutselig blir mange meldekort
     * som saksbehandlerne må behandle manuelt, som egentlig fint kunne ha blitt behandlet automatisk.
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
     * Behandler ett meldekort.
     *
     * - Domenefeil kommer som en left med [MeldekortBehandletAutomatiskStatus] fra [opprettMeldekortbehandling].
     * - Uventede exceptions fanges og håndteres per meldekort, slik at resten kan fortsette.
     */
    private suspend fun behandleMeldekort(
        meldekort: BrukersMeldekort,
        clock: Clock,
    ): Either<Throwable, Unit> {
        val id = meldekort.id
        val sakId = meldekort.sakId

        val sak = sakRepo.hentForSakId(sakId)

        require(sak != null) {
            "Kunne ikke hente sak for mottatt meldekort med id $id, sakId $sakId,"
        }

        return Either.catch {
            opprettMeldekortbehandling(meldekort, sak, clock)
                .onLeft { status ->
                    håndterOpprettBehandlingFeil(status, meldekort, sak, clock)
                }
                .onRight { behandling ->
                    logger.info { "Opprettet automatisk behandling ${behandling.id} for brukers meldekort $id på sak $sakId" }
                }
            return@catch
        }.onLeft {
            logger.error(it) { "Ukjent feil ved automatisk behandling av meldekort fra bruker $id - ${it.message}" }
            håndterOpprettBehandlingFeil(MeldekortBehandletAutomatiskStatus.UKJENT_FEIL, meldekort, sak, clock)
        }
    }

    private suspend fun håndterOpprettBehandlingFeil(
        status: MeldekortBehandletAutomatiskStatus,
        meldekort: BrukersMeldekort,
        sak: Sak,
        clock: Clock,
    ) {
        with("Kunne ikke opprette automatisk behandling for brukers meldekort ${meldekort.id} på sak ${meldekort.sakId} - Status: $status") {
            if (status.loggesSomError) {
                logger.error { this }
            } else {
                logger.info { this }
            }
        }

        val oppgaveOpprettet = opprettOppgaveHvisAdressebeskyttetEllerSkjermetBruker(sak.fnr, meldekort.journalpostId)

        brukersMeldekortRepo.oppdaterAutomatiskBehandletStatus(
            meldekortId = meldekort.id,
            status = status,
            behandlesAutomatisk = !oppgaveOpprettet && status.skalPrøvePåNytt(),
            metadata = meldekort.behandletAutomatiskForsøkshistorikk.inkrementer(clock = clock),
        )
    }

    private suspend fun opprettMeldekortbehandling(
        meldekort: BrukersMeldekort,
        sak: Sak,
        clock: Clock,
    ): Either<MeldekortBehandletAutomatiskStatus, MeldekortBehandletAutomatisk> {
        val meldekortId = meldekort.id

        if (sak.revurderinger.harÅpenRevurdering()) {
            return MeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING.left()
        }

        val navkontor = Either.catch {
            navkontorService.hentOppfolgingsenhet(sak.fnr)
        }.getOrElse {
            with("Kunne ikke hente navkontor for sak ${sak.id}") {
                logger.error(it) { this }
                Sikkerlogg.error(it) { "$this - fnr ${sak.fnr.verdi}" }
            }
            return MeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET.left()
        }

        val (meldekortbehandling, simulering) = sak.opprettAutomatiskMeldekortbehandling(
            brukersMeldekort = meldekort,
            navkontor = navkontor,
            clock = clock,
            simuler = { behandling ->
                simulerService.simulerMeldekort(
                    behandling,
                    sak.utbetalinger.lastOrNull(),
                    sak.meldeperiodeKjeder,
                    sak.kanSendeInnHelgForMeldekort,
                ) { navkontor }
            },
        ).getOrElse {
            return it.left()
        }

        meldekortbehandling.validerKanIverksetteUtbetaling().onLeft {
            when (it) {
                KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeFeilutbetaling -> {
                    logger.error { "Behandling av brukers meldekort $meldekortId viser feilutbetaling i simuleringen" }
                    return MeldekortBehandletAutomatiskStatus.HAR_FEILUTBETALING.left()
                }

                KanIkkeIverksetteUtbetaling.JusteringStøttesIkke,
                KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeJustering,
                -> {
                    logger.error { "Behandling av brukers meldekort $meldekortId viser justeringer i simuleringen" }
                    return MeldekortBehandletAutomatiskStatus.HAR_JUSTERING.left()
                }

                // TODO: ikke implemenert for meldekort ennå
                KanIkkeIverksetteUtbetaling.SimuleringMangler,
                KanIkkeIverksetteUtbetaling.KontrollSimuleringHarEndringer,
                -> Unit
            }
        }

        val meldekortvedtak = meldekortbehandling.opprettVedtak(
            forrigeUtbetaling = sak.utbetalinger.lastOrNull(),
            clock = clock,
        )

        Either.catch {
            sak.leggTilMeldekortbehandling(meldekortbehandling)
        }.onLeft {
            logger.error(it) { "Automatisk behandling av brukers meldekort $meldekortId kunne ikke legges til sak ${sak.id}" }
            return MeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK.left()
        }

        Either.catch {
            sak.leggTilMeldekortvedtak(meldekortvedtak)
        }.onLeft {
            logger.error(it) { "Vedtak for automatisk behandling av brukers meldekort $meldekortId kunne ikke legges til sak ${sak.id}" }
            return MeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK.left()
        }

        val statistikkDTO = statistikkService.generer(
            Statistikkhendelser(meldekortbehandling.tilStatistikkMeldekortDTO(clock)),
        )
        sessionFactory.withTransactionContext { tx ->
            meldekortbehandlingRepo.lagre(meldekortbehandling, simulering, tx)
            meldekortvedtakRepo.lagre(meldekortvedtak, tx)
            brukersMeldekortRepo.oppdaterAutomatiskBehandletStatus(
                meldekortId = meldekortId,
                status = MeldekortBehandletAutomatiskStatus.BEHANDLET,
                behandlesAutomatisk = true,
                metadata = meldekort.behandletAutomatiskForsøkshistorikk.inkrementer(clock = clock),
                tx,
            )
            statistikkService.lagre(statistikkDTO, tx)
        }

        return meldekortbehandling.right()
    }

    private suspend fun opprettOppgaveHvisAdressebeskyttetEllerSkjermetBruker(
        fnr: Fnr,
        journalpostId: JournalpostId,
    ): Boolean {
        val pdlPerson = sakService.hentEnkelPersonMedSkjermingForFnr(fnr, CorrelationId.generate()).getOrThrow()

        if (pdlPerson.strengtFortrolig || pdlPerson.strengtFortroligUtland || pdlPerson.fortrolig || pdlPerson.skjermet) {
            logger.info { "Person har adressebeskyttelse eller er skjermet, oppretter oppgave i Gosys" }
            oppgaveKlient.opprettOppgave(
                fnr = fnr,
                journalpostId = journalpostId,
                oppgavebehov = Oppgavebehov.NYTT_MELDEKORT,
            )
            return true
        }

        return false
    }
}

private fun MeldekortBehandletAutomatiskStatus.skalPrøvePåNytt(): Boolean {
    return when (this) {
        MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING,
        MeldekortBehandletAutomatiskStatus.BEHANDLET,
        MeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK,
        MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_GODKJENT_FRAVÆR,
        MeldekortBehandletAutomatiskStatus.INGEN_DAGER_GIR_RETT,
        MeldekortBehandletAutomatiskStatus.HAR_FEILUTBETALING,
        MeldekortBehandletAutomatiskStatus.HAR_JUSTERING,
        MeldekortBehandletAutomatiskStatus.KAN_IKKE_MELDE_HELG,
        MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_REGISTRERT,
        MeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE,
        MeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK,
        MeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK,
        MeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET,
        -> false

        MeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING,
        MeldekortBehandletAutomatiskStatus.HAR_ÅPEN_BEHANDLING,
        MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_FØRSTE_KJEDE,
        MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_NESTE_KJEDE,
        MeldekortBehandletAutomatiskStatus.UKJENT_FEIL,
        MeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET,
        -> true
    }
}

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
