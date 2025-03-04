package no.nav.tiltakspenger.vedtak.routes.søknad

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
import no.nav.tiltakspenger.saksbehandling.domene.behandling.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
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
            tiltak = no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapTiltak(dto.tiltak),
            barnetillegg =
            dto.barnetilleggPdl.map { no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapBarnetilleggPDL(it) } +
                dto.barnetilleggManuelle.map {
                    no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapBarnetilleggManuelle(
                        it,
                    )
                },
            opprettet = dto.opprettet,
            tidsstempelHosOss = innhentet,
            vedlegg = dto.vedlegg,
            kvp = no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapPeriodeSpm(dto.kvp),
            intro = no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapPeriodeSpm(dto.intro),
            institusjon = no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapPeriodeSpm(dto.institusjon),
            etterlønn = no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapJaNei(dto.etterlønn),
            gjenlevendepensjon = no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapPeriodeSpm(dto.gjenlevendepensjon),
            alderspensjon = no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapFraOgMedSpm(dto.alderspensjon),
            sykepenger = no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapPeriodeSpm(dto.sykepenger),
            supplerendeStønadAlder = no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapPeriodeSpm(dto.supplerendeStønadAlder),
            supplerendeStønadFlyktning = no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapPeriodeSpm(dto.supplerendeStønadFlyktning),
            jobbsjansen = no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapPeriodeSpm(dto.jobbsjansen),
            trygdOgPensjon = no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapPeriodeSpm(dto.trygdOgPensjon),
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
            oppholderSegIEØS = no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapJaNei(dto.oppholderSegIEØS),
            fornavn = dto.fornavn!!,
            mellomnavn = dto.mellomnavn,
            etternavn = dto.etternavn!!,
            fødselsdato = dto.fødselsdato!!,
        )
    }

    private fun mapBarnetilleggPDL(dto: BarnetilleggDTO): BarnetilleggFraSøknad.FraPdl {
        checkNotNull(dto.fødselsdato) { "Fødselsdato kan ikke være null for barnetillegg fra PDL" }
        return BarnetilleggFraSøknad.FraPdl(
            oppholderSegIEØS = no.nav.tiltakspenger.vedtak.routes.søknad.SøknadDTOMapper.mapJaNei(dto.oppholderSegIEØS),
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
