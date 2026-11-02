package no.nav.tiltakspenger.saksbehandling.auth.infra

import no.nav.tiltakspenger.libs.common.GenerellSystembruker
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.saksbehandling.auth.systembrukerMapper
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration

/**
 * AD-gruppene som gir saksbehandlerroller, brukes for å mappe et verifisert saksbehandler-token til Saksbehandler.
 */
fun autoriserteBrukerroller() = Configuration.alleAdRoller

/**
 * Brukes for å mappe et verifisert systembruker-token til Systembruker.
 */
@Suppress("UNCHECKED_CAST")
fun getSystemBrukerMapper() = ::systembrukerMapper as (String, String, Set<String>) -> GenerellSystembruker<
    GenerellSystembrukerrolle,
    GenerellSystembrukerroller<GenerellSystembrukerrolle>,
    >
