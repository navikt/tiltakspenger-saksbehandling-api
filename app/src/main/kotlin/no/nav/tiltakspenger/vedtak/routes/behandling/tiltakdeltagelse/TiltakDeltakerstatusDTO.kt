package no.nav.tiltakspenger.vedtak.routes.behandling.tiltakdeltagelse

import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus

private enum class TiltakDeltakerstatusDTO {
    VenterPåOppstart,
    Deltar,
    HarSluttet,
    Avbrutt,
    Fullført,
    IkkeAktuell,
    Feilregistrert,
    PåbegyntRegistrering,
    SøktInn,
    Venteliste,
    Vurderes,
}

internal fun TiltakDeltakerstatus.toDTO(): String =
    when (this) {
        TiltakDeltakerstatus.VenterPåOppstart -> TiltakDeltakerstatusDTO.VenterPåOppstart.name
        TiltakDeltakerstatus.Deltar -> TiltakDeltakerstatusDTO.Deltar.name
        TiltakDeltakerstatus.HarSluttet -> TiltakDeltakerstatusDTO.HarSluttet.name
        TiltakDeltakerstatus.Avbrutt -> TiltakDeltakerstatusDTO.Avbrutt.name
        TiltakDeltakerstatus.Fullført -> TiltakDeltakerstatusDTO.Fullført.name
        TiltakDeltakerstatus.IkkeAktuell -> TiltakDeltakerstatusDTO.IkkeAktuell.name
        TiltakDeltakerstatus.Feilregistrert -> TiltakDeltakerstatusDTO.Feilregistrert.name
        TiltakDeltakerstatus.PåbegyntRegistrering -> TiltakDeltakerstatusDTO.PåbegyntRegistrering.name
        TiltakDeltakerstatus.SøktInn -> TiltakDeltakerstatusDTO.SøktInn.name
        TiltakDeltakerstatus.Venteliste -> TiltakDeltakerstatusDTO.Venteliste.name
        TiltakDeltakerstatus.Vurderes -> TiltakDeltakerstatusDTO.Vurderes.name
    }
