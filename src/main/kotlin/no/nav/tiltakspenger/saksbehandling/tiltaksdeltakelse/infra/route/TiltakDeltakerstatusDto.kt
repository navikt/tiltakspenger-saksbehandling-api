package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus

enum class TiltakDeltakerstatusDto {
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

fun TiltakDeltakerstatus.toDto(): TiltakDeltakerstatusDto = when (this) {
    TiltakDeltakerstatus.VenterPåOppstart -> TiltakDeltakerstatusDto.VenterPåOppstart
    TiltakDeltakerstatus.Deltar -> TiltakDeltakerstatusDto.Deltar
    TiltakDeltakerstatus.HarSluttet -> TiltakDeltakerstatusDto.HarSluttet
    TiltakDeltakerstatus.Avbrutt -> TiltakDeltakerstatusDto.Avbrutt
    TiltakDeltakerstatus.Fullført -> TiltakDeltakerstatusDto.Fullført
    TiltakDeltakerstatus.IkkeAktuell -> TiltakDeltakerstatusDto.IkkeAktuell
    TiltakDeltakerstatus.Feilregistrert -> TiltakDeltakerstatusDto.Feilregistrert
    TiltakDeltakerstatus.PåbegyntRegistrering -> TiltakDeltakerstatusDto.PåbegyntRegistrering
    TiltakDeltakerstatus.SøktInn -> TiltakDeltakerstatusDto.SøktInn
    TiltakDeltakerstatus.Venteliste -> TiltakDeltakerstatusDto.Venteliste
    TiltakDeltakerstatus.Vurderes -> TiltakDeltakerstatusDto.Vurderes
}
