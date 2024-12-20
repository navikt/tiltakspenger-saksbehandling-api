package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.felles.TiltakId
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.mars
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.objectmothers.ObjectMother.tiltak
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltak
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.Deltar
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltakskilde.Komet
import java.time.LocalDate
import java.util.UUID

interface TiltakMother {

    fun tiltak(
        id: TiltakId = TiltakId.random(),
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
    ): Tiltak {
        return Tiltak(
            id = id,
            eksternDeltagelseId = eksternTiltaksdeltagelseId,
            gjennomføringId = eksternTiltaksgjennomføringsId,
            typeKode = typeKode,
            typeNavn = typeNavn,
            rettPåTiltakspenger = rettPåTiltakspenger,
            deltakelsesperiode = Periode(fom, tom),
            deltakelseStatus = status,
            deltakelseProsent = prosent,
            kilde = kilde,
            antallDagerPerUke = dagerPrUke,
        )
    }

    fun tiltakOgSøknadstiltak(
        id: TiltakId = TiltakId.random(),
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
    ): Pair<Tiltak, Søknadstiltak> {
        return Tiltak(
            id = id,
            eksternDeltagelseId = eksternTiltaksdeltagelseId,
            gjennomføringId = eksternTiltaksgjennomføringsId,
            typeKode = typeKode,
            typeNavn = typeNavn,
            rettPåTiltakspenger = rettPåTiltakspenger,
            deltakelsesperiode = Periode(fom, tom),
            deltakelseStatus = status,
            deltakelseProsent = prosent,
            kilde = kilde,
            antallDagerPerUke = dagerPrUke,
        ) to søknadstiltak(
            id = eksternTiltaksdeltagelseId,
            deltakelseFom = fom,
            deltakelseTom = tom,
            typeKode = typeKode.name,
            typeNavn = typeNavn,
        )
    }
}

fun Søknadstiltak.toTiltak(
    id: TiltakId = TiltakId.random(),
    eksternTiltaksgjennomføringsId: String = UUID.randomUUID().toString(),
): Tiltak {
    return tiltak(
        id = id,
        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
        eksternTiltaksdeltagelseId = this.id,
        // TODO jah: Vi burde ha kontroll på denne fra vi tar inn søknaden i routen til først søknad og deretter saksbehandling-api
        typeKode = when (this.typeKode) {
            "JOBBK" -> TiltakstypeSomGirRett.JOBBKLUBB
            "GRUPPEAMO" -> TiltakstypeSomGirRett.GRUPPE_AMO
            "INDOPPFAG" -> TiltakstypeSomGirRett.OPPFØLGING
            "ARBTREN" -> TiltakstypeSomGirRett.ARBEIDSTRENING
            else -> throw IllegalArgumentException("Ukjent typekode ${this.typeKode}")
        },
        typeNavn = this.typeNavn,
        fom = this.deltakelseFom,
        tom = this.deltakelseTom,
    )
}

fun Tiltak.toSøknadstiltak(): Søknadstiltak {
    return søknadstiltak(
        id = this.eksternDeltagelseId,
        deltakelseFom = this.deltakelsesperiode.fraOgMed,
        deltakelseTom = this.deltakelsesperiode.tilOgMed,
        typeKode = this.typeKode.name,
        typeNavn = this.typeNavn,
    )
}
