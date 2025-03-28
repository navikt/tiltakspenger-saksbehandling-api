package no.nav.tiltakspenger.saksbehandling.routes.søknad

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.soknad.BarnetilleggDTO
import no.nav.tiltakspenger.libs.soknad.FraOgMedDatoSpmDTO
import no.nav.tiltakspenger.libs.soknad.JaNeiSpmDTO
import no.nav.tiltakspenger.libs.soknad.PeriodeSpmDTO
import no.nav.tiltakspenger.libs.soknad.SpmSvarDTO
import no.nav.tiltakspenger.libs.soknad.SøknadDTO
import no.nav.tiltakspenger.libs.soknad.SøknadsTiltakDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak
import java.time.LocalDateTime

object SøknadDTOMapper {
    fun mapSøknad(
        dto: SøknadDTO,
        innhentet: LocalDateTime,
        sak: Sak,
    ): Søknad =
        Søknad(
            id = SøknadId.fromString(dto.søknadId),
            versjon = dto.versjon,
            journalpostId = dto.journalpostId,
            personopplysninger =
            Søknad.Personopplysninger(
                fornavn = dto.personopplysninger.fornavn,
                etternavn = dto.personopplysninger.etternavn,
                fnr = Fnr.fromString(dto.personopplysninger.ident),
            ),
            tiltak = mapTiltak(dto.tiltak),
            barnetillegg =
            dto.barnetilleggPdl.map { mapBarnetilleggPDL(it) } +
                dto.barnetilleggManuelle.map {
                    mapBarnetilleggManuelle(
                        it,
                    )
                },
            opprettet = dto.opprettet,
            tidsstempelHosOss = innhentet,
            vedlegg = dto.vedlegg,
            kvp = mapPeriodeSpm(dto.kvp),
            intro = mapPeriodeSpm(dto.intro),
            institusjon = mapPeriodeSpm(dto.institusjon),
            etterlønn = mapJaNei(dto.etterlønn),
            gjenlevendepensjon = mapPeriodeSpm(dto.gjenlevendepensjon),
            alderspensjon = mapFraOgMedSpm(dto.alderspensjon),
            sykepenger = mapPeriodeSpm(dto.sykepenger),
            supplerendeStønadAlder = mapPeriodeSpm(dto.supplerendeStønadAlder),
            supplerendeStønadFlyktning = mapPeriodeSpm(dto.supplerendeStønadFlyktning),
            jobbsjansen = mapPeriodeSpm(dto.jobbsjansen),
            trygdOgPensjon = mapPeriodeSpm(dto.trygdOgPensjon),
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            oppgaveId = null,
            avbrutt = null,
        )

    private fun mapPeriodeSpm(periodeSpmDTO: PeriodeSpmDTO): Søknad.PeriodeSpm =
        when (periodeSpmDTO.svar) {
            SpmSvarDTO.Nei -> Søknad.PeriodeSpm.Nei
            SpmSvarDTO.Ja -> {
                checkNotNull(periodeSpmDTO.fom) { "Det skal ikke være mulig med null i fradato hvis man har svart JA " }
                checkNotNull(periodeSpmDTO.tom) { "Det skal ikke være mulig med null i tildato hvis man har svart JA " }
                Søknad.PeriodeSpm.Ja(
                    periode =
                    Periode(
                        fraOgMed = periodeSpmDTO.fom!!,
                        tilOgMed = periodeSpmDTO.tom!!,
                    ),
                )
            }
        }

    private fun mapFraOgMedSpm(fraOgMedDatoSpmDTO: FraOgMedDatoSpmDTO): Søknad.FraOgMedDatoSpm {
        return when (fraOgMedDatoSpmDTO.svar) {
            SpmSvarDTO.Nei -> Søknad.FraOgMedDatoSpm.Nei
            SpmSvarDTO.Ja -> {
                requireNotNull(fraOgMedDatoSpmDTO.fom) { "Det skal ikke være mulig med null i fradato hvis man har svart JA" }
                Søknad.FraOgMedDatoSpm.Ja(
                    fra = fraOgMedDatoSpmDTO.fom!!,
                )
            }
        }
    }

    private fun mapTiltak(dto: SøknadsTiltakDTO): Søknadstiltak =
        Søknadstiltak(
            id = dto.id,
            deltakelseFom = dto.deltakelseFom,
            deltakelseTom = dto.deltakelseTom,
            typeKode = dto.typeKode,
            typeNavn = dto.typeNavn,
        )

    private fun mapBarnetilleggManuelle(dto: BarnetilleggDTO): BarnetilleggFraSøknad.Manuell {
        checkNotNull(dto.fornavn) { "Fornavn kan ikke være null for barnetillegg, manuelle barn " }
        checkNotNull(dto.etternavn) { "Etternavn kan ikke være null for barnetillegg, manuelle barn " }
        checkNotNull(dto.fødselsdato) { "Fødselsdato kan ikke være null for barnetillegg, manuelle barn " }

        return BarnetilleggFraSøknad.Manuell(
            oppholderSegIEØS = mapJaNei(dto.oppholderSegIEØS),
            fornavn = dto.fornavn!!,
            mellomnavn = dto.mellomnavn,
            etternavn = dto.etternavn!!,
            fødselsdato = dto.fødselsdato!!,
        )
    }

    private fun mapBarnetilleggPDL(dto: BarnetilleggDTO): BarnetilleggFraSøknad.FraPdl {
        checkNotNull(dto.fødselsdato) { "Fødselsdato kan ikke være null for barnetillegg fra PDL" }
        return BarnetilleggFraSøknad.FraPdl(
            oppholderSegIEØS = mapJaNei(dto.oppholderSegIEØS),
            fornavn = dto.fornavn,
            mellomnavn = dto.mellomnavn,
            etternavn = dto.etternavn,
            fødselsdato = dto.fødselsdato!!,
        )
    }

    private fun mapJaNei(jaNeiSpmDTO: JaNeiSpmDTO): Søknad.JaNeiSpm =
        when (jaNeiSpmDTO.svar) {
            SpmSvarDTO.Nei -> Søknad.JaNeiSpm.Nei
            SpmSvarDTO.Ja -> Søknad.JaNeiSpm.Ja
        }
}
