package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.common.GenerellSystembruker
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.saksbehandling.auth.systembrukerMapper
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration

/**
 * Brukes for å mappe verifisert saksbehandler-token til Saksbehandler
 */
fun autoriserteBrukerroller() = Configuration.alleAdRoller()

/**
 * Brukes for å mappe verifisert systembruker-token til Systembruker
 */
@Suppress("UNCHECKED_CAST")
fun getSystemBrukerMapper() = ::systembrukerMapper as (String, String, Set<String>) -> GenerellSystembruker<
    GenerellSystembrukerrolle,
    GenerellSystembrukerroller<GenerellSystembrukerrolle>,
    >

/**
 * Kaster en [TilgangException] dersom saksbehandler ikke har rollen [Saksbehandlerrolle.SAKSBEHANDLER].
 */
fun krevSaksbehandlerRolle(
    saksbehandler: Saksbehandler,
) = krevRollen(saksbehandler, Saksbehandlerrolle.SAKSBEHANDLER)

/**
 * Kaster en [TilgangException] dersom saksbehandler ikke har rollen [Saksbehandlerrolle.BESLUTTER].
 */
fun krevBeslutterRolle(
    saksbehandler: Saksbehandler,
) = krevRollen(saksbehandler, Saksbehandlerrolle.BESLUTTER)

/**
 * Kaster en [TilgangException] dersom saksbehandler ikke har rollen [Saksbehandlerrolle.SAKSBEHANDLER] eller [Saksbehandlerrolle.BESLUTTER].
 */
fun krevSaksbehandlerEllerBeslutterRolle(
    saksbehandler: Saksbehandler,
) = krevEnAvRollene(saksbehandler, Saksbehandlerrolle.SAKSBEHANDLER, Saksbehandlerrolle.BESLUTTER)

/**
 * Kaster en [TilgangException] dersom saksbehandler ikke har rollen [krevRollen].
 */
fun krevRollen(saksbehandler: Saksbehandler, krevRollen: Saksbehandlerrolle) {
    if (!saksbehandler.roller.contains(krevRollen)) {
        throw TilgangException(saksbehandler, krevRollen)
    }
}

/**
 * Kaster en [TilgangException] dersom saksbehandler ikke har en av rollene i [krevEnAvRollene].
 */
fun krevEnAvRollene(saksbehandler: Saksbehandler, vararg krevEnAvRollene: Saksbehandlerrolle) {
    return krevEnAvRollene(saksbehandler, krevEnAvRollene.toList())
}

/**
 * Kaster en [TilgangException] dersom saksbehandler ikke har en av rollene i [krevEnAvRollene].
 */
fun krevEnAvRollene(saksbehandler: Saksbehandler, krevEnAvRollene: Saksbehandlerroller) {
    return krevEnAvRollene(saksbehandler, krevEnAvRollene.toList())
}

/**
 * Kaster en [TilgangException] dersom saksbehandler ikke har en av rollene i [krevEnAvRollene].
 */
fun krevEnAvRollene(saksbehandler: Saksbehandler, krevEnAvRollene: List<Saksbehandlerrolle>) {
    if (krevEnAvRollene.none { saksbehandler.roller.contains(it) }) {
        throw TilgangException(saksbehandler, krevEnAvRollene)
    }
}

fun krevHentEllerOpprettSakRollen(systembruker: Systembruker) {
    if (!systembruker.roller.harHentEllerOpprettSak()) {
        throw TilgangException("$systembruker mangler rollen HENT_ELLER_OPPRETT_SAK.")
    }
}

fun krevLagreSoknadRollen(systembruker: Systembruker) {
    if (!systembruker.roller.harLagreSoknad()) {
        throw TilgangException("$systembruker mangler rollen LAGRE_SOKNAD.")
    }
}

fun krevLagreMeldekortRollen(systembruker: Systembruker) {
    if (!systembruker.roller.harLagreMeldekort()) {
        throw TilgangException("$systembruker mangler rollen LAGRE_MELDEKORT.")
    }
}
