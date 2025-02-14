package no.nav.tiltakspenger.saksbehandling.domene.vilkår.tiltaksdeltagelse

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.KanIkkeLeggeTilSaksopplysning

fun Behandling.leggTilTiltaksdeltagelseSaksopplysning(
    kommando: LeggTilTiltaksdeltagelseKommando,
): Either<KanIkkeLeggeTilSaksopplysning, Behandling> {
    require(saksbehandler == kommando.saksbehandler.navIdent) {
        "Kan bare legge til saksopplysninger på egen sak. Saksbehandler på behandling: $saksbehandler, utførendeSaksbehandler: ${kommando.saksbehandler}, behandlingId: ${kommando.behandlingId}"
    }
    return vilkårssett!!.oppdaterTiltaksdeltagelse(kommando).map {
        this.copy(
            vilkårssett = it,
            saksbehandler = kommando.saksbehandler.navIdent,
        )
    }
}
