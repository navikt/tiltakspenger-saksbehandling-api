package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.KanIkkeSendeMeldekortbehandlingTilBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.SendMeldekortbehandlingTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import java.time.Clock

/**
 * Har ansvar for å sende et meldekort til beslutter og evt. lagre dager/begrunnelse dersom det sendes med.
 */
class SendMeldekortbehandlingTilBeslutterService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakService: SakService,
) {
    private val logger = KotlinLogging.logger {}

    fun sendMeldekortTilBeslutter(
        kommando: SendMeldekortbehandlingTilBeslutterKommando,
        clock: Clock,
    ): Either<KanIkkeSendeMeldekortbehandlingTilBeslutter, Pair<Sak, MeldekortbehandlingManuell>> {
        val sak = sakService.hentForSakId(kommando.sakId)

        val meldekortbehandling = sak.hentMeldekortbehandling(kommando.meldekortId)!!

        if (!meldekortbehandling.erFullstendigUtfylt) {
            logger.warn { "Meldeperiodene må være fullstendig utfylt før send til beslutning - sakId: ${sak.id}, meldekortId: ${kommando.meldekortId}" }
            return KanIkkeSendeMeldekortbehandlingTilBeslutter.MeldeperiodeneErIkkeFullstendigUtfylt.left()
        }

        if (!sak.harSisteMeldeperiodeVersjoner(kommando.meldekortId)) {
            logger.warn { "Meldeperiodene må være siste versjon ved send til beslutning - sakId: ${sak.id}, meldekortId: ${kommando.meldekortId}" }
            return KanIkkeSendeMeldekortbehandlingTilBeslutter.MeldeperiodeneErIkkeSisteVersjon.left()
        }

        return sak.meldekortbehandlinger.sendTilBeslutter(
            kommando = kommando,
            clock = clock,
        ).map { (meldekortbehandlinger, meldekort) ->
            val oppdatertSak = sak.oppdaterMeldekortbehandlinger(meldekortbehandlinger)

            meldekort.validerKanIverksetteUtbetaling().onLeft {
                return KanIkkeSendeMeldekortbehandlingTilBeslutter.UtbetalingStøttesIkke(it).left()
            }

            meldekortbehandlingRepo.oppdater(meldekort)
            logger.info { "Meldekort med id ${meldekort.id} sendt til beslutter. Saksbehandler: ${kommando.saksbehandler.navIdent}" }
            Pair(oppdatertSak, meldekort)
        }
    }
}
