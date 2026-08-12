package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand

/**
 * Filtrene fanene tilbyr.
 * `null` betyr «ikke filtrert».
 *
 * [saksbehandler] er ett felt som treffer både saksbehandler og beslutter, fordi benken har én nedtrekksliste for de to.
 * Verdien [IKKE_TILDELT] betyr at behandlingen ikke er plukket opp av noen.
 *
 * [skjulPåVent] tar bort behandlingene som er satt på vent, for saksbehandlere som vil se køen av det som faktisk kan jobbes med.
 */
sealed interface BenkFiltrering {
    val saksbehandler: String?
    val skjulPåVent: Boolean

    companion object {
        const val IKKE_TILDELT: String = "IKKE_TILDELT"
    }
}

data class BenkSøknaderFiltrering(
    val status: BenkBehandlingsstatus?,
    val søknadstype: BenkSøknadstype?,
    val resultat: BenkSøknadsbehandlingResultat?,
    override val saksbehandler: String?,
    override val skjulPåVent: Boolean = false,
) : BenkFiltrering

data class BenkRevurderingerFiltrering(
    val status: BenkBehandlingsstatus?,
    val resultat: BenkRevurderingResultat?,
    override val saksbehandler: String?,
    override val skjulPåVent: Boolean = false,
) : BenkFiltrering

data class BenkMeldekortFiltrering(
    val status: BenkBehandlingsstatus?,
    val type: BenkMeldekortType?,
    override val saksbehandler: String?,
    override val skjulPåVent: Boolean = false,
) : BenkFiltrering

data class BenkKlageFiltrering(
    val status: BenkBehandlingsstatus?,
    val resultat: BenkKlagebehandlingResultat?,
    override val saksbehandler: String?,
    override val skjulPåVent: Boolean = false,
) : BenkFiltrering

data class BenkTilbakekrevingFiltrering(
    val status: BenkTilbakekrevingStatus?,
    val kilde: BenkTilbakekrevingKilde?,
    override val saksbehandler: String?,
    val minstebeløp: Long,
    override val skjulPåVent: Boolean = false,
) : BenkFiltrering

/**
 * Ett kall henter én fane.
 * Kommandoen er derfor generisk over fanens filter og fanens sorteringskolonner, slik at feil kombinasjon ikke kompilerer.
 */
data class HentBenkKommando<F : BenkFiltrering, K : BenkSorteringKolonne>(
    val filtrering: F,
    val sortering: BenkSortering<K>,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand
