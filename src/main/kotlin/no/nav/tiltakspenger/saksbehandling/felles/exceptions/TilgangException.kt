package no.nav.tiltakspenger.saksbehandling.felles.exceptions

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson

/**
 * https://etterlevelse.ansatt.nav.no/dokumentasjon/62267282-f8c2-4f26-8bed-393ccfa094e3/INFOSIKKERHET/RELEVANTE_KRAV/krav/255/1
 *
 * Suksesskriterium 2 av 8 - Opplyse hvorfor saksbehandler ikke kunne søke opp brukeren
 */
class TilgangException(
    private val tilgangsnektårsak: Tilgangsnektårsak,
    message: String,
) : RuntimeException(message) {
    constructor(
        tilgangsnektårsak: Tilgangsnektårsak,
        saksbehandler: Saksbehandler,
        krevRolle: Saksbehandlerrolle,
    ) : this(
        tilgangsnektårsak,
        "Saksbehandler ${saksbehandler.navIdent} mangler rollen $krevRolle. Saksbehandlers roller: ${saksbehandler.roller}",
    )

    constructor(
        tilgangsnektårsak: Tilgangsnektårsak,
        saksbehandler: Saksbehandler,
        vararg krevEnAvRollene: Saksbehandlerrolle,
    ) : this(tilgangsnektårsak, saksbehandler, krevEnAvRollene.toList())

    constructor(
        tilgangsnektårsak: Tilgangsnektårsak,
        saksbehandler: Saksbehandler,
        krevEnAvRollene: Saksbehandlerroller,
    ) : this(tilgangsnektårsak, saksbehandler, krevEnAvRollene.toList())

    constructor(
        tilgangsnektårsak: Tilgangsnektårsak,
        saksbehandler: Saksbehandler,
        krevEnAvRollene: List<Saksbehandlerrolle>,
    ) : this(
        tilgangsnektårsak,
        "Saksbehandler ${saksbehandler.navIdent} mangler en av rollene $krevEnAvRollene. Saksbehandlers roller: ${saksbehandler.roller}",
    )

    // Denne er helt sikkert på feil sted :)
    fun toErrorJson(): ErrorJson = when (this.tilgangsnektårsak) {
        Tilgangsnektårsak.KODE_6 -> ErrorJson(
            "Du har ikke tilgang til brukere med strengt fortrolig adresse.",
            "tilgang_nektet_kode_6",
        )

        Tilgangsnektårsak.KODE_6_UTLAND -> ErrorJson(
            "Du har ikke tilgang til brukere med strengt fortrolig adresse i utlandet.",
            "tilgang_nektet_kode_6_utland",
        )

        Tilgangsnektårsak.KODE_7 -> ErrorJson(
            "Du har ikke tilgang til brukere med fortrolig adresse",
            "tilgang_nektet_kode_7",
        )

        Tilgangsnektårsak.SKJERMET -> ErrorJson(
            "Du har ikke tilgang til skjermet bruker",
            "tilgang_nektet_skjermet",
        )

        Tilgangsnektårsak.HABILITET -> ErrorJson(
            "Du har ikke tilgang til data om deg selv eller dine nærstående.",
            "tilgang_nektet_habilitet",
        )

        Tilgangsnektårsak.VERGE -> ErrorJson(
            "Du har ikke tilgang fordi du er registrert som brukerens verge.",
            "tilgang_nektet_verge",
        )

        Tilgangsnektårsak.KREV_ROLLEN -> ErrorJson(
            this.message ?: "Du har ikke tilgang til denne handlingen fordi du krever nødvendig rolle.",
            "tilgang_nektet_krev_rolle",
        )

        Tilgangsnektårsak.MANGLER_ROLLE -> ErrorJson(
            this.message ?: "Du har ikke tilgang til denne ressursen fordi du mangler nødvendig rolle.",
            "tilgang_nektet_mangler_rolle",
        )
    }
}

enum class Tilgangsnektårsak {
    KODE_6,
    KODE_6_UTLAND,
    KODE_7,
    SKJERMET,
    HABILITET,
    VERGE,

    KREV_ROLLEN,
    MANGLER_ROLLE,
}
