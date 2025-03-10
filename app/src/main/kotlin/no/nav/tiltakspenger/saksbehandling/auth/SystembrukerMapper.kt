package no.nav.tiltakspenger.saksbehandling.auth

import mu.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.felles.Systembrukerrolle
import no.nav.tiltakspenger.saksbehandling.felles.Systembrukerroller

private val logger = KotlinLogging.logger { }

fun systembrukerMapper(
    klientId: String,
    klientnavn: String,
    roller: Set<String>,
): Systembruker {
    return Systembruker(
        roller = Systembrukerroller(
            roller.mapNotNull { rolle ->
                when (rolle) {
                    "hente_data" -> Systembrukerrolle.HENTE_DATA
                    "lage_hendelser" -> Systembrukerrolle.LAGE_HENDELSER
                    "access_as_application" -> null
                    else -> null.also {
                        logger.debug { "Filtrerer bort ukjent systembrukerrolle: $rolle" }
                    }
                }
            }.toSet(),
        ),
        klientId = klientId,
        klientnavn = klientnavn,
    )
}
