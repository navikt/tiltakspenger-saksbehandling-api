package no.nav.tiltakspenger.saksbehandling.domene.vilkår.alder

import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling

fun Behandling.leggTilAlderSaksopplysning(command: LeggTilAlderSaksopplysningCommand): Behandling {
    require(saksbehandler == command.saksbehandler.navIdent) {
        "Kan bare legge til saksopplysninger på egen sak. Saksbehandler på behandling: $saksbehandler, utførendeSaksbehandler: ${command.saksbehandler}, behandlingId: ${command.behandlingId}"
    }

    val oppdatertFørstegangsbehandling =
        this.copy(
            vilkårssett = vilkårssett!!.oppdaterAlder(command),
            saksbehandler = command.saksbehandler.navIdent,
        )
    return oppdatertFørstegangsbehandling
}
