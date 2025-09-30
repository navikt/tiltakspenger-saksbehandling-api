package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import java.time.LocalDate
import java.time.LocalDateTime

data class PapirsøknadBody(
    val journalpostId: String,
    val personopplysninger: PersonopplysningerDTO,
    val manueltSattSøknadsperiode: PeriodeDTO?,
    val tiltak: SøknadsTiltakDTO,
    val kravtidspunkt: LocalDateTime,
    val antallVedlegg: Int,
    val barnetilleggPdl: List<BarnetilleggDTO>,
    val barnetilleggManuelle: List<BarnetilleggDTO>,
    val kvp: PeriodeSpmDTO?,
    val intro: PeriodeSpmDTO?,
    val institusjon: PeriodeSpmDTO?,
    val etterlønn: JaNeiSpmDTO?,
    val gjenlevendepensjon: PeriodeSpmDTO?,
    val alderspensjon: FraOgMedDatoSpmDTO?,
    val sykepenger: PeriodeSpmDTO?,
    val supplerendeStønadAlder: PeriodeSpmDTO?,
    val supplerendeStønadFlyktning: PeriodeSpmDTO?,
    val jobbsjansen: PeriodeSpmDTO?,
    val trygdOgPensjon: PeriodeSpmDTO?,
) {
    fun tilKommando(): StartBehandlingAvPapirsøknadCommand {
        return StartBehandlingAvPapirsøknadCommand(
            personopplysninger = personopplysninger.tilDomene(),
            journalpostId = JournalpostId(journalpostId),
            manueltSattSøknadsperiode = manueltSattSøknadsperiode?.toDomain(),
            søknadstiltak = tiltak.tilDomene(),
            kravtidspunkt = kravtidspunkt,
            barnetillegg = barnetilleggPdl.map { it.tilDomenePdl() } +
                barnetilleggManuelle.map { it.tilDomeneManuell() },
            antallVedlegg = antallVedlegg,
            kvp = this.kvp?.tilDomene(),
            intro = this.intro?.tilDomene(),
            institusjon = this.institusjon?.tilDomene(),
            etterlønn = this.etterlønn?.tilDomene(),
            gjenlevendepensjon = this.gjenlevendepensjon?.tilDomene(),
            alderspensjon = this.alderspensjon?.tilDomene(),
            sykepenger = this.sykepenger?.tilDomene(),
            supplerendeStønadAlder = this.supplerendeStønadAlder?.tilDomene(),
            supplerendeStønadFlyktning = this.supplerendeStønadFlyktning?.tilDomene(),
            jobbsjansen = this.jobbsjansen?.tilDomene(),
            trygdOgPensjon = this.trygdOgPensjon?.tilDomene(),
        )
    }

    data class SøknadsTiltakDTO(
        val id: String,
        val deltakelseFom: LocalDate,
        val deltakelseTom: LocalDate,
        val arrangør: String,
        val typeKode: String,
        val typeNavn: String,
    )

    data class PersonopplysningerDTO(
        val ident: String,
        val fornavn: String,
        val etternavn: String,
    )

    data class BarnetilleggDTO(
        val fødselsdato: LocalDate?,
        val fornavn: String?,
        val mellomnavn: String?,
        val etternavn: String?,
        val oppholderSegIEØS: JaNeiSpmDTO,
    )

    data class JaNeiSpmDTO(
        val svar: SpmSvarDTO,
    )

    data class PeriodeSpmDTO(
        val svar: SpmSvarDTO,
        val periode: PeriodeDTO,
    )

    data class FraOgMedDatoSpmDTO(
        val svar: SpmSvarDTO,
        val fraOgMed: LocalDate?,
    )

    enum class SpmSvarDTO {
        Nei,
        Ja,
    }

    fun PersonopplysningerDTO.tilDomene(): Søknad.Personopplysninger = Søknad.Personopplysninger(
        fnr = Fnr.fromString(this.ident),
        fornavn = this.fornavn,
        etternavn = this.etternavn,
    )

    fun PeriodeSpmDTO.tilDomene(): Søknad.PeriodeSpm =
        when (this.svar) {
            SpmSvarDTO.Nei -> Søknad.PeriodeSpm.Nei
            SpmSvarDTO.Ja -> {
                checkNotNull(this.periode.fraOgMed) { "Det skal ikke være mulig med null i fradato hvis man har svart JA " }
                checkNotNull(this.periode.tilOgMed) { "Det skal ikke være mulig med null i tildato hvis man har svart JA " }
                Søknad.PeriodeSpm.Ja(
                    periode = this.periode.toDomain(),
                )
            }
        }

    fun FraOgMedDatoSpmDTO.tilDomene(): Søknad.FraOgMedDatoSpm {
        return when (this.svar) {
            SpmSvarDTO.Nei -> Søknad.FraOgMedDatoSpm.Nei
            SpmSvarDTO.Ja -> {
                requireNotNull(this.fraOgMed) { "Det skal ikke være mulig med null i fradato hvis man har svart JA" }
                Søknad.FraOgMedDatoSpm.Ja(
                    fra = this.fraOgMed,
                )
            }
        }
    }

    fun SøknadsTiltakDTO.tilDomene(): Søknadstiltak =
        Søknadstiltak(
            id = this.id,
            deltakelseFom = this.deltakelseFom,
            deltakelseTom = this.deltakelseTom,
            typeKode = this.typeKode,
            typeNavn = this.typeNavn,
        )

    fun BarnetilleggDTO.tilDomeneManuell(): BarnetilleggFraSøknad.Manuell {
        checkNotNull(this.fornavn) { "Fornavn kan ikke være null for barnetillegg, manuelle barn " }
        checkNotNull(this.etternavn) { "Etternavn kan ikke være null for barnetillegg, manuelle barn " }
        checkNotNull(this.fødselsdato) { "Fødselsdato kan ikke være null for barnetillegg, manuelle barn " }

        return BarnetilleggFraSøknad.Manuell(
            oppholderSegIEØS = this.oppholderSegIEØS.tilDomene(),
            fornavn = this.fornavn,
            mellomnavn = this.mellomnavn,
            etternavn = this.etternavn,
            fødselsdato = this.fødselsdato,
        )
    }

    fun BarnetilleggDTO.tilDomenePdl(): BarnetilleggFraSøknad.FraPdl {
        checkNotNull(this.fødselsdato) { "Fødselsdato kan ikke være null for barnetillegg fra PDL" }
        return BarnetilleggFraSøknad.FraPdl(
            oppholderSegIEØS = this.oppholderSegIEØS.tilDomene(),
            fornavn = this.fornavn,
            mellomnavn = this.mellomnavn,
            etternavn = this.etternavn,
            fødselsdato = this.fødselsdato,
        )
    }

    fun JaNeiSpmDTO.tilDomene(): Søknad.JaNeiSpm =
        when (this.svar) {
            SpmSvarDTO.Nei -> Søknad.JaNeiSpm.Nei
            SpmSvarDTO.Ja -> Søknad.JaNeiSpm.Ja
        }
}
