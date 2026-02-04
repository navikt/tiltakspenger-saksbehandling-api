@file:Suppress("ktlint:standard:max-line-length")

package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

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
import no.nav.tiltakspenger.libs.tiltak.TiltakshistorikkDTO
import no.nav.tiltakspenger.libs.tiltak.toTiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
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
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde

internal fun mapTiltak(
    tiltakDTOListe: List<TiltakshistorikkDTO>,
): TiltaksdeltakelserFraRegister {
    return tiltakDTOListe
        .map { tiltakDto ->
            TiltaksdeltakelseFraRegister(
                eksternDeltakelseId = tiltakDto.id,
                gjennomføringId = tiltakDto.gjennomforing.id,
                typeNavn = tiltakDto.gjennomforing.typeNavn,
                typeKode =
                tiltakDto.gjennomforing.arenaKode.toTiltakstypeSomGirRett().getOrElse {
                    throw IllegalStateException(
                        "Inneholder tiltakstype som ikke gir rett (som vi ikke støtter i MVP): ${tiltakDto.gjennomforing.arenaKode}. Tiltaksid: ${tiltakDto.id}",
                    )
                },
                rettPåTiltakspenger = tiltakDto.gjennomforing.arenaKode.rettPåTiltakspenger,
                deltakelseFraOgMed = tiltakDto.deltakelseFom,
                deltakelseTilOgMed = tiltakDto.deltakelseTom,
                deltakelseStatus = tiltakDto.deltakelseStatus.toDomain(),
                antallDagerPerUke = tiltakDto.antallDagerPerUke,
                deltakelseProsent = tiltakDto.deltakelseProsent,
                kilde = tiltakDto.kilde.toTiltakskilde(),
                deltidsprosentGjennomforing = tiltakDto.gjennomforing.deltidsprosent,

            )
        }.let { TiltaksdeltakelserFraRegister(it) }
}

internal fun mapTiltakMedArrangørnavn(
    harAdressebeskyttelse: Boolean,
    tiltakDTOListe: List<TiltakshistorikkDTO>,
): List<TiltaksdeltakelseMedArrangørnavn> {
    return tiltakDTOListe
        .map { tiltakDto ->
            TiltaksdeltakelseMedArrangørnavn(
                eksternDeltakelseId = tiltakDto.id,
                typeNavn = tiltakDto.gjennomforing.typeNavn,
                typeKode =
                tiltakDto.gjennomforing.arenaKode.toTiltakstypeSomGirRett().getOrElse {
                    throw IllegalStateException(
                        "Inneholder tiltakstype som ikke gir rett (som vi ikke støtter i MVP): ${tiltakDto.gjennomforing.arenaKode}. Tiltaksid: ${tiltakDto.id}",
                    )
                },
                deltakelseFraOgMed = tiltakDto.deltakelseFom,
                deltakelseTilOgMed = tiltakDto.deltakelseTom,
                visningsnavn = if (!harAdressebeskyttelse) {
                    tiltakDto.gjennomforing.visningsnavn
                } else {
                    tiltakDto.gjennomforing.typeNavn
                },
            )
        }
}

fun TiltakshistorikkDTO.Kilde.toTiltakskilde(): Tiltakskilde {
    return when (this) {
        TiltakshistorikkDTO.Kilde.KOMET -> Tiltakskilde.Komet
        TiltakshistorikkDTO.Kilde.ARENA -> Tiltakskilde.Arena
        TiltakshistorikkDTO.Kilde.TEAM_TILTAK -> Tiltakskilde.TeamTiltak
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

fun TiltakshistorikkDTO.harFomOgTomEllerRelevantStatus(
    tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
): Boolean {
    if (tiltaksdeltakelserDetErSøktTiltakspengerFor.eksterneIder.contains(id)) {
        return true
    }
    if (deltakelseFom != null || deltakelseTom != null) {
        return true
    }
    // Venter på oppstart er den eneste statusen der datoer kan mangle og som kan være relevant ift tiltakspenger
    return deltakelseStatus == VENTER_PA_OPPSTART
}

fun TiltakshistorikkDTO.rettPaTiltakspenger(): Boolean =
    gjennomforing.arenaKode.toTiltakstypeSomGirRett().isRight()
