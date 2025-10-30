package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import java.time.LocalDate

data class PapirsøknadBody(
    val journalpostId: String,
    val personopplysninger: PersonopplysningerDTO,
    val manueltSattSøknadsperiode: PeriodeDTO?,
    val kravDato: LocalDate,
    val antallVedlegg: Int,
    val svar: PapirsøknadSvarDTO,
) {
    fun tilKommando(): StartBehandlingAvPapirsøknadCommand {
        return StartBehandlingAvPapirsøknadCommand(
            personopplysninger = personopplysninger.tilDomene(),
            journalpostId = JournalpostId(journalpostId),
            manueltSattSøknadsperiode = manueltSattSøknadsperiode?.toDomain(),
            søknadstiltak = this.svar.tiltak?.tilDomene(),
            opprettet = kravDato.atStartOfDay(),
            barnetillegg = this.svar.barnetilleggPdl.map { it.tilDomenePdl() } +
                this.svar.barnetilleggManuelle.map { it.tilDomeneManuell() },
            antallVedlegg = antallVedlegg,
            kvp = this.svar.kvp?.tilDomene(),
            intro = this.svar.intro?.tilDomene(),
            institusjon = this.svar.institusjon?.tilDomene(),
            etterlønn = this.svar.etterlønn?.tilDomene(),
            gjenlevendepensjon = this.svar.gjenlevendepensjon?.tilDomene(),
            alderspensjon = this.svar.alderspensjon?.tilDomene(),
            sykepenger = this.svar.sykepenger?.tilDomene(),
            supplerendeStønadAlder = this.svar.supplerendeStønadAlder?.tilDomene(),
            supplerendeStønadFlyktning = this.svar.supplerendeStønadFlyktning?.tilDomene(),
            jobbsjansen = this.svar.jobbsjansen?.tilDomene(),
            trygdOgPensjon = this.svar.trygdOgPensjon?.tilDomene(),
        )
    }

    data class SøknadsTiltakDTO(
        val eksternDeltakelseId: String,
        val deltakelseFraOgMed: LocalDate,
        val deltakelseTilOgMed: LocalDate,
        val arrangørNavn: String?,
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
        val oppholdInnenforEøs: JaNeiSpmDTO?,
    )

    data class JaNeiSpmDTO(
        val svar: Boolean?,
    )

    data class PeriodeSpmDTO(
        val svar: Boolean,
        val periode: PeriodeDTO?,
    )

    data class FraOgMedDatoSpmDTO(
        val svar: Boolean,
        val fraOgMed: LocalDate?,
    )

    fun PersonopplysningerDTO.tilDomene(): Søknad.Personopplysninger = Søknad.Personopplysninger(
        fnr = Fnr.fromString(this.ident),
        fornavn = this.fornavn,
        etternavn = this.etternavn,
    )

    fun PeriodeSpmDTO.tilDomene(): Søknad.PeriodeSpm =
        when (this.svar) {
            false -> Søknad.PeriodeSpm.Nei
            true -> {
                checkNotNull(this.periode?.fraOgMed) { "Det skal ikke være mulig med null i fradato hvis man har svart JA " }
                checkNotNull(this.periode.tilOgMed) { "Det skal ikke være mulig med null i tildato hvis man har svart JA " }
                Søknad.PeriodeSpm.Ja(
                    periode = this.periode.toDomain(),
                )
            }
        }

    fun FraOgMedDatoSpmDTO.tilDomene(): Søknad.FraOgMedDatoSpm {
        return when (this.svar) {
            false -> Søknad.FraOgMedDatoSpm.Nei
            true -> {
                requireNotNull(this.fraOgMed) { "Det skal ikke være mulig med null i fradato hvis man har svart JA" }
                Søknad.FraOgMedDatoSpm.Ja(
                    fra = this.fraOgMed,
                )
            }
        }
    }

    fun SøknadsTiltakDTO.tilDomene(): Søknadstiltak =
        Søknadstiltak(
            id = this.eksternDeltakelseId,
            deltakelseFom = this.deltakelseFraOgMed,
            deltakelseTom = this.deltakelseTilOgMed,
            typeKode = TiltakResponsDTO.TiltakType.valueOf(this.typeKode),
            typeNavn = this.typeNavn,
        )

    fun BarnetilleggDTO.tilDomeneManuell(): BarnetilleggFraSøknad.Manuell {
        checkNotNull(this.fornavn) { "Fornavn kan ikke være null for barnetillegg, manuelle barn " }
        checkNotNull(this.etternavn) { "Etternavn kan ikke være null for barnetillegg, manuelle barn " }
        checkNotNull(this.fødselsdato) { "Fødselsdato kan ikke være null for barnetillegg, manuelle barn " }

        return BarnetilleggFraSøknad.Manuell(
            oppholderSegIEØS = this.oppholdInnenforEøs?.tilDomene(),
            fornavn = this.fornavn,
            mellomnavn = this.mellomnavn,
            etternavn = this.etternavn,
            fødselsdato = this.fødselsdato,
        )
    }

    fun BarnetilleggDTO.tilDomenePdl(): BarnetilleggFraSøknad.FraPdl {
        checkNotNull(this.fødselsdato) { "Fødselsdato kan ikke være null for barnetillegg fra PDL" }
        return BarnetilleggFraSøknad.FraPdl(
            oppholderSegIEØS = this.oppholdInnenforEøs?.tilDomene(),
            fornavn = this.fornavn,
            mellomnavn = this.mellomnavn,
            etternavn = this.etternavn,
            fødselsdato = this.fødselsdato,
        )
    }

    fun JaNeiSpmDTO.tilDomene(): Søknad.JaNeiSpm? =
        when (this.svar) {
            false -> Søknad.JaNeiSpm.Nei
            true -> Søknad.JaNeiSpm.Ja
            null -> null
        }
}
