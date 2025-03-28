package no.nav.tiltakspenger.saksbehandling

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.soknad.BarnetilleggDTO
import no.nav.tiltakspenger.libs.soknad.FraOgMedDatoSpmDTO
import no.nav.tiltakspenger.libs.soknad.JaNeiSpmDTO
import no.nav.tiltakspenger.libs.soknad.PeriodeSpmDTO
import no.nav.tiltakspenger.libs.soknad.PersonopplysningerDTO
import no.nav.tiltakspenger.libs.soknad.SpmSvarDTO
import no.nav.tiltakspenger.libs.soknad.SøknadDTO
import no.nav.tiltakspenger.libs.soknad.SøknadsTiltakDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.SøknadDTOMapper
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import kotlin.test.assertEquals

internal class SøknadDTOMapperTest {
    @Test
    fun mapBasisFelter() {
        val sak = ObjectMother.nySak()
        val søknadDTO = søknadDTO(saksnummer = sak.saksnummer)
        val søknad = SøknadDTOMapper.mapSøknad(søknadDTO, LocalDateTime.MIN, sak)

        assertEquals(søknadDTO.søknadId, søknad.id.toString())
        assertEquals(søknadDTO.journalpostId, søknad.journalpostId)
        assertEquals(søknadDTO.personopplysninger.fornavn, søknad.personopplysninger.fornavn)
        assertEquals(søknadDTO.personopplysninger.etternavn, søknad.personopplysninger.etternavn)
        assertEquals(søknadDTO.personopplysninger.ident, søknad.fnr.verdi)
        assertEquals(søknadDTO.opprettet, søknad.opprettet)
        assertEquals(søknadDTO.vedlegg, søknad.vedlegg)

        assertEquals(søknad.kvp, Søknad.PeriodeSpm.Nei)
        assertEquals(søknad.intro, Søknad.PeriodeSpm.Nei)
        assertEquals(søknad.institusjon, Søknad.PeriodeSpm.Nei)
        assertEquals(søknad.jobbsjansen, Søknad.PeriodeSpm.Nei)
        assertEquals(søknad.supplerendeStønadAlder, Søknad.PeriodeSpm.Nei)
        assertEquals(søknad.supplerendeStønadFlyktning, Søknad.PeriodeSpm.Nei)
        assertEquals(søknad.sykepenger, Søknad.PeriodeSpm.Nei)
        assertEquals(søknad.alderspensjon, Søknad.FraOgMedDatoSpm.Nei)
        assertEquals(søknad.gjenlevendepensjon, Søknad.PeriodeSpm.Nei)
        assertEquals(søknad.trygdOgPensjon, Søknad.PeriodeSpm.Nei)
        assertEquals(søknad.etterlønn, Søknad.JaNeiSpm.Nei)
        assertEquals(søknad.sakId, sak.id)
        assertEquals(søknad.saksnummer, sak.saksnummer)
    }

    @Test
    fun `ja i alt`() {
        val fra = LocalDate.of(2023, 1, 1)
        val til = LocalDate.of(2023, 12, 31)
        val sak = ObjectMother.nySak()
        val søknadDTO =
            søknadDTO(
                kvp = PeriodeSpmDTO(svar = SpmSvarDTO.Ja, fom = fra, tom = til),
                intro = PeriodeSpmDTO(svar = SpmSvarDTO.Ja, fom = fra, tom = til),
                institusjon = PeriodeSpmDTO(svar = SpmSvarDTO.Ja, fom = fra, tom = til),
                jobbsjansen = PeriodeSpmDTO(svar = SpmSvarDTO.Ja, fom = fra, tom = til),
                supplerendeAlder = PeriodeSpmDTO(svar = SpmSvarDTO.Ja, fom = fra, tom = til),
                supplerendeFlykting = PeriodeSpmDTO(svar = SpmSvarDTO.Ja, fom = fra, tom = til),
                sykepenger = PeriodeSpmDTO(svar = SpmSvarDTO.Ja, fom = fra, tom = til),
                alderspensjon = FraOgMedDatoSpmDTO(svar = SpmSvarDTO.Ja, fom = fra),
                gjenlevendePensjon = PeriodeSpmDTO(svar = SpmSvarDTO.Ja, fom = fra, tom = til),
                trygdOgPensjon = PeriodeSpmDTO(svar = SpmSvarDTO.Ja, fom = fra, tom = til),
                etterlønn = JaNeiSpmDTO(SpmSvarDTO.Ja),
                saksnummer = sak.saksnummer,
            )
        val søknad = SøknadDTOMapper.mapSøknad(søknadDTO, LocalDateTime.MIN, sak)

        assertEquals(søknad.kvp, Søknad.PeriodeSpm.Ja(Periode(fraOgMed = fra, tilOgMed = til)))
        assertEquals(søknad.intro, Søknad.PeriodeSpm.Ja(Periode(fraOgMed = fra, tilOgMed = til)))
        assertEquals(søknad.institusjon, Søknad.PeriodeSpm.Ja(Periode(fraOgMed = fra, tilOgMed = til)))
        assertEquals(søknad.jobbsjansen, Søknad.PeriodeSpm.Ja(Periode(fraOgMed = fra, tilOgMed = til)))
        assertEquals(søknad.supplerendeStønadAlder, Søknad.PeriodeSpm.Ja(Periode(fraOgMed = fra, tilOgMed = til)))
        assertEquals(søknad.supplerendeStønadFlyktning, Søknad.PeriodeSpm.Ja(Periode(fraOgMed = fra, tilOgMed = til)))
        assertEquals(søknad.sykepenger, Søknad.PeriodeSpm.Ja(Periode(fraOgMed = fra, tilOgMed = til)))
        assertEquals(søknad.alderspensjon, Søknad.FraOgMedDatoSpm.Ja(fra = fra))
        assertEquals(søknad.gjenlevendepensjon, Søknad.PeriodeSpm.Ja(Periode(fraOgMed = fra, tilOgMed = til)))
        assertEquals(søknad.trygdOgPensjon, Søknad.PeriodeSpm.Ja(Periode(fraOgMed = fra, tilOgMed = til)))
        assertEquals(søknad.etterlønn, Søknad.JaNeiSpm.Ja)
        assertEquals(søknad.sakId, sak.id)
        assertEquals(søknad.saksnummer, sak.saksnummer)
    }

