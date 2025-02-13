package no.nav.tiltakspenger.saksbehandling.domene.vilkår.introduksjonsprogrammet

import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling

fun Behandling.leggTilIntroSaksopplysning(command: LeggTilIntroSaksopplysningCommand): Behandling {
    require(saksbehandler == command.saksbehandler.navIdent) {
        "Kan bare legge til saksopplysninger på egen sak. Saksbehandler på behandling: $saksbehandler, utførendeSaksbehandler: ${command.saksbehandler}, behandlingId: ${command.behandlingId}"
    }

    val oppdatertFørstegangsbehandling =
        this.copy(
            vilkårssett = vilkårssett!!.oppdaterIntro(command),
            saksbehandler = command.saksbehandler.navIdent,
        )
    return oppdatertFørstegangsbehandling
}
