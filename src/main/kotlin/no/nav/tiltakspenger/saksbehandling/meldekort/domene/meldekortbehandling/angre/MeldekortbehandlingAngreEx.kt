package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.angre

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import java.time.Clock

fun Meldekortbehandling.angreMeldekortbehandling(
    saksbehandler: Saksbehandler,
    clock: Clock,
): Either<KanIkkeAngreMeldekortbehandling, Meldekortbehandling> {
    kanAngreMeldekortbehandling(saksbehandler).onLeft { return it.left() }

    val nå = nå(clock)

    return when (this.status) {
        MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> {
            require(this is MeldekortbehandlingManuell) {
                "Forventet MeldekortbehandlingManuell for status KLAR_TIL_BESLUTNING, var ${this::class.simpleName}"
            }

            MeldekortUnderBehandling(
                id = id,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                opprettet = opprettet,
                navkontor = navkontor,
                saksbehandler = this.saksbehandler,
                beslutter = null,
                begrunnelse = begrunnelse,
                attesteringer = attesteringer,
                sendtTilBeslutning = null,
                simulering = simulering,
                utbetalingskontroll = utbetalingskontroll,
                status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
                sistEndret = nå,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                skalSendeVedtaksbrev = skalSendeVedtaksbrev,
                meldeperioder = meldeperioder,
                ventestatus = ventestatus,
                klagebehandling = klagebehandling,
            ).right()
        }

        MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING,
        MeldekortbehandlingStatus.UNDER_BEHANDLING,
        MeldekortbehandlingStatus.UNDER_BESLUTNING,
        MeldekortbehandlingStatus.GODKJENT,
        MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET,
        MeldekortbehandlingStatus.AVBRUTT,
        -> KanIkkeAngreMeldekortbehandling.MeldekortbehandlingenErIEnTilstandSomIkkeTillaterÅAngre(this.status)
            .left()
    }
}

fun Meldekortbehandling.kanAngreMeldekortbehandling(
    saksbehandler: Saksbehandler,
): Either<KanIkkeAngreMeldekortbehandling, Unit> {
    return when (this.status) {
        MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> {
            if (this.beslutter != null) {
                KanIkkeAngreMeldekortbehandling.KanIkkeVæreTattAvEnBeslutter.left() // TODO - vet ikke om denne er nødvendig - UNDER_BESLUTNING
            } else if (this.saksbehandler != saksbehandler.navIdent) {
                KanIkkeAngreMeldekortbehandling.MåVæreSammeSaksbehandlerForÅAngreMeldekortbehandlingen.left()
            } else {
                Unit.right()
            }
        }

        MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING,
        MeldekortbehandlingStatus.UNDER_BEHANDLING,
        MeldekortbehandlingStatus.UNDER_BESLUTNING,
        MeldekortbehandlingStatus.GODKJENT,
        MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET,
        MeldekortbehandlingStatus.AVBRUTT,
        -> KanIkkeAngreMeldekortbehandling.MeldekortbehandlingenErIEnTilstandSomIkkeTillaterÅAngre(this.status)
            .left()
    }
}