    private fun søknadDTO(
        fra: LocalDate = LocalDate.of(2023, 1, 1),
        til: LocalDate = LocalDate.of(2023, 12, 31),
        versjon: String = "1",
        søknadId: String = Søknad.randomId().toString(),
        journalpostId: String = "43",
        personopplysninger: PersonopplysningerDTO =
            PersonopplysningerDTO(
                fornavn = "Ola",
                etternavn = "Nordmann",
                ident = Fnr.random().verdi,
            ),
        kvp: PeriodeSpmDTO = PeriodeSpmDTO(svar = SpmSvarDTO.Nei, fom = null, tom = null),
        intro: PeriodeSpmDTO = PeriodeSpmDTO(svar = SpmSvarDTO.Nei, fom = null, tom = null),
        institusjon: PeriodeSpmDTO = PeriodeSpmDTO(svar = SpmSvarDTO.Nei, fom = null, tom = null),
        barnetilleggPdl: List<BarnetilleggDTO> = emptyList(),
        barnetilleggManuelle: List<BarnetilleggDTO> = emptyList(),
        opprettet: LocalDateTime = LocalDateTime.of(2022, Month.SEPTEMBER, 13, 15, 0),
        tiltak: SøknadsTiltakDTO =
            SøknadsTiltakDTO(
                id = "arenaId",
                arrangør = "Arrangørnavn",
                typeKode = "AMO",
                typeNavn = "AMO",
                deltakelseFom = fra,
                deltakelseTom = til,
            ),
        alderspensjon: FraOgMedDatoSpmDTO = FraOgMedDatoSpmDTO(svar = SpmSvarDTO.Nei, fom = null),
        etterlønn: JaNeiSpmDTO = JaNeiSpmDTO(SpmSvarDTO.Nei),
        gjenlevendePensjon: PeriodeSpmDTO = PeriodeSpmDTO(svar = SpmSvarDTO.Nei, fom = null, tom = null),
        jobbsjansen: PeriodeSpmDTO = PeriodeSpmDTO(svar = SpmSvarDTO.Nei, fom = null, tom = null),
        supplerendeAlder: PeriodeSpmDTO = PeriodeSpmDTO(svar = SpmSvarDTO.Nei, fom = null, tom = null),
        supplerendeFlykting: PeriodeSpmDTO = PeriodeSpmDTO(svar = SpmSvarDTO.Nei, fom = null, tom = null),
        sykepenger: PeriodeSpmDTO = PeriodeSpmDTO(svar = SpmSvarDTO.Nei, fom = null, tom = null),
        trygdOgPensjon: PeriodeSpmDTO = PeriodeSpmDTO(svar = SpmSvarDTO.Nei, fom = null, tom = null),
        vedlegg: Int = 0,
        saksnummer: Saksnummer,
    ) = SøknadDTO(
        versjon = versjon,
        søknadId = søknadId,
        journalpostId = journalpostId,
        personopplysninger = personopplysninger,
        kvp = kvp,
        intro = intro,
        institusjon = institusjon,
        barnetilleggPdl = barnetilleggPdl,
        barnetilleggManuelle = barnetilleggManuelle,
        tiltak = tiltak,
        alderspensjon = alderspensjon,
        etterlønn = etterlønn,
        gjenlevendepensjon = gjenlevendePensjon,
        jobbsjansen = jobbsjansen,
        supplerendeStønadAlder = supplerendeAlder,
        supplerendeStønadFlyktning = supplerendeFlykting,
        sykepenger = sykepenger,
        trygdOgPensjon = trygdOgPensjon,
        opprettet = opprettet,
        vedlegg = vedlegg,
        saksnummer = saksnummer.verdi,
    )
}
