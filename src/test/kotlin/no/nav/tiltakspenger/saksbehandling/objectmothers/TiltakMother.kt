package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.getOrElse
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.libs.tiltak.toTiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus.Deltar
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltakskilde.Komet
import java.time.LocalDate
import java.util.UUID

interface TiltakMother {

    /**
     * Dupliserer dene med [tiltaksdeltagelse] for å være bakoverkompabilitet med tester som bruker tac
     */
    fun tiltaksdeltagelseTac(
        eksternTiltaksdeltagelseId: String = "TA12345",
        typeKode: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        typeNavn: String = "Testnavn",
        eksternTiltaksgjennomføringsId: String? = null,
        fom: LocalDate,
        tom: LocalDate,
        status: TiltakDeltakerstatus = Deltar,
        dagerPrUke: Float? = 5F,
        prosent: Float? = 100F,
        rettPåTiltakspenger: Boolean = true,
        kilde: Tiltakskilde = Tiltakskilde.Arena,
        deltidsprosentGjennomforing: Double? = null,
    ): Tiltaksdeltagelse {
        return Tiltaksdeltagelse(
            eksternDeltagelseId = eksternTiltaksdeltagelseId,
            gjennomføringId = eksternTiltaksgjennomføringsId,
            typeKode = typeKode,
            typeNavn = typeNavn,
            rettPåTiltakspenger = rettPåTiltakspenger,
            deltagelseFraOgMed = fom,
            deltagelseTilOgMed = tom,
            deltakelseStatus = status,
            deltakelseProsent = prosent,
            kilde = kilde,
            antallDagerPerUke = dagerPrUke,
            deltidsprosentGjennomforing = deltidsprosentGjennomforing,
        )
    }

    fun tiltaksdeltagelse(
        eksternTiltaksdeltagelseId: String = UUID.randomUUID().toString(),
        typeKode: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        typeNavn: String = "Arbeidsmarkedsoppfølging gruppe",
        eksternTiltaksgjennomføringsId: String = UUID.randomUUID().toString(),
        fom: LocalDate = 1.januar(2023),
        tom: LocalDate = 31.mars(2023),
        status: TiltakDeltakerstatus = Deltar,
        dagerPrUke: Float? = 5F,
        prosent: Float? = 100F,
        rettPåTiltakspenger: Boolean = true,
        kilde: Tiltakskilde = Komet,
        deltidsprosentGjennomforing: Double? = null,
    ): Tiltaksdeltagelse {
        return Tiltaksdeltagelse(
            eksternDeltagelseId = eksternTiltaksdeltagelseId,
            gjennomføringId = eksternTiltaksgjennomføringsId,
            typeKode = typeKode,
            typeNavn = typeNavn,
            rettPåTiltakspenger = rettPåTiltakspenger,
            deltagelseFraOgMed = fom,
            deltagelseTilOgMed = tom,
            deltakelseStatus = status,
            deltakelseProsent = prosent,
            kilde = kilde,
            antallDagerPerUke = dagerPrUke,
            deltidsprosentGjennomforing = deltidsprosentGjennomforing,
        )
    }

    fun tiltakOgSøknadstiltak(
        eksternTiltaksdeltagelseId: String = UUID.randomUUID().toString(),
        typeKode: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        typeNavn: String = "Arbeidsmarkedsoppfølging gruppe",
        eksternTiltaksgjennomføringsId: String = UUID.randomUUID().toString(),
        søknadFraOgMed: LocalDate = 1.januar(2023),
        søknadTilOgMed: LocalDate = 31.mars(2023),
        registerFraOgMed: LocalDate? = søknadFraOgMed,
        registerTilOgMed: LocalDate? = søknadTilOgMed,
        status: TiltakDeltakerstatus = Deltar,
        dagerPrUke: Float? = 5F,
        prosent: Float? = 100F,
        rettPåTiltakspenger: Boolean = true,
        kilde: Tiltakskilde = Komet,
        deltidsprosentGjennomforing: Double? = null,
    ): Pair<Tiltaksdeltagelse, Søknadstiltak> {
        val tiltaksdeltagelse = Tiltaksdeltagelse(
            eksternDeltagelseId = eksternTiltaksdeltagelseId,
            gjennomføringId = eksternTiltaksgjennomføringsId,
            typeKode = typeKode,
            typeNavn = typeNavn,
            rettPåTiltakspenger = rettPåTiltakspenger,
            deltagelseFraOgMed = registerFraOgMed,
            deltagelseTilOgMed = registerTilOgMed,
            deltakelseStatus = status,
            deltakelseProsent = prosent,
            kilde = kilde,
            antallDagerPerUke = dagerPrUke,
            deltidsprosentGjennomforing = deltidsprosentGjennomforing,
        )
        return tiltaksdeltagelse to søknadstiltak(
            id = eksternTiltaksdeltagelseId,
            deltakelseFom = søknadFraOgMed,
            deltakelseTom = søknadTilOgMed,
            typeKode = typeKode.name,
            typeNavn = typeNavn,
        )
    }

