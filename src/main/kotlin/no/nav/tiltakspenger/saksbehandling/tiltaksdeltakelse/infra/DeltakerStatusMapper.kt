package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.AVBRUTT
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.DELTAR
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.FEILREGISTRERT
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.FULLFORT
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.HAR_SLUTTET
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.IKKE_AKTUELL
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.PABEGYNT_REGISTRERING
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.SOKT_INN
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.VENTELISTE
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.VENTER_PA_OPPSTART
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.VURDERES
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.Avbrutt
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.Deltar
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.Feilregistrert
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.Fullført
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.HarSluttet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.IkkeAktuell
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.PåbegyntRegistrering
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.SøktInn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.Venteliste
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.VenterPåOppstart
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.Vurderes

/**
 * Brukes av kafka-konsumentene for tiltaksdeltakerhendelser.
 * HTTP-kjeden mapper ikke lenger gjennom [DeltakerStatusDTO] — den mapper [no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kildestatus] direkte i `infra/http/TiltakshistorikkTilRegisterMapper.kt`.
 */
fun DeltakerStatusDTO.toDomain(): TiltakDeltakerstatus {
    return when (this) {
        VURDERES -> Vurderes
        VENTER_PA_OPPSTART -> VenterPåOppstart
        DELTAR -> Deltar
        HAR_SLUTTET -> HarSluttet
        AVBRUTT -> Avbrutt
        IKKE_AKTUELL -> IkkeAktuell
        FEILREGISTRERT -> Feilregistrert
        PABEGYNT_REGISTRERING -> PåbegyntRegistrering
        SOKT_INN -> SøktInn
        VENTELISTE -> Venteliste
        FULLFORT -> Fullført
    }
}
