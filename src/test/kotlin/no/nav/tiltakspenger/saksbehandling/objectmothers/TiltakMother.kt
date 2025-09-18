package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.getOrElse
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.libs.tiltak.toTiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.søknad.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus.Deltar
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
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
        typeKode: TiltakstypeSomGirRett = TiltakstypeSomGirRett.JOBBKLUBB,
        typeNavn: String = "Testnavn",
        eksternTiltaksgjennomføringsId: String? = null,
        fom: LocalDate,
        tom: LocalDate,
        status: TiltakDeltakerstatus = Deltar,
        dagerPrUke: Float? = 5F,
        prosent: Float? = 100F,
        rettPåTiltakspenger: Boolean = true,
        kilde: Tiltakskilde = Tiltakskilde.Arena,
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
        )
        return tiltaksdeltagelse to søknadstiltak(
            id = eksternTiltaksdeltagelseId,
            deltakelseFom = søknadFraOgMed,
            deltakelseTom = søknadTilOgMed,
            typeKode = typeKode.name,
            typeNavn = typeNavn,
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
        // TODO jah: Vi burde ha kontroll på denne fra vi tar inn søknaden i routen til først søknad og deretter saksbehandling-api
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
        typeKode = this.typeKode.name,
        typeNavn = this.typeNavn,
    )
}
