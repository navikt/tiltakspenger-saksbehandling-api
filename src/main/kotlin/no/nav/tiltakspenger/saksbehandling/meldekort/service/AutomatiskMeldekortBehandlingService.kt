package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettAutomatiskBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.opprettUtbetalingsvedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.tilStatistikk
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
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
) {
    val logger = KotlinLogging.logger { }

    suspend fun behandleBrukersMeldekort() {
        Either.catch {
            val meldekortListe = brukersMeldekortRepo.hentMeldekortSomSkalBehandlesAutomatisk()

            logger.debug { "Fant ${meldekortListe.size} meldekort som skal behandles automatisk" }

            meldekortListe.forEach { meldekort ->
                Either.catch {
                    opprettMeldekortBehandling(meldekort)
                }.onLeft {
                    logger.error(it) { "Feil ved automatisk behandling av meldekort fra bruker ${meldekort.id} - ${it.message}" }
                }
            }
        }.onLeft {
            logger.error(it) { "Feil ved automatisk behandling av meldekort fra bruker - ${it.message}" }
        }
    }

    private suspend fun opprettMeldekortBehandling(meldekort: BrukersMeldekort): MeldekortBehandletAutomatisk {
        val meldekortId = meldekort.id
        val sakId = meldekort.sakId

        val sak = sakRepo.hentForSakId(sakId)!!

        val navkontor = Either.catch {
            navkontorService.hentOppfolgingsenhet(sak.fnr)
        }.getOrElse {
            with("Kunne ikke hente navkontor for sak $sakId") {
                logger.error { this }
                sikkerlogg.error(it) { "$this - fnr ${sak.fnr.verdi}" }
            }
            throw it
        }

        val meldekortBehandling = Either.catch {
            sak.opprettAutomatiskBehandling(
                meldekort = meldekort,
                navkontor = navkontor,
                clock = clock,
            )
        }.getOrElse {
            logger.error(it) { "Kunne ikke opprette automatisk behandling for brukers meldekort $meldekortId" }
            throw it
        }

        val utbetalingsvedtak = meldekortBehandling.opprettUtbetalingsvedtak(
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            forrigeUtbetalingsvedtak = sak.utbetalinger.lastOrNull(),
            clock = clock,
        )

        Either.catch {
            sak.leggTilMeldekortbehandling(meldekortBehandling)
                .leggTilUtbetalingsvedtak(utbetalingsvedtak)
        }.onLeft {
            logger.error(it) { "Automatisk behandling for brukers meldekort $meldekortId kunne ikke legges til sak $sakId" }
            throw it
        }
        val utbetalingsstatistikk = utbetalingsvedtak.tilStatistikk()

        sessionFactory.withTransactionContext { tx ->
            meldekortBehandlingRepo.lagre(meldekortBehandling, tx)
            brukersMeldekortRepo.markerMeldekortSomBehandlet(
                meldekortId = meldekortId,
                behandletTidspunkt = nå(clock),
                tx,
            )
            utbetalingsvedtakRepo.lagre(utbetalingsvedtak, tx)
            statistikkStønadRepo.lagre(utbetalingsstatistikk, tx)
        }

        logger.info { "Opprettet automatisk behandling ${meldekortBehandling.id} for brukers meldekort $meldekortId på sak $sakId" }

        return meldekortBehandling
    }
}
