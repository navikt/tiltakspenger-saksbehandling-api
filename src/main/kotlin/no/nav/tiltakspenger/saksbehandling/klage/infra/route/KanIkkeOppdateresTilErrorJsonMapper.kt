package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling

fun KanIkkeOppdatereKlagebehandling.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (val u = this) {
        is KanIkkeOppdatereKlagebehandling.FeilKlagebehandlingsstatus -> {
            Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Feil klagebehandlingsstatus. Forventet: ${u.forventetStatus}, faktisk: ${u.faktiskStatus}",
                    "feil_klagebehandlingsstatus",
                ),
            )
        }

        is KanIkkeOppdatereKlagebehandling.FeilRammebehandlingssstatus -> {
            Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Feil rammebehandlingsstatus. Forventet: ${u.forventetStatus}, faktisk: ${u.faktiskStatus}",
                    "feil_rammebehandlingsstatus",
                ),
            )
        }

        is KanIkkeOppdatereKlagebehandling.FeilResultat -> {
            Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Klagebehandling har feil resultat. Forventet: ${u.forventetResultat}, faktisk: ${u.faktiskResultat}",
                    "feil_resultat",
                ),
            )
        }

        is KanIkkeOppdatereKlagebehandling.KlageErKnyttetTilRammebehandling -> {
            Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Klagebehandling er knyttet til rammebehandling med id ${u.rammebehandlingId}, og kan derfor ikke oppdateres",
                    "klage_er_knyttet_til_rammebehandling",
                ),
            )
        }
    }
}
