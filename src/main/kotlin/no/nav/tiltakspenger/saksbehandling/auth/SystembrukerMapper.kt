package no.nav.tiltakspenger.saksbehandling.auth

import io.github.oshai.kotlinlogging.KotlinLogging
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
                    "hent_eller_opprett_sak" -> Systembrukerrolle.HENT_ELLER_OPPRETT_SAK
                    "lagre_soknad" -> Systembrukerrolle.LAGRE_SOKNAD
                    "lagre_meldekort" -> Systembrukerrolle.LAGRE_MELDEKORT
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
