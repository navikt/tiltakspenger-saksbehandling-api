package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.getOrElse
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.libs.tiltak.toTiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.tilTiltakstype
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.Deltar
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde.Komet
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
    ): Tiltaksdeltakelse {
        return Tiltaksdeltakelse(
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
        // Det er litt vanskelig å konstant kontrollere tiltakelses-id'en fra høyere nivåer. Så vi benytter en enkel statisk id her.
        eksternTiltaksdeltagelseId: String = "61328250-7d5d-4961-b70e-5cb727a34371",
        typeKode: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        typeNavn: String = "Arbeidsmarkedsoppfølging gruppe",
        // Det er litt vanskelig å konstant kontrollere tiltakelses-id'en fra høyere nivåer. Så vi benytter en enkel statisk id her.
        eksternTiltaksgjennomføringsId: String = "358f6fe9-ebbe-4f7d-820f-2c0f04055c23",
        fom: LocalDate = 1.januar(2023),
        tom: LocalDate = 31.mars(2023),
        status: TiltakDeltakerstatus = Deltar,
        dagerPrUke: Float? = 5F,
        prosent: Float? = 100F,
        rettPåTiltakspenger: Boolean = true,
        kilde: Tiltakskilde = Komet,
        deltidsprosentGjennomforing: Double? = null,
    ): Tiltaksdeltakelse {
        return Tiltaksdeltakelse(
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
    ): Pair<Tiltaksdeltakelse, Søknadstiltak> {
        val tiltaksdeltakelse = Tiltaksdeltakelse(
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
        return tiltaksdeltakelse to søknadstiltak(
            id = eksternTiltaksdeltagelseId,
            deltakelseFom = søknadFraOgMed,
            deltakelseTom = søknadTilOgMed,
            typeKode = typeKode.tilTiltakstype(),
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
): Tiltaksdeltakelse {
    return tiltaksdeltagelse(
        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
        eksternTiltaksdeltagelseId = this.id,
        typeKode = this.typeKode.toTiltakstypeSomGirRett().getOrElse {
            throw IllegalArgumentException("Ugyldig typekode ${this.typeKode}")
        },
        typeNavn = this.typeNavn,
        fom = this.deltakelseFom,
        tom = this.deltakelseTom,
    )
}

fun Tiltaksdeltakelse.toSøknadstiltak(): Søknadstiltak {
    return søknadstiltak(
        id = this.eksternDeltagelseId,
        deltakelseFom = this.deltagelseFraOgMed!!,
        deltakelseTom = this.deltagelseTilOgMed!!,
        typeKode = this.typeKode.tilTiltakstype(),
        typeNavn = this.typeNavn,
    )
}