    fun tiltaksdeltagelseMedArrangørnavn(
        eksternTiltaksdeltakelseId: String = UUID.randomUUID().toString(),
        typeKode: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        typeNavn: String = "Arbeidsmarkedsoppfølging gruppe",
        eksternTiltaksgjennomføringsId: String = UUID.randomUUID().toString(),
        arrangørnavn: String? = "Testarrangør med geolokaliserende navn",
        fom: LocalDate = 1.januar(2023),
        tom: LocalDate = 31.mars(2023),
        status: TiltakDeltakerstatus = Deltar,
        dagerPrUke: Float? = 5F,
        prosent: Float? = 100F,
        rettPåTiltakspenger: Boolean = true,
        kilde: Tiltakskilde = Komet,
        harAdressebeskyttelse: Boolean = false,
    ): TiltaksdeltakelseMedArrangørnavn {
        return TiltaksdeltakelseMedArrangørnavn(
            eksternDeltakelseId = eksternTiltaksdeltakelseId,
            gjennomføringId = eksternTiltaksgjennomføringsId,
            typeKode = typeKode,
            typeNavn = typeNavn,
            arrangørnavnFørSensur = arrangørnavn,
            rettPåTiltakspenger = rettPåTiltakspenger,
            deltakelseFraOgMed = fom,
            deltakelseTilOgMed = tom,
            deltakelseStatus = status,
            deltakelseProsent = prosent,
            kilde = kilde,
            antallDagerPerUke = dagerPrUke,
            harAdressebeskyttelse = harAdressebeskyttelse,
        )
    }
}

fun Søknadstiltak.toTiltak(
    eksternTiltaksgjennomføringsId: String = UUID.randomUUID().toString(),
): Tiltaksdeltagelse {
    val typeKode = TiltakResponsDTO.TiltakType.valueOf(this.typeKode)

    return tiltaksdeltagelse(
        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
        eksternTiltaksdeltagelseId = this.id,
        typeKode = typeKode.toTiltakstypeSomGirRett().getOrElse {
            throw IllegalArgumentException("Ugyldig typekode ${this.typeKode}")
        },
        typeNavn = this.typeNavn,
        fom = this.deltakelseFom,
        tom = this.deltakelseTom,
    )
}

fun Tiltaksdeltagelse.toSøknadstiltak(): Søknadstiltak {
    return søknadstiltak(
        id = this.eksternDeltagelseId,
        deltakelseFom = this.deltagelseFraOgMed!!,
        deltakelseTom = this.deltagelseTilOgMed!!,
        typeKode = this.typeKode.tilTiltakstype().name,
        typeNavn = this.typeNavn,
    )
}

fun TiltakstypeSomGirRett.tilTiltakstype(): TiltakResponsDTO.TiltakType {
    return when (this) {
        TiltakstypeSomGirRett.ARBEIDSFORBEREDENDE_TRENING -> TiltakResponsDTO.TiltakType.ARBFORB
        TiltakstypeSomGirRett.ARBEIDSRETTET_REHABILITERING -> TiltakResponsDTO.TiltakType.ARBRRHDAG
        TiltakstypeSomGirRett.ARBEIDSTRENING -> TiltakResponsDTO.TiltakType.ARBTREN
        TiltakstypeSomGirRett.AVKLARING -> TiltakResponsDTO.TiltakType.AVKLARAG
        TiltakstypeSomGirRett.DIGITAL_JOBBKLUBB -> TiltakResponsDTO.TiltakType.DIGIOPPARB
        TiltakstypeSomGirRett.ENKELTPLASS_AMO -> TiltakResponsDTO.TiltakType.ENKELAMO
        TiltakstypeSomGirRett.ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG -> TiltakResponsDTO.TiltakType.ENKFAGYRKE
        TiltakstypeSomGirRett.FORSØK_OPPLÆRING_LENGRE_VARIGHET -> TiltakResponsDTO.TiltakType.FORSOPPLEV
        TiltakstypeSomGirRett.GRUPPE_AMO -> TiltakResponsDTO.TiltakType.GRUPPEAMO
        TiltakstypeSomGirRett.GRUPPE_VGS_OG_HØYERE_YRKESFAG -> TiltakResponsDTO.TiltakType.GRUFAGYRKE
        TiltakstypeSomGirRett.HØYERE_UTDANNING -> TiltakResponsDTO.TiltakType.HOYEREUTD
        TiltakstypeSomGirRett.INDIVIDUELL_JOBBSTØTTE -> TiltakResponsDTO.TiltakType.INDJOBSTOT
        TiltakstypeSomGirRett.INDIVIDUELL_KARRIERESTØTTE_UNG -> TiltakResponsDTO.TiltakType.IPSUNG
        TiltakstypeSomGirRett.JOBBKLUBB -> TiltakResponsDTO.TiltakType.JOBBK
        TiltakstypeSomGirRett.OPPFØLGING -> TiltakResponsDTO.TiltakType.INDOPPFAG
        TiltakstypeSomGirRett.UTVIDET_OPPFØLGING_I_NAV -> TiltakResponsDTO.TiltakType.UTVAOONAV
        TiltakstypeSomGirRett.UTVIDET_OPPFØLGING_I_OPPLÆRING -> TiltakResponsDTO.TiltakType.UTVOPPFOPL
    }
}
