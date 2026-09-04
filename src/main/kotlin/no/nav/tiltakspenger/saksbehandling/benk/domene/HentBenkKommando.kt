package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand

/**
 * Filtrene fanene tilbyr.
 * `null` betyr «ikke filtrert».
 *
 * [saksbehandler] er ett felt som treffer både saksbehandler og beslutter, fordi benken har én nedtrekksliste for de to.
 * Verdien [IKKE_TILDELT] betyr at saksbehandler eller beslutter (eller begge) ikke er tildelt.
 * [IKKE_TILDELT_SAKSBEHANDLER] treffer raden som ikke har saksbehandler.
 * [IKKE_TILDELT_BESLUTTER] treffer raden som har saksbehandler, men ikke beslutter.
 *
 * [skjulPåVent] tar bort behandlingene som er satt på vent, for saksbehandlere som vil se køen av det som faktisk kan jobbes med.
 *
 * [skjulVenterPåAnnenSaksbehandler] tar bort behandlingene som venter på en annen saksbehandler - enten de som kallende saksbehandler har sendt til beslutter, eller som kallende saksbehandler har underkjent
 *
 * Fanene uten et beslutningssteg (klage) har ikke filteret.
 */
sealed interface BenkFiltrering {
    val saksbehandler: String?
    val skjulPåVent: Boolean
    val skjulVenterPåAnnenSaksbehandler: Boolean

    companion object {
        const val IKKE_TILDELT: String = "IKKE_TILDELT"
        const val IKKE_TILDELT_SAKSBEHANDLER: String = "IKKE_TILDELT_SAKSBEHANDLER"
        const val IKKE_TILDELT_BESLUTTER: String = "IKKE_TILDELT_BESLUTTER"
    }
}

data class BenkSøknaderFiltrering(
    val status: BenkBehandlingsstatus?,
    val søknadstype: BenkSøknadstype?,
    val resultat: BenkSøknadsbehandlingResultat?,
    override val saksbehandler: String?,
    override val skjulPåVent: Boolean = false,
    override val skjulVenterPåAnnenSaksbehandler: Boolean = false,
) : BenkFiltrering

data class BenkRevurderingerFiltrering(
    val status: BenkBehandlingsstatus?,
    val resultat: BenkRevurderingResultat?,
    override val saksbehandler: String?,
    override val skjulPåVent: Boolean = false,
    override val skjulVenterPåAnnenSaksbehandler: Boolean = false,
) : BenkFiltrering

data class BenkMeldekortFiltrering(
    val status: BenkBehandlingsstatus?,
    val type: BenkMeldekortType?,
    override val saksbehandler: String?,
    override val skjulPåVent: Boolean = false,
    override val skjulVenterPåAnnenSaksbehandler: Boolean = false,
) : BenkFiltrering

data class BenkKlageFiltrering(
    val status: BenkBehandlingsstatus?,
    val resultat: BenkKlagebehandlingResultat?,
    override val saksbehandler: String?,
    override val skjulPåVent: Boolean = false,
) : BenkFiltrering {
    override val skjulVenterPåAnnenSaksbehandler: Boolean = false
}

data class BenkTilbakekrevingFiltrering(
    val status: BenkTilbakekrevingStatus?,
    val kilde: BenkTilbakekrevingKilde?,
    override val saksbehandler: String?,
    val minstebeløp: Long,
    override val skjulPåVent: Boolean = false,
    /** Tilbakekreving kaller beslutningssteget godkjenning, men filteret er det samme. */
    override val skjulVenterPåAnnenSaksbehandler: Boolean = false,
) : BenkFiltrering

/**
 * Ett kall henter én fane.
 * Kommandoen er derfor generisk over fanens filter og fanens sorteringskolonner, slik at feil kombinasjon ikke kompilerer.
 */
data class HentBenkKommando<F : BenkFiltrering, K : BenkSorteringKolonne>(
    val filtrering: F,
    val sortering: BenkSortering<K>,
    val paginering: BenkPaginering = BenkPaginering(),
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand
