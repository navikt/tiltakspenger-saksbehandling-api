@file:Suppress("ktlint:standard:max-line-length")

package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http

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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltagelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus.Avbrutt
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus.Deltar
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus.Feilregistrert
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus.Fullført
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus.HarSluttet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus.IkkeAktuell
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus.PåbegyntRegistrering
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus.SøktInn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus.Venteliste
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus.VenterPåOppstart
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus.Vurderes
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltakskilde

internal fun mapTiltak(
    tiltakDTOListe: List<TiltakTilSaksbehandlingDTO>,
): Tiltaksdeltagelser {
    // TODO fjern deprekerte felter når gjennomforing ikke er nullable
    return tiltakDTOListe
        .map { tiltakDto ->
            Tiltaksdeltagelse(
                eksternDeltagelseId = tiltakDto.id,
                gjennomføringId = tiltakDto.gjennomforing?.id ?: tiltakDto.gjennomføringId,
                typeNavn = tiltakDto.gjennomforing?.typeNavn ?: tiltakDto.typeNavn,
                typeKode =
                tiltakDto.gjennomforing?.arenaKode?.toTiltakstypeSomGirRett()?.getOrElse {
                    throw IllegalStateException(
                        "Inneholder tiltakstype som ikke gir rett (som vi ikke støtter i MVP): ${tiltakDto.typeKode}. Tiltaksid: ${tiltakDto.id}",
                    )
                } ?: tiltakDto.typeKode.toTiltakstypeSomGirRett().getOrElse {
                    throw IllegalStateException(
                        "Inneholder tiltakstype som ikke gir rett (som vi ikke støtter i MVP): ${tiltakDto.typeKode}. Tiltaksid: ${tiltakDto.id}",
                    )
                },
                rettPåTiltakspenger = tiltakDto.gjennomforing?.arenaKode?.rettPåTiltakspenger
                    ?: tiltakDto.typeKode.rettPåTiltakspenger,
                deltagelseFraOgMed = tiltakDto.deltakelseFom,
                deltagelseTilOgMed = tiltakDto.deltakelseTom,
                deltakelseStatus = tiltakDto.deltakelseStatus.toDomain(),
                antallDagerPerUke = tiltakDto.deltakelsePerUke,
                deltakelseProsent = tiltakDto.deltakelseProsent,
                kilde = tiltakDto.kilde.toTiltakskilde(tiltakDto.id),
                deltidsprosentGjennomforing = tiltakDto.gjennomforing?.deltidsprosent,

            )
        }.let { Tiltaksdeltagelser(it) }
}

internal fun mapTiltakMedArrangørnavn(
    harAdressebeskyttelse: Boolean,
    tiltakDTOListe: List<TiltakTilSaksbehandlingDTO>,
): List<TiltaksdeltakelseMedArrangørnavn> {
    // TODO fjern deprekerte felter når gjennomforing ikke er nullable
    return tiltakDTOListe
        .map { tiltakDto ->
            TiltaksdeltakelseMedArrangørnavn(
                harAdressebeskyttelse = harAdressebeskyttelse,
                arrangørnavnFørSensur = tiltakDto.gjennomforing?.arrangørnavn,
                eksternDeltakelseId = tiltakDto.id,
                gjennomføringId = tiltakDto.gjennomforing?.id ?: tiltakDto.gjennomføringId,
                typeNavn = tiltakDto.gjennomforing?.typeNavn ?: tiltakDto.typeNavn,
                typeKode =
                tiltakDto.gjennomforing?.arenaKode?.toTiltakstypeSomGirRett()?.getOrElse {
                    throw IllegalStateException(
                        "Inneholder tiltakstype som ikke gir rett (som vi ikke støtter i MVP): ${tiltakDto.typeKode}. Tiltaksid: ${tiltakDto.id}",
                    )
                } ?: tiltakDto.typeKode.toTiltakstypeSomGirRett().getOrElse {
                    throw IllegalStateException(
                        "Inneholder tiltakstype som ikke gir rett (som vi ikke støtter i MVP): ${tiltakDto.typeKode}. Tiltaksid: ${tiltakDto.id}",
                    )
                },
                rettPåTiltakspenger = tiltakDto.gjennomforing?.arenaKode?.rettPåTiltakspenger
                    ?: tiltakDto.typeKode.rettPåTiltakspenger,
                deltakelseFraOgMed = tiltakDto.deltakelseFom,
                deltakelseTilOgMed = tiltakDto.deltakelseTom,
                deltakelseStatus = tiltakDto.deltakelseStatus.toDomain(),
                antallDagerPerUke = tiltakDto.deltakelsePerUke,
                deltakelseProsent = tiltakDto.deltakelseProsent,
                kilde = tiltakDto.kilde.toTiltakskilde(tiltakDto.id),
            )
        }
}

fun String.toTiltakskilde(tiltaksId: String): Tiltakskilde {
    return when {
        this.lowercase()
            .contains("komet") -> Tiltakskilde.Komet

        this.lowercase()
            .contains("arena") -> Tiltakskilde.Arena

        else -> throw IllegalStateException(
            "Kunne ikke parse tiltak fra tiltakspenger-tiltak. Ukjent kilde: $this. Forventet Arena eller Komet. Tiltaksid: $tiltaksId",
        )
    }
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

fun TiltakTilSaksbehandlingDTO.harFomOgTomEllerRelevantStatus(
    tiltaksdeltagelserDetErSøktTiltakspengerFor: TiltaksdeltagelserDetErSøktTiltakspengerFor,
): Boolean {
    if (tiltaksdeltagelserDetErSøktTiltakspengerFor.ider.contains(id)) {
        return true
    }
    if (deltakelseFom != null || deltakelseTom != null) {
        return true
    }
    // Venter på oppstart er den eneste statusen der datoer kan mangle og som kan være relevant ift tiltakspenger
    return deltakelseStatus == VENTER_PA_OPPSTART
}

fun TiltakTilSaksbehandlingDTO.rettPaTiltakspenger(): Boolean =
    typeKode.toTiltakstypeSomGirRett().isRight()
