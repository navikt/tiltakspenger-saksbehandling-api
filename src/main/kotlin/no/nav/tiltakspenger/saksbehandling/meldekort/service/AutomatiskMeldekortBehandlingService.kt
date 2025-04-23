package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.logging.sikkerlogg
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
                    opprettMeldekortBehandling(meldekort).onLeft {
                        brukersMeldekortRepo.oppdaterAutomatiskBehandletStatus(
                            meldekort.id,
                            it.tilMeldekortBehandletAutomatiskStatus(),
                            false,
                        )
                    }.onRight {
                        logger.info { "Opprettet automatisk behandling ${it.id} for brukers meldekort $${meldekort.id} på sak ${meldekort.sakId}" }
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
    ): Either<AutomatiskMeldekortbehandlingFeilet, MeldekortBehandletAutomatisk> {
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
            return AutomatiskMeldekortbehandlingFeilet.HenteNavkontorFeilet.left()
        }

        val meldekortBehandling = sak.opprettAutomatiskMeldekortBehandling(
            meldekort = meldekort,
            navkontor = navkontor,
            clock = clock,
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
            return AutomatiskMeldekortbehandlingFeilet.BehandlingFeiletPåSak.left()
        }

        Either.catch {
            sak.leggTilUtbetalingsvedtak(utbetalingsvedtak)
        }.onLeft {
            logger.error(it) { "Utbetalingsvedtak for automatisk behandling av brukers meldekort $meldekortId kunne ikke legges til sak $sakId" }
            return AutomatiskMeldekortbehandlingFeilet.UtbetalingFeiletPåSak.left()
        }

        val utbetalingsstatistikk = utbetalingsvedtak.tilStatistikk()

        sessionFactory.withTransactionContext { tx ->
            meldekortBehandlingRepo.lagre(meldekortBehandling, tx)
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

sealed interface AutomatiskMeldekortbehandlingFeilet {
    data object HenteNavkontorFeilet : AutomatiskMeldekortbehandlingFeilet
    data object BehandlingFeiletPåSak : AutomatiskMeldekortbehandlingFeilet
    data object UtbetalingFeiletPåSak : AutomatiskMeldekortbehandlingFeilet
    data object SkalIkkeBehandlesAutomatisk : AutomatiskMeldekortbehandlingFeilet
    data object AlleredeBehandlet : AutomatiskMeldekortbehandlingFeilet
    data object UtdatertMeldeperiode : AutomatiskMeldekortbehandlingFeilet

    fun tilMeldekortBehandletAutomatiskStatus(): BrukersMeldekortBehandletAutomatiskStatus = when (this) {
        AlleredeBehandlet -> BrukersMeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET
        BehandlingFeiletPåSak -> BrukersMeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK
        HenteNavkontorFeilet -> BrukersMeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET
        SkalIkkeBehandlesAutomatisk -> BrukersMeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK
        UtbetalingFeiletPåSak -> BrukersMeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK
        UtdatertMeldeperiode -> BrukersMeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE
    }
}
