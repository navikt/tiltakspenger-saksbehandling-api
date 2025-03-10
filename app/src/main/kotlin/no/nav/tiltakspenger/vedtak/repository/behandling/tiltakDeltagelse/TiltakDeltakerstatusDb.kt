package no.nav.tiltakspenger.vedtak.repository.behandling.tiltakDeltagelse

import no.nav.tiltakspenger.vedtak.saksbehandling.domene.tiltak.TiltakDeltakerstatus

private enum class TiltakDeltakerstatusDb {
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

internal fun TiltakDeltakerstatus.toDb(): String =
    when (this) {
        TiltakDeltakerstatus.VenterPåOppstart -> TiltakDeltakerstatusDb.VenterPåOppstart.name
        TiltakDeltakerstatus.Deltar -> TiltakDeltakerstatusDb.Deltar.name
        TiltakDeltakerstatus.HarSluttet -> TiltakDeltakerstatusDb.HarSluttet.name
        TiltakDeltakerstatus.Avbrutt -> TiltakDeltakerstatusDb.Avbrutt.name
        TiltakDeltakerstatus.Fullført -> TiltakDeltakerstatusDb.Fullført.name
        TiltakDeltakerstatus.IkkeAktuell -> TiltakDeltakerstatusDb.IkkeAktuell.name
        TiltakDeltakerstatus.Feilregistrert -> TiltakDeltakerstatusDb.Feilregistrert.name
        TiltakDeltakerstatus.PåbegyntRegistrering -> TiltakDeltakerstatusDb.PåbegyntRegistrering.name
        TiltakDeltakerstatus.SøktInn -> TiltakDeltakerstatusDb.SøktInn.name
        TiltakDeltakerstatus.Venteliste -> TiltakDeltakerstatusDb.Venteliste.name
        TiltakDeltakerstatus.Vurderes -> TiltakDeltakerstatusDb.Vurderes.name
    }

internal fun String.toTiltakDeltakerstatus(): TiltakDeltakerstatus =
    when (TiltakDeltakerstatusDb.valueOf(this)) {
        TiltakDeltakerstatusDb.VenterPåOppstart -> TiltakDeltakerstatus.VenterPåOppstart
        TiltakDeltakerstatusDb.Deltar -> TiltakDeltakerstatus.Deltar
        TiltakDeltakerstatusDb.HarSluttet -> TiltakDeltakerstatus.HarSluttet
        TiltakDeltakerstatusDb.Avbrutt -> TiltakDeltakerstatus.Avbrutt
        TiltakDeltakerstatusDb.Fullført -> TiltakDeltakerstatus.Fullført
        TiltakDeltakerstatusDb.IkkeAktuell -> TiltakDeltakerstatus.IkkeAktuell
        TiltakDeltakerstatusDb.Feilregistrert -> TiltakDeltakerstatus.Feilregistrert
        TiltakDeltakerstatusDb.PåbegyntRegistrering -> TiltakDeltakerstatus.PåbegyntRegistrering
        TiltakDeltakerstatusDb.SøktInn -> TiltakDeltakerstatus.SøktInn
        TiltakDeltakerstatusDb.Venteliste -> TiltakDeltakerstatus.Venteliste
        TiltakDeltakerstatusDb.Vurderes -> TiltakDeltakerstatus.Vurderes
    }
