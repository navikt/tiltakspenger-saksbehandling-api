package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.iverksett.IverksettMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.iverksett.KanIkkeIverksetteMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.tilStatistikkMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.MeldekortvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.logg
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import java.time.Clock

class IverksettMeldekortbehandlingService(
    val sakService: SakService,
    val oppdaterBeregningOgSimuleringMeldekortService: OppdaterBeregningOgSimuleringMeldekortService,
    val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    val utbetalingRepo: UtbetalingRepo,
    val meldeperiodeRepo: MeldeperiodeRepo,
    val sessionFactory: SessionFactory,
    private val meldekortvedtakRepo: MeldekortvedtakRepo,
    private val clock: Clock,
    private val statistikkService: StatistikkService,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun iverksettMeldekort(
        kommando: IverksettMeldekortbehandlingKommando,
    ): Either<KanIkkeIverksetteMeldekortbehandling, Pair<Sak, Meldekortbehandling>> {
        val meldekortId = kommando.meldekortId
        val sakId = kommando.sakId
        val sak = sakService.hentForSakId(sakId)
        val meldekortbehandling: Meldekortbehandling = sak.hentMeldekortbehandling(meldekortId)
            ?: throw IllegalArgumentException("Fant ikke meldekort med id $meldekortId i sak $sakId")

        require(meldekortbehandling is MeldekortbehandlingManuell) {
            "Meldekortet må være behandlet for å iverksettes"
        }
        if (meldekortbehandling.beslutter == null || meldekortbehandling.status != MeldekortbehandlingStatus.UNDER_BESLUTNING) {
            return KanIkkeIverksetteMeldekortbehandling.BehandlingenErIkkeUnderBeslutning.left()
        }
        // Sjekkes før kontrollsimuleringen, siden den krever at det er beslutteren på behandlingen som ber om oppdateringen.
        if (meldekortbehandling.saksbehandler == kommando.beslutter.navIdent) {
            return KanIkkeIverksetteMeldekortbehandling.SaksbehandlerOgBeslutterKanIkkeVæreLik.left()
        }
        if (meldekortbehandling.beslutter != kommando.beslutter.navIdent) {
            return KanIkkeIverksetteMeldekortbehandling.MåVæreBeslutterForMeldekortet.left()
        }

        if (!sak.harSisteMeldeperiodeVersjoner(meldekortId)) {
            logger.warn { "Kan ikke iverksette meldekortbehandling hvor meldeperiodene ikke er siste versjon av meldeperioden i saken. sakId: $sakId, meldekortId: $meldekortId" }
            return KanIkkeIverksetteMeldekortbehandling.MeldeperiodeneErIkkeSisteVersjon.left()
        }

        // Andre meldekortbehandlinger på saken kan ha blitt iverksatt siden behandlingen ble sendt til beslutter, og da er ikke tallene beslutter så på lenger de som ville blitt utbetalt.
        val (sakMedKontroll, behandlingMedKontroll) = oppdaterBeregningOgSimuleringMeldekortService.oppdaterUtbetalingskontroll(
            sak = sak,
            meldekortId = meldekortId,
            saksbehandlerEllerBeslutter = kommando.beslutter,
        ).getOrElse {
            it.logg(logger, "Kontrollsimulering feilet ved iverksettelse. sakId: $sakId, meldekortId: $meldekortId")
            return KanIkkeIverksetteMeldekortbehandling.SimuleringFeil(it).left()
        }

        behandlingMedKontroll.validerKanIverksetteUtbetaling().onLeft {
            it.logg(logger) { "Utbetaling på meldekortbehandlingen har et resultat som ikke kan iverksettes. sakId: $sakId, meldekortId: $meldekortId" }
            // Lagrer kontrollen slik at beslutter ser hva som avviker.
            meldekortbehandlingRepo.oppdater(behandlingMedKontroll)
            return KanIkkeIverksetteMeldekortbehandling.UtbetalingStøttesIkke(it, sakMedKontroll).left()
        }

        return (behandlingMedKontroll as MeldekortbehandlingManuell).iverksettMeldekort(kommando.beslutter, clock, kommando.correlationId).map { (iverksattMeldekortbehandling, klagestatistikk) ->
            val meldekortvedtak = iverksattMeldekortbehandling.opprettVedtak(
                forrigeUtbetaling = sakMedKontroll.utbetalinger.lastOrNull(),
                clock = clock,
            )

            val meldekortstatistikk = Statistikkhendelser(iverksattMeldekortbehandling.tilStatistikkMeldekortDTO(clock))
            val statistikkDTO = statistikkService.generer(meldekortstatistikk + klagestatistikk)
            val oppdatertSak = sakMedKontroll.oppdaterMeldekortbehandling(iverksattMeldekortbehandling)
                .leggTilMeldekortvedtak(meldekortvedtak)
            sessionFactory.withTransactionContext { tx ->
                meldekortbehandlingRepo.oppdater(iverksattMeldekortbehandling, tx)
                meldekortvedtakRepo.lagre(meldekortvedtak, tx)
                statistikkService.lagre(statistikkDTO, tx)
                sakService.markerSkalSendesTilMeldekortApi(sakId = sakId, sessionContext = tx)

                runBlocking {
                    tx.onSuccess {
                        if (meldekortvedtak.meldekortbehandling.harFeilutbetaling()) {
                            logger.info { "Meldekort med feilutbetaling har blitt iverksatt - Meldekort-id $meldekortId - vedtak-id: ${meldekortvedtak.id} - sak-id: $sakId" }
                        }
                    }
                }
            }
            oppdatertSak to iverksattMeldekortbehandling
        }
    }
}
