package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.getOrElse
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.libs.tiltak.toTiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.sak.SøknadstiltakIdGenerator
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.tilTiltakstype
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.Deltar
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde.Komet
import java.time.LocalDate
import java.util.UUID

interface TiltakMother {

    /**
     * Dupliserer dene med [tiltaksdeltakelse] for å være bakoverkompabilitet med tester som bruker tac
     */
    fun tiltaksdeltakelseTac(
        // Unik per kall; `tiltaksdeltaker.ekstern_id` har unik indeks i basen.
        eksternTiltaksdeltakelseId: String = UUID.randomUUID().toString(),
        typeKode: TiltakstypeSomGirRettDTO = TiltakstypeSomGirRettDTO.GRUPPE_AMO,
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
        internDeltakelseId: TiltaksdeltakerId = TiltaksdeltakerId.random(),
    ): Tiltaksdeltakelse {
        return Tiltaksdeltakelse(
            eksternDeltakelseId = eksternTiltaksdeltakelseId,
            gjennomføringId = eksternTiltaksgjennomføringsId,
            typeKode = typeKode,
            typeNavn = typeNavn,
            rettPåTiltakspenger = rettPåTiltakspenger,
            deltakelseFraOgMed = fom,
            deltakelseTilOgMed = tom,
            deltakelseStatus = status,
            deltakelseProsent = prosent,
            kilde = kilde,
            antallDagerPerUke = dagerPrUke,
            deltidsprosentGjennomforing = deltidsprosentGjennomforing,
            internDeltakelseId = internDeltakelseId,
        )
    }

    fun SøknadstiltakIdGenerator.tiltaksdeltakelse(
        periode: Periode = 1.januar(2023) til 31.mars(2023),
        eksternTiltaksdeltakelseId: String = this.generer(),
        typeKode: TiltakstypeSomGirRettDTO = TiltakstypeSomGirRettDTO.GRUPPE_AMO,
        typeNavn: String = "Arbeidsmarkedsoppfølging gruppe",
        // Denne er ikke unik i databasen og brukes ikke i domenelogikken, så den kan være statisk på tvers av alle deltakelser.
        eksternTiltaksgjennomføringsId: String = "358f6fe9-ebbe-4f7d-820f-2c0f04055c23",
        fom: LocalDate = periode.fraOgMed,
        tom: LocalDate = periode.tilOgMed,
        status: TiltakDeltakerstatus = Deltar,
        dagerPrUke: Float? = 5F,
        prosent: Float? = 100F,
        rettPåTiltakspenger: Boolean = true,
        kilde: Tiltakskilde = Komet,
        deltidsprosentGjennomforing: Double? = null,
        internDeltakelseId: TiltaksdeltakerId = TiltaksdeltakerId.random(),
    ): Tiltaksdeltakelse {
        return Tiltaksdeltakelse(
            eksternDeltakelseId = eksternTiltaksdeltakelseId,
            gjennomføringId = eksternTiltaksgjennomføringsId,
            typeKode = typeKode,
            typeNavn = typeNavn,
            rettPåTiltakspenger = rettPåTiltakspenger,
            deltakelseFraOgMed = fom,
            deltakelseTilOgMed = tom,
            deltakelseStatus = status,
            deltakelseProsent = prosent,
            kilde = kilde,
            antallDagerPerUke = dagerPrUke,
            deltidsprosentGjennomforing = deltidsprosentGjennomforing,
            internDeltakelseId = internDeltakelseId,
        )
    }

    fun tiltaksdeltakelse(
        periode: Periode = 1.januar(2023) til 31.mars(2023),
        // Unik per kall; `tiltaksdeltaker.ekstern_id` har unik indeks i basen.
        // Skal to objekter representere samme deltakelse, må id-en sendes inn eksplisitt begge steder.
        eksternTiltaksdeltakelseId: String = UUID.randomUUID().toString(),
        typeKode: TiltakstypeSomGirRettDTO = TiltakstypeSomGirRettDTO.GRUPPE_AMO,
        typeNavn: String = "Arbeidsmarkedsoppfølging gruppe",
        // Denne er ikke unik i databasen og brukes ikke i domenelogikken, så den kan være statisk på tvers av alle deltakelser.
        eksternTiltaksgjennomføringsId: String = "358f6fe9-ebbe-4f7d-820f-2c0f04055c23",
        fom: LocalDate = periode.fraOgMed,
        tom: LocalDate = periode.tilOgMed,
        status: TiltakDeltakerstatus = Deltar,
        dagerPrUke: Float? = 5F,
        prosent: Float? = 100F,
        rettPåTiltakspenger: Boolean = true,
        kilde: Tiltakskilde = Komet,
        deltidsprosentGjennomforing: Double? = null,
        internDeltakelseId: TiltaksdeltakerId = TiltaksdeltakerId.random(),
    ): Tiltaksdeltakelse {
        return Tiltaksdeltakelse(
            eksternDeltakelseId = eksternTiltaksdeltakelseId,
            gjennomføringId = eksternTiltaksgjennomføringsId,
            typeKode = typeKode,
            typeNavn = typeNavn,
            rettPåTiltakspenger = rettPåTiltakspenger,
            deltakelseFraOgMed = fom,
            deltakelseTilOgMed = tom,
            deltakelseStatus = status,
            deltakelseProsent = prosent,
            kilde = kilde,
            antallDagerPerUke = dagerPrUke,
            deltidsprosentGjennomforing = deltidsprosentGjennomforing,
            internDeltakelseId = internDeltakelseId,
        )
    }

