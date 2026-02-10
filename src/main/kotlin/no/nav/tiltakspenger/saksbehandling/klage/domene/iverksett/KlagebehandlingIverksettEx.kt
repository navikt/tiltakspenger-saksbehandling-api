package no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.VEDTATT

fun Klagebehandling.iverksettOmgjøring(command: IverksettOmgjøringKommando): Either<KanIkkeIverksetteKlagebehandling, Klagebehandling> {
    // left eller throw?...
    require(resultat is Klagebehandlingsresultat.Omgjør && resultat.rammebehandlingId != null) {
        """
            Kan kun iverksette omgjøring dersom resultatet er Omgjør - og at rammebehandlingId er satt.
                - Ved avvisning skal man bruke [iverksettAvvisning]
            Feilen skjedde for sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id, resultat=${resultat?.javaClass?.simpleName}
        """.trimIndent()
    }

    if (!erUnderBehandling) {
        return KanIkkeIverksetteKlagebehandling.MåHaStatusUnderBehandling(status.toString()).left()
    }

    if (kanIverksette == false) {
        return KanIkkeIverksetteKlagebehandling.AndreGrunner(kanIkkeIverksetteGrunner).left()
    }

    return this.copy(
        sistEndret = command.iverksattTidspunkt,
        iverksattTidspunkt = command.iverksattTidspunkt,
        status = VEDTATT,
    ).right()
}

fun Klagebehandling.iverksettAvvisning(
    kommando: IverksettAvvisningKommando,
): Either<KanIkkeIverksetteKlagebehandling, Klagebehandling> {
    // left eller throw?...
    require(resultat is Klagebehandlingsresultat.Avvist) {
        """
            Kan kun iverksette avvisning dersom resultatet er Avvist.
                - Ved omgjøring skal man bruke [iverksettOmgjøring]
            Feilen skjedde for sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id, resultat=${resultat?.javaClass?.simpleName}
        """.trimIndent()
    }

    if (!erUnderBehandling) {
        return KanIkkeIverksetteKlagebehandling.MåHaStatusUnderBehandling(status.toString()).left()
    }

    if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
        return KanIkkeIverksetteKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = this.saksbehandler!!,
            faktiskSaksbehandler = kommando.saksbehandler.navIdent,
        ).left()
    }

    if (kanIverksette == false) {
        return KanIkkeIverksetteKlagebehandling.AndreGrunner(kanIkkeIverksetteGrunner).left()
    }

    return this.copy(
        sistEndret = kommando.iverksattTidspunkt,
        iverksattTidspunkt = kommando.iverksattTidspunkt,
        status = VEDTATT,
    ).right()
}
