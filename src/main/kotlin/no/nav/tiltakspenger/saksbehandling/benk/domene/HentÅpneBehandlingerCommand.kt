package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand

data class HentÅpneBehandlingerCommand(
    val åpneBehandlingerFiltrering: ÅpneBehandlingerFiltrering,
    val sortering: Sortering,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand

data class ÅpneBehandlingerFiltrering(
    val behandlingstype: List<BehandlingssammendragType>?,
    val status: List<BehandlingssammendragStatus>?,
)

enum class Sortering {
    ASC,
    DESC,
}
