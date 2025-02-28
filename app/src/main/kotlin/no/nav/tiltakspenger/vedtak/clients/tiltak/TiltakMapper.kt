@file:Suppress("ktlint:standard:max-line-length")

package no.nav.tiltakspenger.vedtak.clients.tiltak

import arrow.core.getOrElse
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
import no.nav.tiltakspenger.libs.tiltak.TiltakTilSaksbehandlingDTO
import no.nav.tiltakspenger.libs.tiltak.toTiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.Avbrutt
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.Deltar
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.Feilregistrert
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.Fullført
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.HarSluttet
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.IkkeAktuell
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.PåbegyntRegistrering
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.SøktInn
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.Venteliste
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.VenterPåOppstart
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.Vurderes
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltakskilde

internal fun mapTiltak(
    tiltakDTOListe: List<TiltakTilSaksbehandlingDTO>,
): List<Tiltaksdeltagelse> =
    tiltakDTOListe
        .filterNot { it.deltakelseFom == null }
        .filterNot { it.deltakelseTom == null }
        .map { tiltakDto ->
            Tiltaksdeltagelse(
                eksternDeltagelseId = tiltakDto.id,
                gjennomføringId = tiltakDto.gjennomføringId,
                typeNavn = tiltakDto.typeNavn,
                typeKode = tiltakDto.typeKode.toTiltakstypeSomGirRett().getOrElse {
                    throw IllegalStateException(
                        "Inneholder tiltakstype som ikke gir rett (som vi ikke støtter i MVP): ${tiltakDto.typeKode}. Tiltaksid: ${tiltakDto.id}",
                    )
                },
                rettPåTiltakspenger = tiltakDto.typeKode.rettPåTiltakspenger,
                deltagelseFraOgMed = tiltakDto.deltakelseFom,
                deltagelseTilOgMed = tiltakDto.deltakelseTom,
                deltakelseStatus = tiltakDto.deltakelseStatus.toDomain(),
                antallDagerPerUke = tiltakDto.deltakelsePerUke,
                deltakelseProsent = tiltakDto.deltakelseProsent,
                kilde =
                when {
                    tiltakDto.kilde.lowercase().contains("komet") -> Tiltakskilde.Komet
                    tiltakDto.kilde.lowercase().contains("arena") -> Tiltakskilde.Arena
                    else -> throw IllegalStateException(
                        "Kunne ikke parse tiltak fra tiltakspenger-tiltak. Ukjent kilde: ${tiltakDto.kilde}. Forventet Arena eller Komet. Tiltaksid: ${tiltakDto.id}",
                    )
                },
            )
        }

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
