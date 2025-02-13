package no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.KanIkkeLeggeTilSaksopplysning

fun Behandling.leggTilLivsoppholdSaksopplysning(
    command: LeggTilLivsoppholdSaksopplysningCommand,
): Either<KanIkkeLeggeTilSaksopplysning, Behandling> {
    require(saksbehandler == command.saksbehandler.navIdent) {
        "Kan bare legge til saksopplysninger på egen sak. Saksbehandler på behandling: $saksbehandler, utførendeSaksbehandler: ${command.saksbehandler}, behandlingId: ${command.behandlingId}"
    }

    return vilkårssett!!
        .oppdaterLivsopphold(command)
        .map {
            this.copy(
                vilkårssett = it,
                saksbehandler = command.saksbehandler.navIdent,
            )
        }
}