    fun tiltakOgSøknadstiltak(
        eksternTiltaksdeltakelseId: String = UUID.randomUUID().toString(),
        typeKode: TiltakstypeSomGirRettDTO = TiltakstypeSomGirRettDTO.GRUPPE_AMO,
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
        tiltaksdeltakerId: TiltaksdeltakerId = TiltaksdeltakerId.random(),
    ): Pair<Tiltaksdeltakelse, Søknadstiltak> {
        val tiltaksdeltakelse = Tiltaksdeltakelse(
            eksternDeltakelseId = eksternTiltaksdeltakelseId,
            gjennomføringId = eksternTiltaksgjennomføringsId,
            typeKode = typeKode,
            typeNavn = typeNavn,
            rettPåTiltakspenger = rettPåTiltakspenger,
            deltakelseFraOgMed = registerFraOgMed,
            deltakelseTilOgMed = registerTilOgMed,
            deltakelseStatus = status,
            deltakelseProsent = prosent,
            kilde = kilde,
            antallDagerPerUke = dagerPrUke,
            deltidsprosentGjennomforing = deltidsprosentGjennomforing,
            internDeltakelseId = tiltaksdeltakerId,
        )
        return tiltaksdeltakelse to søknadstiltak(
            id = eksternTiltaksdeltakelseId,
            deltakelseFom = søknadFraOgMed,
            deltakelseTom = søknadTilOgMed,
            typeKode = typeKode.tilTiltakstype(),
            typeNavn = typeNavn,
            tiltaksdeltakerId = tiltaksdeltakerId,
        )
    }

    fun tiltaksdeltakelseMedArrangørnavn(
        eksternTiltaksdeltakelseId: String = UUID.randomUUID().toString(),
        typeKode: TiltakstypeSomGirRettDTO = TiltakstypeSomGirRettDTO.GRUPPE_AMO,
        typeNavn: String = "Arbeidsmarkedsoppfølging gruppe",
        arrangørnavn: String? = "Testarrangør med geolokaliserende navn",
        fom: LocalDate? = 1.januar(2023),
        tom: LocalDate? = 31.mars(2023),
        harAdressebeskyttelse: Boolean = false,
        visningsnavn: String = if (harAdressebeskyttelse) typeNavn else "$typeNavn hos $arrangørnavn",
    ): TiltaksdeltakelseMedArrangørnavn {
        return TiltaksdeltakelseMedArrangørnavn(
            eksternDeltakelseId = eksternTiltaksdeltakelseId,
            typeKode = typeKode,
            typeNavn = typeNavn,
            deltakelseFraOgMed = fom,
            deltakelseTilOgMed = tom,
            visningsnavn = visningsnavn,
        )
    }
}

fun Søknadstiltak.toTiltak(
    eksternTiltaksgjennomføringsId: String = UUID.randomUUID().toString(),
): Tiltaksdeltakelse {
    return tiltaksdeltakelse(
        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
        eksternTiltaksdeltakelseId = this.id,
        typeKode = this.typeKode.toTiltakstypeSomGirRett().getOrElse {
            throw IllegalArgumentException("Ugyldig typekode ${this.typeKode}")
        },
        typeNavn = this.typeNavn,
        fom = this.deltakelseFom,
        tom = this.deltakelseTom,
        internDeltakelseId = this.tiltaksdeltakerId,
    )
}

fun Tiltaksdeltakelse.toSøknadstiltak(tiltaksdeltakerId: TiltaksdeltakerId = TiltaksdeltakerId.random()): Søknadstiltak {
    return søknadstiltak(
        id = this.eksternDeltakelseId,
        deltakelseFom = this.deltakelseFraOgMed!!,
        deltakelseTom = this.deltakelseTilOgMed!!,
        typeKode = this.typeKode.tilTiltakstype(),
        typeNavn = this.typeNavn,
        tiltaksdeltakerId = tiltaksdeltakerId,
    )
}
