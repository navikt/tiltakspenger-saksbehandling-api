package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingAvbrutt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import java.time.LocalDateTime

/** Saksbehandler avbryter en meldekortbehandling */
fun Meldekortbehandling.avbryt(
    avbruttAv: Saksbehandler,
    avbruttBegrunnelse: NonBlankString,
    tidspunkt: LocalDateTime,
): Either<KanIkkeAvbryteMeldekortbehandling, Meldekortbehandling> {
    if (this.status != MeldekortbehandlingStatus.UNDER_BEHANDLING) {
        return KanIkkeAvbryteMeldekortbehandling.MåVæreUnderBehandling.left()
    }
    if (this.saksbehandler != avbruttAv.navIdent) {
        return KanIkkeAvbryteMeldekortbehandling.MåVæreSaksbehandlerForMeldekortet.left()
    }

    return this.avbryt(
        saksbehandlerIdent = avbruttAv.navIdent,
        avbruttBegrunnelse = avbruttBegrunnelse,
        tidspunkt = tidspunkt,
    ).right()
}

/** Avbryter en meldekortbehandling fordi det ikke lengre er rett til tiltakspenger i perioden. Dette er en automatisk handling. */
fun Meldekortbehandling.avbrytIkkeRettTilTiltakspenger(
    tidspunkt: LocalDateTime,
): MeldekortbehandlingAvbrutt {
    return avbryt(
        saksbehandlerIdent = AUTOMATISK_SAKSBEHANDLER_ID,
        avbruttBegrunnelse = "Ikke rett til tiltakspenger".toNonBlankString(),
        tidspunkt = tidspunkt,
    )
}

private fun Meldekortbehandling.avbryt(
    saksbehandlerIdent: String,
    avbruttBegrunnelse: NonBlankString,
    tidspunkt: LocalDateTime,
): MeldekortbehandlingAvbrutt {
    when (status) {
        MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING,
        MeldekortbehandlingStatus.UNDER_BEHANDLING,
        MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING,
        MeldekortbehandlingStatus.UNDER_BESLUTNING,
        -> Unit

        MeldekortbehandlingStatus.GODKJENT,
        MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET,
        MeldekortbehandlingStatus.AVBRUTT,
        -> throw IllegalStateException("Kan ikke avbryte en meldekortbehandling med status $status")
    }

    return MeldekortbehandlingAvbrutt(
        id = id,
        sakId = sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        opprettet = opprettet,
        simulering = null,
        saksbehandler = saksbehandler,
        navkontor = navkontor,
        type = type,
        begrunnelse = begrunnelse,
        attesteringer = attesteringer,
        avbrutt = Avbrutt(
            tidspunkt = tidspunkt,
            saksbehandler = saksbehandlerIdent,
            begrunnelse = avbruttBegrunnelse,
        ),
        sistEndret = tidspunkt,
        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
        meldeperioder = meldeperioder,
        skalSendeVedtaksbrev = skalSendeVedtaksbrev,
    )
}
