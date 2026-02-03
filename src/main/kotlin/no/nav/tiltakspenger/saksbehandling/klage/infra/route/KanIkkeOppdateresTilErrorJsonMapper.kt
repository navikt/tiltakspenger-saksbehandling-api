package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling

fun Klagebehandling.KanIkkeOppdateres.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (val u = this) {
        is Klagebehandling.KanIkkeOppdateres.FeilKlagebehandlingsstatus -> {
            Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Feil klagebehandlingsstatus. Forventet: ${u.forventetStatus}, faktisk: ${u.faktiskStatus}",
                    "feil_klagebehandlingsstatus",
                ),
            )
        }

        is Klagebehandling.KanIkkeOppdateres.FeilRammebehandlingssstatus -> {
            Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Feil rammebehandlingsstatus. Forventet: ${u.forventetStatus}, faktisk: ${u.faktiskStatus}",
                    "feil_rammebehandlingsstatus",
                ),
            )
        }

        is Klagebehandling.KanIkkeOppdateres.FeilResultat -> {
            Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Klagebehandling har feil resultat. Forventet: ${u.forventetResultat}, faktisk: ${u.faktiskResultat}",
                    "feil_resultat",
                ),
            )
        }
    }
}
