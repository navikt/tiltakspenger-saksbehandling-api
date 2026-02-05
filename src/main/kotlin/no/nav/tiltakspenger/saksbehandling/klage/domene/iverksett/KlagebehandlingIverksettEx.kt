package no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.VEDTATT

/**
 * Ved medhold/omgjøring må rammebehandlingId være satt, rammebehandlingen må være i riktig tilstand og skal kun kalles via [no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettRammebehandlingService]
 */
fun Klagebehandling.iverksett(
    kommando: IverksettKlagebehandlingKommando,
): Either<KanIkkeIverksetteKlagebehandling, Klagebehandling> {
    if (!erUnderBehandling) {
        return KanIkkeIverksetteKlagebehandling.MåHaStatusUnderBehandling(status.toString()).left()
    }
    if (kommando.saksbehandler != null && !erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
        return KanIkkeIverksetteKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = this.saksbehandler!!,
            faktiskSaksbehandler = kommando.saksbehandler.navIdent,
        ).left()
    }
    when (resultat) {
        is Klagebehandlingsresultat.Avvist, is Klagebehandlingsresultat.Omgjør -> {
            when (resultat) {
                is Klagebehandlingsresultat.Avvist -> {
                    if (kommando.iverksettFraRammebehandling) {
                        return KanIkkeIverksetteKlagebehandling.FeilInngang(
                            forventetInngang = "Iverksett klagebehandling utenom rammebehandling",
                            faktiskInngang = "Iverksett klagebehandling fra rammebehandling",
                        ).left()
                    }
                }

                is Klagebehandlingsresultat.Omgjør -> {
                    if (!kommando.iverksettFraRammebehandling) {
                        return KanIkkeIverksetteKlagebehandling.FeilInngang(
                            forventetInngang = "Iverksett klagebehandling fra rammebehandling",
                            faktiskInngang = "Iverksett klagebehandling utenom rammebehandling",
                        ).left()
                    }
                }
            }
            if (kanIverksette == false) {
                return KanIkkeIverksetteKlagebehandling.AndreGrunner(kanIkkeIverksetteGrunner).left()
            }
        }

        null -> return KanIkkeIverksetteKlagebehandling.FeilResultat(
            forventetResultat = Klagebehandlingsresultat.Avvist::class.simpleName!!,
            faktiskResultat = null,
        ).left()
    }

    return this.copy(
        sistEndret = kommando.iverksattTidspunkt,
        iverksattTidspunkt = kommando.iverksattTidspunkt,
        status = VEDTATT,
    ).right()
}
