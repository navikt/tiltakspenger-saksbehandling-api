package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.domene.loggkontekst
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.KanIkkeSendeMeldekortbehandlingTilBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.SendMeldekortbehandlingTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.utbetalingskontroll.oppdaterUtbetalingskontrollForMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.logg
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

/**
 * Har ansvar for å sende et meldekort til beslutter og evt. lagre dager/begrunnelse dersom det sendes med.
 */
class SendMeldekortbehandlingTilBeslutterService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakService: SakService,
    private val simulerService: SimulerService,
    private val erProd: Boolean,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun sendMeldekortTilBeslutter(
        kommando: SendMeldekortbehandlingTilBeslutterKommando,
        clock: Clock,
    ): Either<KanIkkeSendeMeldekortbehandlingTilBeslutter, Pair<Sak, MeldekortbehandlingManuell>> {
        val sak = sakService.hentForSakId(kommando.sakId)

        val meldekortbehandling = sak.hentMeldekortbehandling(kommando.meldekortId)!!

        if (!meldekortbehandling.erFullstendigUtfylt) {
            logger.warn { "Meldeperiodene må være fullstendig utfylt før send til beslutning - ${meldekortbehandling.loggkontekst(kommando.correlationId)}" }
            return KanIkkeSendeMeldekortbehandlingTilBeslutter.MeldeperiodeneErIkkeFullstendigUtfylt.left()
        }

        if (!sak.harSisteMeldeperiodeVersjoner(kommando.meldekortId)) {
            logger.warn { "Meldeperiodene må være siste versjon ved send til beslutning - ${meldekortbehandling.loggkontekst(kommando.correlationId)}" }
            return KanIkkeSendeMeldekortbehandlingTilBeslutter.MeldeperiodeneErIkkeSisteVersjon.left()
        }

        val (sakMedKontroll, behandlingMedKontroll) = sak.oppdaterUtbetalingskontrollForMeldekort(
            meldekortId = kommando.meldekortId,
            simuler = { behandling, beregning ->
                simulerService.simulerMeldekort(
                    behandling = behandling,
                    forrigeUtbetaling = sak.utbetalinger.lastOrNull(),
                    meldeperiodeKjeder = sak.meldeperiodeKjeder,
                    kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort,
                    beregning = beregning,
                    brukersNavkontor = { behandling.navkontor },
                )
            },
            clock = clock,
        ).getOrElse {
            it.logg(logger, "Kontrollsimulering feilet ved send til beslutter - ${meldekortbehandling.loggkontekst(kommando.correlationId)}")
            return KanIkkeSendeMeldekortbehandlingTilBeslutter.SimuleringFeil(it).left()
        }

        return sakMedKontroll.meldekortbehandlinger.sendTilBeslutter(
            kommando = kommando,
            clock = clock,
        ).map { (meldekortbehandlinger, meldekort) ->
            val oppdatertSak = sakMedKontroll.oppdaterMeldekortbehandlinger(meldekortbehandlinger)

            meldekort.validerKanIverksetteUtbetaling().onLeft {
                if (it == KanIkkeIverksetteUtbetaling.SimuleringMangler && !erProd) {
                    return@onLeft
                }

                it.logg(logger) { "Utbetaling på meldekortbehandlingen har et resultat som ikke kan sendes til beslutter - ${meldekort.loggkontekst(kommando.correlationId)}" }

                // Lagrer kontrollen slik at saksbehandler ser hva som avviker, selv om behandlingen blir stående under behandling.
                meldekortbehandlingRepo.oppdater(behandlingMedKontroll)

                return KanIkkeSendeMeldekortbehandlingTilBeslutter.UtbetalingStøttesIkke(it).left()
            }

            meldekortbehandlingRepo.oppdater(meldekort)
            logger.info { "Meldekortbehandling sendt til beslutter - ${meldekort.loggkontekst(kommando.correlationId)}" }
            Pair(oppdatertSak, meldekort)
        }
    }
}
