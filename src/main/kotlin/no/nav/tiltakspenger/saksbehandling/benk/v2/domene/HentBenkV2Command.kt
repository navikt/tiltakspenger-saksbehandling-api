package no.nav.tiltakspenger.saksbehandling.benk.v2.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand

/**
 * Filtrene fanene tilbyr.
 * `null` betyr «ikke filtrert».
 *
 * [saksbehandler] er ett felt som treffer både saksbehandler og beslutter, fordi benken har én nedtrekksliste for de to.
 * Verdien [IKKE_TILDELT] betyr at behandlingen ikke er plukket opp av noen.
 */
sealed interface BenkV2Filtrering {
    val saksbehandler: String?

    companion object {
        const val IKKE_TILDELT: String = "IKKE_TILDELT"
    }
}

data class BenkSøknaderFiltrering(
    val status: BenkV2Behandlingsstatus?,
    val søknadstype: BenkSøknadstype?,
    override val saksbehandler: String?,
) : BenkV2Filtrering

data class BenkRevurderingerFiltrering(
    val status: BenkV2Behandlingsstatus?,
    val resultat: BenkRevurderingResultat?,
    override val saksbehandler: String?,
) : BenkV2Filtrering

data class BenkMeldekortFiltrering(
    val status: BenkV2Behandlingsstatus?,
    val type: BenkMeldekortType?,
    override val saksbehandler: String?,
) : BenkV2Filtrering

data class BenkKlageFiltrering(
    val status: BenkV2Behandlingsstatus?,
    val resultat: BenkKlagebehandlingResultat?,
    override val saksbehandler: String?,
) : BenkV2Filtrering

data class BenkTilbakekrevingFiltrering(
    val status: BenkTilbakekrevingStatus?,
    val kilde: BenkTilbakekrevingKilde?,
    override val saksbehandler: String?,
    val minstebeløp: Long,
) : BenkV2Filtrering

/**
 * Ett kall henter én fane.
 * Kommandoen er derfor generisk over fanens filter og fanens sorteringskolonner, slik at feil kombinasjon ikke kompilerer.
 */
data class HentBenkV2Command<F : BenkV2Filtrering, K : BenkV2SorteringKolonne>(
    val filtrering: F,
    val sortering: BenkV2Sortering<K>,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand
