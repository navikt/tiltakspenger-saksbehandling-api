package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.common.januarDateTime
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.Søknadstiltak
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface SøknadMother {
    fun søknadstiltak(
        id: String = UUID.randomUUID().toString(),
        deltakelseFom: LocalDate = 1.januar(2022),
        deltakelseTom: LocalDate = 31.januar(2022),
        typeKode: String = "JOBBK",
        typeNavn: String = "Jobbklubb",
    ): Søknadstiltak =
        Søknadstiltak(
            id = id,
            deltakelseFom = deltakelseFom,
            deltakelseTom = deltakelseTom,
            typeKode = typeKode,
            typeNavn = typeNavn,
        )

    fun barnetilleggMedIdent(
        oppholderSegIEØS: Søknad.JaNeiSpm = Søknad.JaNeiSpm.Ja,
        fornavn: String = "Fornavn Barn",
        mellomnavn: String? = "Mellomnavn Barn",
        etternavn: String = "Etternavn Barn",
        fødselsdato: LocalDate = 14.juni(2012),
        søktBarnetillegg: Boolean = true,
    ): BarnetilleggFraSøknad =
        BarnetilleggFraSøknad.FraPdl(
            oppholderSegIEØS = oppholderSegIEØS,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            fødselsdato = fødselsdato,
        )

    fun barnetilleggUtenIdent(
        oppholderSegIEØS: Søknad.JaNeiSpm = Søknad.JaNeiSpm.Ja,
        fornavn: String = "Fornavn Barn",
        mellomnavn: String? = "Mellomnavn Barn",
        etternavn: String = "Etternavn Barn",
        fødselsdato: LocalDate = 14.juni(2012),
    ): BarnetilleggFraSøknad =
        BarnetilleggFraSøknad.Manuell(
            oppholderSegIEØS = oppholderSegIEØS,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            fødselsdato = fødselsdato,
        )

    fun personopplysningFødselsdato() = 1.januar(2000)

    fun nySøknad(
        periode: Periode = ObjectMother.virkningsperiode(),
        versjon: String = "1",
        id: SøknadId = Søknad.randomId(),
        journalpostId: String = "journalpostId",
        dokumentInfoId: String = "dokumentInfoId",
        filnavn: String = "filnavn",
        fnr: Fnr = Fnr.random(),
        personopplysninger: Søknad.Personopplysninger = personSøknad(fnr = fnr),
        kvp: Søknad.PeriodeSpm = periodeNei(),
        intro: Søknad.PeriodeSpm = periodeNei(),
        institusjon: Søknad.PeriodeSpm = periodeNei(),
        opprettet: LocalDateTime = 1.januarDateTime(2022),
        barnetillegg: List<BarnetilleggFraSøknad> = listOf(),
        tidsstempelHosOss: LocalDateTime = 1.januarDateTime(2022),
        søknadstiltak: Søknadstiltak = søknadstiltak(deltakelseFom = periode.fraOgMed, deltakelseTom = periode.tilOgMed),
        trygdOgPensjon: Søknad.PeriodeSpm = periodeNei(),
        vedlegg: Int = 0,
        etterlønn: Søknad.JaNeiSpm = nei(),
        gjenlevendepensjon: Søknad.PeriodeSpm = periodeNei(),
        alderspensjon: Søknad.FraOgMedDatoSpm = fraOgMedDatoNei(),
        sykepenger: Søknad.PeriodeSpm = periodeNei(),
        supplerendeStønadAlder: Søknad.PeriodeSpm = periodeNei(),
        supplerendeStønadFlyktning: Søknad.PeriodeSpm = periodeNei(),
        jobbsjansen: Søknad.PeriodeSpm = periodeNei(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        avbrutt: Avbrutt? = null,
    ): Søknad =
        Søknad(
            versjon = versjon,
            id = id,
            journalpostId = journalpostId,
            personopplysninger = personopplysninger,
            tiltak = søknadstiltak,
            barnetillegg = barnetillegg,
            opprettet = opprettet,
            tidsstempelHosOss = tidsstempelHosOss,
            vedlegg = vedlegg,
            kvp = kvp,
            intro = intro,
            institusjon = institusjon,
            etterlønn = etterlønn,
            gjenlevendepensjon = gjenlevendepensjon,
            alderspensjon = alderspensjon,
            sykepenger = sykepenger,
            supplerendeStønadAlder = supplerendeStønadAlder,
            supplerendeStønadFlyktning = supplerendeStønadFlyktning,
            jobbsjansen = jobbsjansen,
            trygdOgPensjon = trygdOgPensjon,
            sakId = sakId,
            saksnummer = saksnummer,
            avbrutt = avbrutt,
        )

    fun personSøknad(
        fornavn: String = "Fornavn",
        etternavn: String = "Etternavn",
        fnr: Fnr = Fnr.random(),
    ) = Søknad.Personopplysninger(
        fornavn = fornavn,
        etternavn = etternavn,
        fnr = fnr,
    )

    fun nei() = Søknad.JaNeiSpm.Nei

    fun fraOgMedDatoNei() = Søknad.FraOgMedDatoSpm.Nei

    fun periodeNei() = Søknad.PeriodeSpm.Nei

    fun ja() = Søknad.JaNeiSpm.Ja

    fun fraOgMedDatoJa(fom: LocalDate = 1.januar(2022)) =
        Søknad.FraOgMedDatoSpm.Ja(
            fra = fom,
        )

    fun periodeJa(
        fom: LocalDate = 1.januar(2022),
        tom: LocalDate = 31.januar(2022),
    ) = Søknad.PeriodeSpm.Ja(
        periode =
        Periode(
            fraOgMed = fom,
            tilOgMed = tom,
        ),
    )
}
