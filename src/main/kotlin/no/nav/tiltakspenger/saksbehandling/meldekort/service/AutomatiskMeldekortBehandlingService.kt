package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.backoff.shouldRetry
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettAutomatiskMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.tilStatistikkMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Åpningstider.erInnenforØkonomisystemetsÅpningstider
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class AutomatiskMeldekortBehandlingService(
    private val brukersMeldekortRepo: BrukersMeldekortRepo,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val sakRepo: SakRepo,
    private val meldekortvedtakRepo: MeldekortvedtakRepo,
    private val navkontorService: NavkontorService,
    private val sessionFactory: SessionFactory,
    private val simulerService: SimulerService,
    private val personKlient: PersonKlient,
    private val oppgaveKlient: OppgaveKlient,
    private val statistikkMeldekortRepo: StatistikkMeldekortRepo,
) {
    val logger = KotlinLogging.logger { }
    val venteIntervaller: Map<Long, Duration> = mapOf(
        1L to 1.days,
        2L to 2.days,
        3L to 3.days,
    )

    suspend fun behandleBrukersMeldekort(clock: Clock) {
        if (!erInnenforØkonomisystemetsÅpningstider(clock)) return

        Either.catch {
            val meldekortListe = brukersMeldekortRepo.hentMeldekortSomSkalBehandlesAutomatisk()

            logger.debug { "Fant ${meldekortListe.size} meldekort som skal behandles automatisk" }

            for (meldekort in meldekortListe) {
                if (skalHoppeOverMeldekort(meldekort, clock)) continue

                behandleEttMeldekort(meldekort, clock).onLeft {
                    logger.error(it) { "Ukjent feil ved automatisk behandling av meldekort fra bruker ${meldekort.id} - ${it.message}" }
                }
            }
        }.onLeft {
            logger.error(it) { "Feil ved automatisk behandling av meldekort fra bruker - ${it.message}" }
        }
    }

    /**
     * Logikk for å vente på å behandle brukers meldekort på nytt dersom meldekortet ikke kunne behandles grunnet en ukjent feil.
     * Dette er for at ved en feilsituasjon på et perfekt tidspunkt ikke skal føre til at det plutselig blir mange meldekort
     * som saksbehandlerne må behandle manuelt, som egentlig fint kunne ha blitt behandlet automatisk.
     */
    private fun skalHoppeOverMeldekort(meldekort: BrukersMeldekort, clock: Clock): Boolean {
        if (meldekort.behandletAutomatiskStatus != MeldekortBehandletAutomatiskStatus.UKJENT_FEIL_PRØVER_IGJEN) return false

        val (forrigeForsøk, _, antallForsøk) = meldekort.behandletAutomatiskForsøkshistorikk
        if (forrigeForsøk == null) return false

        val kanPrøvePåNyttNå = forrigeForsøk.shouldRetry(antallForsøk, clock, venteIntervaller, 5.days).first
        return !kanPrøvePåNyttNå
    }

    /**
     * Behandler ett meldekort.
     *
     * - Domenefeil kommer som en left med [MeldekortBehandletAutomatiskStatus] fra [opprettMeldekortBehandling].
     * - Uventede exceptions fanges og håndteres per meldekort, slik at resten kan fortsette.
     */
    private suspend fun behandleEttMeldekort(
        meldekort: BrukersMeldekort,
        clock: Clock,
    ): Either<Throwable, Unit> {
        val sak = try {
            sakRepo.hentForSakId(meldekort.sakId)!!
        } catch (e: Exception) {
            logger.error { "Kunne ikke hente sak for mottatt meldekort med id ${meldekort.id}, sakId ${meldekort.sakId}, ${e.message}" }
            throw e
        }

        return Either.catch {
            opprettMeldekortBehandling(meldekort, sak, clock)
                .onLeft { status ->
                    håndterOpprettBehandlingFeil(status, meldekort, sak)
                }
                .onRight { behandling ->
                    logger.info { "Opprettet automatisk behandling ${behandling.id} for brukers meldekort ${meldekort.id} på sak ${meldekort.sakId}" }
                }
            return@catch
        }.onLeft { throwable ->
            håndterUkjentFeil(throwable, meldekort, sak)
        }
    }

    private suspend fun håndterOpprettBehandlingFeil(
        status: MeldekortBehandletAutomatiskStatus,
        meldekort: BrukersMeldekort,
        sak: Sak,
    ) {
        if (status.loggesSomError) {
            logger.error { "Kunne ikke opprette automatisk behandling for brukers meldekort ${meldekort.id} på sak ${meldekort.sakId} - Feil: $status" }
        } else {
            logger.info { "Kunne ikke opprette automatisk behandling for brukers meldekort ${meldekort.id} på sak ${meldekort.sakId} - Status: $status" }
        }
        opprettOppgaveForAdressebeskyttetBruker(sak.fnr, meldekort.journalpostId)
        brukersMeldekortRepo.oppdaterAutomatiskBehandletStatus(
            meldekortId = meldekort.id,
            status = status,
            behandlesAutomatisk = false,
            metadata = meldekort.behandletAutomatiskForsøkshistorikk,
        )
    }

    /**
     * Håndterer en ukjent feil når vi forsøker å opprette og behandle et meldekort automatisk. Dersom meldekortet kommer
     * fra en bruker med adressebeskyttelse eller meldekortet har blitt forsøkt behandlet automatisk et visst antall ganger,
     * vil ikke forsøkes behandlet automatisk på nytt.
     */
    private suspend fun håndterUkjentFeil(
        throwable: Throwable,
        meldekort: BrukersMeldekort,
        sak: Sak,
    ) {
        logger.error(throwable) { "Ukjent feil ved automatisk behandling av meldekort fra bruker ${meldekort.id} - ${throwable.message}" }
        val oppgaveOpprettet = opprettOppgaveForAdressebeskyttetBruker(sak.fnr, meldekort.journalpostId)
        val (_, _, antallForsøk) = meldekort.behandletAutomatiskForsøkshistorikk
        // Forhindre at man forsøker på nytt og oppretter flere oppgaver for samme sak dersom bruker har adressebeskyttelse eller maks antall forsøk har blitt nådd.
        if (oppgaveOpprettet || antallForsøk >= venteIntervaller.size) {
            brukersMeldekortRepo.oppdaterAutomatiskBehandletStatus(
                meldekortId = meldekort.id,
                status = MeldekortBehandletAutomatiskStatus.UKJENT_FEIL,
                behandlesAutomatisk = false,
                metadata = meldekort.behandletAutomatiskForsøkshistorikk,
            )
        } else {
            brukersMeldekortRepo.oppdaterAutomatiskBehandletStatus(
                meldekortId = meldekort.id,
                status = MeldekortBehandletAutomatiskStatus.UKJENT_FEIL_PRØVER_IGJEN,
                behandlesAutomatisk = true,
                metadata = meldekort.behandletAutomatiskForsøkshistorikk,
            )
        }
    }

    private suspend fun opprettMeldekortBehandling(
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

        val (meldekortBehandling, simulering) = sak.opprettAutomatiskMeldekortBehandling(
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

        val meldekortvedtak = meldekortBehandling.opprettVedtak(
            forrigeUtbetaling = sak.utbetalinger.lastOrNull(),
            clock = clock,
        )

        Either.catch {
            sak.leggTilMeldekortbehandling(meldekortBehandling)
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

        sessionFactory.withTransactionContext { tx ->
            meldekortBehandlingRepo.lagre(meldekortBehandling, simulering, tx)
            meldekortvedtakRepo.lagre(meldekortvedtak, tx)
            brukersMeldekortRepo.oppdaterAutomatiskBehandletStatus(
                meldekortId = meldekortId,
                status = MeldekortBehandletAutomatiskStatus.BEHANDLET,
                behandlesAutomatisk = true,
                metadata = meldekort.behandletAutomatiskForsøkshistorikk.inkrementer(clock),
                tx,
            )
            statistikkMeldekortRepo.lagre(meldekortBehandling.tilStatistikkMeldekortDTO(clock), tx)
        }

        return meldekortBehandling.right()
    }

    private suspend fun opprettOppgaveForAdressebeskyttetBruker(fnr: Fnr, journalpostId: JournalpostId): Boolean {
        val pdlPerson = personKlient.hentEnkelPerson(fnr)
        if (pdlPerson.strengtFortrolig || pdlPerson.strengtFortroligUtland) {
            logger.info { "Person har adressebeskyttelse, oppretter oppgave for meldekort som ikke kan behandles automatisk" }
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
