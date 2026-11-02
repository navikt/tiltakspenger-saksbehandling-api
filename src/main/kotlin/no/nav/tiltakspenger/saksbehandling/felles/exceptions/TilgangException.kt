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
    private val brukermelding: String? = null,
) : RuntimeException(message) {
    constructor(
        tilgangsnektårsak: Tilgangsnektårsak,
        saksbehandler: Saksbehandler,
        krevRolle: Saksbehandlerrolle,
    ) : this(
        tilgangsnektårsak = tilgangsnektårsak,
        message = "Saksbehandler ${saksbehandler.navIdent} mangler rollen $krevRolle. Har rollene: ${saksbehandler.roller.tilLoggtekst()}.",
        brukermelding = "Du mangler rollen ${krevRolle.tilVisningsnavn()}, som kreves for denne handlingen.",
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
        tilgangsnektårsak = tilgangsnektårsak,
        message = "Saksbehandler ${saksbehandler.navIdent} mangler en av rollene $krevEnAvRollene. Har rollene: ${saksbehandler.roller.tilLoggtekst()}.",
        brukermelding = "Du mangler en av rollene ${krevEnAvRollene.tilVisningsliste()}, som kreves for denne handlingen.",
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

        Tilgangsnektårsak.ANNET -> ErrorJson(
            "Du har ikke tilgang til denne brukeren.",
            "tilgang_nektet_annet",
        )

        Tilgangsnektårsak.KREV_ROLLEN -> ErrorJson(
            brukermelding ?: "Du mangler en rolle som kreves for denne handlingen.",
            "tilgang_nektet_krev_rolle",
        )

        Tilgangsnektårsak.MANGLER_ROLLE -> ErrorJson(
            this.message ?: "Du har ikke tilgang til denne ressursen fordi du mangler nødvendig rolle.",
            "tilgang_nektet_mangler_rolle",
        )
    }
}

private fun Saksbehandlerrolle.tilVisningsnavn(): String = name.lowercase().replace("_", " ")

/** Gir «a, b eller c», slik at lista leses likt uansett hvor mange roller den har. */
private fun List<Saksbehandlerrolle>.tilVisningsliste(): String {
    val navn = map { it.tilVisningsnavn() }
    return if (navn.size <= 1) navn.joinToString() else "${navn.dropLast(1).joinToString()} eller ${navn.last()}"
}

private fun Saksbehandlerroller.tilLoggtekst(): String = toList().joinToString(prefix = "[", postfix = "]")

enum class Tilgangsnektårsak {
    KODE_6,
    KODE_6_UTLAND,
    KODE_7,
    SKJERMET,
    HABILITET,
    VERGE,

    /** Tilgangsmaskinen avviste på en regel vi ikke kategoriserer, eller på en avvisningskode vi ikke kjenner. */
    ANNET,

    KREV_ROLLEN,
    MANGLER_ROLLE,
}
