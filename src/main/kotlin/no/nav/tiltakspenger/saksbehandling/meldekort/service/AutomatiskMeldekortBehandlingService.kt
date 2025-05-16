package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettAutomatiskMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.opprettUtbetalingsvedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.tilStatistikk
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

class AutomatiskMeldekortBehandlingService(
    private val brukersMeldekortRepo: BrukersMeldekortRepo,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val sakRepo: SakRepo,
    private val utbetalingsvedtakRepo: UtbetalingsvedtakRepo,
    private val statistikkStønadRepo: StatistikkStønadRepo,
    private val navkontorService: NavkontorService,
    private val clock: Clock,
    private val sessionFactory: SessionFactory,
    private val simulerService: SimulerService,
) {
    val logger = KotlinLogging.logger { }

    suspend fun behandleBrukersMeldekort() {
        Either.catch {
            val meldekortListe = brukersMeldekortRepo.hentMeldekortSomSkalBehandlesAutomatisk()

            logger.debug { "Fant ${meldekortListe.size} meldekort som skal behandles automatisk" }

            meldekortListe.forEach { meldekort ->
                Either.catch {
                    opprettMeldekortBehandling(meldekort).onLeft {
                        logger.error { "Kunne ikke opprette automatisk behandling for brukers meldekort ${meldekort.id} på sak ${meldekort.sakId} - Feil: $it" }
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
    ): Either<BrukersMeldekortBehandletAutomatiskStatus, MeldekortBehandletAutomatisk> {
        val meldekortId = meldekort.id
        val sakId = meldekort.sakId

        val sak = sakRepo.hentForSakId(sakId)!!

        if (sak.revurderinger.harÅpenRevurdering()) {
            return BrukersMeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING.left()
        }

        val navkontor = Either.catch {
            navkontorService.hentOppfolgingsenhet(sak.fnr)
        }.getOrElse {
            with("Kunne ikke hente navkontor for sak $sakId") {
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

        val utbetalingsvedtak = meldekortBehandling.opprettUtbetalingsvedtak(
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            forrigeUtbetalingsvedtak = sak.utbetalinger.lastOrNull(),
            clock = clock,
        )

        Either.catch {
            sak.leggTilMeldekortbehandling(meldekortBehandling)
        }.onLeft {
            logger.error(it) { "Automatisk behandling av brukers meldekort $meldekortId kunne ikke legges til sak $sakId" }
            return BrukersMeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK.left()
        }

        Either.catch {
            sak.leggTilUtbetalingsvedtak(utbetalingsvedtak)
        }.onLeft {
            logger.error(it) { "Utbetalingsvedtak for automatisk behandling av brukers meldekort $meldekortId kunne ikke legges til sak $sakId" }
            return BrukersMeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK.left()
        }

        val utbetalingsstatistikk = utbetalingsvedtak.tilStatistikk()

        sessionFactory.withTransactionContext { tx ->
            meldekortBehandlingRepo.lagre(meldekortBehandling, simulering, tx)
            utbetalingsvedtakRepo.lagre(utbetalingsvedtak, tx)
            statistikkStønadRepo.lagre(utbetalingsstatistikk, tx)
            brukersMeldekortRepo.oppdaterAutomatiskBehandletStatus(
                meldekortId = meldekortId,
                status = BrukersMeldekortBehandletAutomatiskStatus.BEHANDLET,
                behandlesAutomatisk = true,
                tx,
            )
        }

        return meldekortBehandling.right()
    }
}
