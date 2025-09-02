package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettAutomatiskMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.tilStatistikkMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortVedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

class AutomatiskMeldekortBehandlingService(
    private val brukersMeldekortRepo: BrukersMeldekortRepo,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val sakRepo: SakRepo,
    private val meldekortVedtakRepo: MeldekortVedtakRepo,
    private val navkontorService: NavkontorService,
    private val clock: Clock,
    private val sessionFactory: SessionFactory,
    private val simulerService: SimulerService,
    private val personKlient: PersonKlient,
    private val oppgaveKlient: OppgaveKlient,
    private val statistikkMeldekortRepo: StatistikkMeldekortRepo,
) {
    val logger = KotlinLogging.logger { }

    suspend fun behandleBrukersMeldekort() {
        Either.catch {
            val meldekortListe = brukersMeldekortRepo.hentMeldekortSomSkalBehandlesAutomatisk()

            logger.debug { "Fant ${meldekortListe.size} meldekort som skal behandles automatisk" }

            meldekortListe.forEach { meldekort ->
                val sak = sakRepo.hentForSakId(meldekort.sakId)!!
                Either.catch {
                    opprettMeldekortBehandling(meldekort, sak).onLeft {
                        logger.error { "Kunne ikke opprette automatisk behandling for brukers meldekort ${meldekort.id} på sak ${meldekort.sakId} - Feil: $it" }
                        opprettOppgaveForAdressebeskyttetBruker(sak.fnr, meldekort.journalpostId)
                        brukersMeldekortRepo.oppdaterAutomatiskBehandletStatus(
                            meldekortId = meldekort.id,
                            status = it,
                            behandlesAutomatisk = false,
                        )
                    }.onRight {
                        logger.info { "Opprettet automatisk behandling ${it.id} for brukers meldekort ${meldekort.id} på sak ${meldekort.sakId}" }
                    }
                }.onLeft {
                    logger.error(it) { "Ukjent feil ved automatisk behandling av meldekort fra bruker ${meldekort.id} - ${it.message}" }
                    opprettOppgaveForAdressebeskyttetBruker(sak.fnr, meldekort.journalpostId)
                    brukersMeldekortRepo.oppdaterAutomatiskBehandletStatus(
                        meldekort.id,
                        BrukersMeldekortBehandletAutomatiskStatus.UKJENT_FEIL,
                        false,
                    )
                }
            }
        }.onLeft {
            logger.error(it) { "Feil ved automatisk behandling av meldekort fra bruker - ${it.message}" }
        }
    }

    private suspend fun opprettMeldekortBehandling(
        meldekort: BrukersMeldekort,
        sak: Sak,
    ): Either<BrukersMeldekortBehandletAutomatiskStatus, MeldekortBehandletAutomatisk> {
        val meldekortId = meldekort.id

        if (sak.revurderinger.harÅpenRevurdering()) {
            return BrukersMeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING.left()
        }

        val navkontor = Either.catch {
            navkontorService.hentOppfolgingsenhet(sak.fnr)
        }.getOrElse {
            with("Kunne ikke hente navkontor for sak ${sak.id}") {
                logger.error(it) { this }
                Sikkerlogg.error(it) { "$this - fnr ${sak.fnr.verdi}" }
            }
            return BrukersMeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET.left()
        }

        val (meldekortBehandling, simulering) = sak.opprettAutomatiskMeldekortBehandling(
            brukersMeldekort = meldekort,
            navkontor = navkontor,
            clock = clock,
            simuler = { behandling -> simulerService.simulerMeldekort(behandling, sak.utbetalinger.lastOrNull(), sak.meldeperiodeKjeder) { navkontor } },
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
            return BrukersMeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK.left()
        }

        Either.catch {
            sak.leggTilMeldekortVedtak(meldekortvedtak)
        }.onLeft {
            logger.error(it) { "Vedtak for automatisk behandling av brukers meldekort $meldekortId kunne ikke legges til sak ${sak.id}" }
            return BrukersMeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK.left()
        }

        sessionFactory.withTransactionContext { tx ->
            meldekortBehandlingRepo.lagre(meldekortBehandling, simulering, tx)
            meldekortVedtakRepo.lagre(meldekortvedtak, tx)
            brukersMeldekortRepo.oppdaterAutomatiskBehandletStatus(
                meldekortId = meldekortId,
                status = BrukersMeldekortBehandletAutomatiskStatus.BEHANDLET,
                behandlesAutomatisk = true,
                tx,
            )
            statistikkMeldekortRepo.lagre(meldekortBehandling.tilStatistikkMeldekortDTO(), tx)
        }

        return meldekortBehandling.right()
    }

    private suspend fun opprettOppgaveForAdressebeskyttetBruker(fnr: Fnr, journalpostId: JournalpostId) {
        val pdlPerson = personKlient.hentEnkelPerson(fnr)
        if (pdlPerson.strengtFortrolig || pdlPerson.strengtFortroligUtland) {
            logger.info { "Person har adressebeskyttelse, oppretter oppgave for meldekort som ikke kan behandles automatisk" }
            oppgaveKlient.opprettOppgave(
                fnr = fnr,
                journalpostId = journalpostId,
                oppgavebehov = Oppgavebehov.NYTT_MELDEKORT,
            )
        }
    }
}
