package no.nav.tiltakspenger.saksbehandling.domene.vilkår.kravfrist

import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling

/** Brukes ikke i MVPen. */
fun Behandling.leggTilKravfristSaksopplysning(command: LeggTilKravfristSaksopplysningCommand): Behandling {
    require(saksbehandler == command.saksbehandler.navIdent) {
        "Kan bare legge til saksopplysninger på egen sak. Saksbehandler på behandling: $saksbehandler, utførendeSaksbehandler: ${command.saksbehandler}, behandlingId: ${command.behandlingId}"
    }

    val oppdatertFørstegangsbehandling =
        this.copy(
            vilkårssett = vilkårssett!!.oppdaterKravdato(command),
            saksbehandler = command.saksbehandler.navIdent,
        )
    return oppdatertFørstegangsbehandling
}
