package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.Tilgangsnektårsak

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
 * Kaster en [TilgangException] dersom saksbehandler ikke har rollen [Saksbehandlerrolle.TILBAKEKREVING].
 * Rollen kreves i tillegg til saksbehandler- eller beslutterrollen for å endre tilbakekrevingsbehandlinger.
 */
fun krevTilbakekrevingRolle(
    saksbehandler: Saksbehandler,
) = krevRollen(saksbehandler, Saksbehandlerrolle.TILBAKEKREVING)

/**
 * Kaster en [TilgangException] dersom saksbehandler ikke har rollen [krevRollen].
 */
fun krevRollen(saksbehandler: Saksbehandler, krevRollen: Saksbehandlerrolle) {
    if (!saksbehandler.roller.contains(krevRollen)) {
        throw TilgangException(Tilgangsnektårsak.KREV_ROLLEN, saksbehandler, krevRollen)
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
        throw TilgangException(Tilgangsnektårsak.KREV_ROLLEN, saksbehandler, krevEnAvRollene)
    }
}

/**
 * Kaster en [TilgangException] dersom systembrukeren ikke har rollen [Systembrukerrolle.HENT_ELLER_OPPRETT_SAK].
 */
fun krevHentEllerOpprettSakRollen(systembruker: Systembruker) {
    if (!systembruker.roller.harHentEllerOpprettSak()) {
        throw TilgangException(Tilgangsnektårsak.MANGLER_ROLLE, "$systembruker mangler rollen HENT_ELLER_OPPRETT_SAK.")
    }
}

/**
 * Kaster en [TilgangException] dersom systembrukeren ikke har rollen [Systembrukerrolle.LAGRE_SOKNAD].
 */
fun krevLagreSoknadRollen(systembruker: Systembruker) {
    if (!systembruker.roller.harLagreSoknad()) {
        throw TilgangException(Tilgangsnektårsak.MANGLER_ROLLE, "$systembruker mangler rollen LAGRE_SOKNAD.")
    }
}

/**
 * Kaster en [TilgangException] dersom systembrukeren ikke har rollen [Systembrukerrolle.LAGRE_MELDEKORT].
 */
fun krevLagreMeldekortRollen(systembruker: Systembruker) {
    if (!systembruker.roller.harLagreMeldekort()) {
        throw TilgangException(Tilgangsnektårsak.MANGLER_ROLLE, "$systembruker mangler rollen LAGRE_MELDEKORT.")
    }
}
