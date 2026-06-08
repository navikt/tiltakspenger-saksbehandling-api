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

        is KanIkkeOppdatereKlagebehandling.FeilTilknyttetBehandlingsstatus -> {
            Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Feil tilknyttet behandlingsstatus. Forventet: ${u.forventetStatus}, faktisk: ${u.faktiskStatus}",
                    "feil_tilknyttet_behandlingsstatus",
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

        is KanIkkeOppdatereKlagebehandling.KlageErKnyttetTilBehandling -> {
            Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Klagebehandling er knyttet til behandling med id ${u.behandlingId}, og kan derfor ikke oppdateres",
                    "klage_er_knyttet_til_behandling",
                ),
            )
        }
    }
}
