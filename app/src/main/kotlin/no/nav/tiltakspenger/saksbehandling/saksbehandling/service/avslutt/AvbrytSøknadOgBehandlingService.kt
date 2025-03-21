package no.nav.tiltakspenger.saksbehandling.saksbehandling.service.avslutt

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer

interface AvbrytSøknadOgBehandlingService {
    suspend fun avbrytSøknadOgBehandling(command: AvbrytSøknadOgBehandlingCommand): Either<KunneIkkeAvbryteSøknadOgBehandling, Sak>
}

sealed interface KunneIkkeAvbryteSøknadOgBehandling {
    data object Feil : KunneIkkeAvbryteSøknadOgBehandling
}

data class AvbrytSøknadOgBehandlingCommand(
    val saksnummer: Saksnummer,
    val søknadId: SøknadId?,
    val behandlingId: BehandlingId?,
    val avsluttetAv: Saksbehandler,
    val correlationId: CorrelationId,
    val begrunnelse: String,
) {
    init {
        require(søknadId != null || behandlingId != null) { "Enten søknadId eller behandlingId må være satt" }
    }
}
