package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Behandlingsarsak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Oppholdssted
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.LocalDate

data class ManueltRegistrertSøknadBody(
    val journalpostId: String,
    val manueltSattSøknadsperiode: PeriodeDTO?,
    val manueltSattSøknadMottattDato: LocalDate?,
    val manueltSattTiltak: String?,
    val antallVedlegg: Int,
    val søknadstype: SøknadstypeDTO,
    val svar: ManueltRegistrertSøknadSvarDTO,
    val behandlingsarsak: Behandlingsarsak?,
) {
    fun tilKommando(internTiltaksdeltakelsesId: TiltaksdeltakerId?): StartBehandlingAvManueltRegistrertSøknadCommand {
        return StartBehandlingAvManueltRegistrertSøknadCommand(
            journalpostId = JournalpostId(journalpostId),
            manueltSattSøknadsperiode = manueltSattSøknadsperiode?.toDomain(),
            manueltSattSøknadMottattDato = manueltSattSøknadMottattDato,
            manueltSattTiltak = manueltSattTiltak,
            behandlingsarsak = behandlingsarsak,
            søknadstiltak = this.svar.tiltak?.tilDomene(internTiltaksdeltakelsesId),
            barnetillegg = this.svar.barnetilleggPdl.map { it.tilDomenePdl() } +
                this.svar.barnetilleggManuelle.map { it.tilDomeneManuell() },
            antallVedlegg = antallVedlegg,
            søknadstype = søknadstype.tilDomene(),
            harSøktPåTiltak = this.svar.harSøktPåTiltak.tilDomene(),
            harSøktOmBarnetillegg = this.svar.harSøktOmBarnetillegg.tilDomene(),
            kvp = this.svar.kvp.tilDomene(),
            intro = this.svar.intro.tilDomene(),
            institusjon = this.svar.institusjon.tilDomene(),
            etterlønn = this.svar.etterlønn.tilDomene(),
            gjenlevendepensjon = this.svar.gjenlevendepensjon.tilDomene(),
            alderspensjon = this.svar.alderspensjon.tilDomene(),
            sykepenger = this.svar.sykepenger.tilDomene(),
            supplerendeStønadAlder = this.svar.supplerendeStønadAlder.tilDomene(),
            supplerendeStønadFlyktning = this.svar.supplerendeStønadFlyktning.tilDomene(),
            jobbsjansen = this.svar.jobbsjansen.tilDomene(),
            trygdOgPensjon = this.svar.trygdOgPensjon.tilDomene(),
        )
    }

    data class SøknadsTiltakDTO(
        val eksternDeltakelseId: String,
        val deltakelseFraOgMed: LocalDate,
        val deltakelseTilOgMed: LocalDate,
        val tiltakskodeFraKilden: String,
        val typeNavn: String,
    ) {
        fun tilDomene(internTiltaksdeltakelsesId: TiltaksdeltakerId?): Søknadstiltak =
            Søknadstiltak(
                id = this.eksternDeltakelseId,
                deltakelseFom = this.deltakelseFraOgMed,
                deltakelseTom = this.deltakelseTilOgMed,
                typeNavn = this.typeNavn,
                tiltaksdeltakerId = internTiltaksdeltakelsesId
                    ?: throw IllegalArgumentException("Mangler intern tiltaksdeltakerid for eksternid ${this.eksternDeltakelseId} for manuelt registrert søknad"),
            )
    }

    data class BarnetilleggDTO(
        val fødselsdato: LocalDate?,
        val fornavn: String?,
        val mellomnavn: String?,
        val etternavn: String?,
        val oppholdssted: OppholdsstedSpmDTO,
        val fnr: String?,
    ) {

        fun tilDomeneManuell(): BarnetilleggFraSøknad.Manuell {
            checkNotNull(this.fornavn) { "Fornavn kan ikke være null for barnetillegg, manuelle barn " }
            checkNotNull(this.etternavn) { "Etternavn kan ikke være null for barnetillegg, manuelle barn " }
            checkNotNull(this.fødselsdato) { "Fødselsdato kan ikke være null for barnetillegg, manuelle barn " }

            return BarnetilleggFraSøknad.Manuell(
                oppholderSegIEØS = this.oppholderSegIEØS(),
                oppholdssted = this.oppholdssted.tilDomene(),
                fornavn = this.fornavn,
                mellomnavn = this.mellomnavn,
                etternavn = this.etternavn,
                fødselsdato = this.fødselsdato,
            )
        }

        fun tilDomenePdl(): BarnetilleggFraSøknad.FraPdl {
            checkNotNull(this.fødselsdato) { "Fødselsdato kan ikke være null for barnetillegg fra PDL" }
            return BarnetilleggFraSøknad.FraPdl(
                oppholderSegIEØS = this.oppholderSegIEØS(),
                oppholdssted = this.oppholdssted.tilDomene(),
                fornavn = this.fornavn,
                mellomnavn = this.mellomnavn,
                etternavn = this.etternavn,
                fødselsdato = this.fødselsdato,
                fnr = this.fnr?.let { Fnr.fromString(it) },
            )
        }

        private fun oppholderSegIEØS(): Søknad.JaNeiSpm =
            this.oppholdssted.tilDomene()?.tilOppholderSegIEØS() ?: Søknad.JaNeiSpm.IkkeBesvart
    }

    enum class OppholdsstedSvar {
        NORGE,
        ANNET_EØS_LAND,
        IKKE_EØS_LAND,
        IKKE_BESVART,
    }

    data class OppholdsstedSpmDTO(
        val svar: OppholdsstedSvar,
    ) {
        fun tilDomene(): Oppholdssted? =
            when (svar) {
                OppholdsstedSvar.NORGE -> Oppholdssted.NORGE
                OppholdsstedSvar.ANNET_EØS_LAND -> Oppholdssted.ANNET_EØS_LAND
                OppholdsstedSvar.IKKE_EØS_LAND -> Oppholdssted.IKKE_EØS_LAND
                OppholdsstedSvar.IKKE_BESVART -> null
            }
    }

    enum class JaNeiSvar {
        JA,
        NEI,
        IKKE_BESVART,
    }

    data class JaNeiSpmDTO(
        val svar: JaNeiSvar,
    ) {
        fun tilDomene(): Søknad.JaNeiSpm =
            when (this.svar) {
                JaNeiSvar.IKKE_BESVART -> Søknad.JaNeiSpm.IkkeBesvart
                JaNeiSvar.NEI -> Søknad.JaNeiSpm.Nei
                JaNeiSvar.JA -> Søknad.JaNeiSpm.Ja
            }
    }

    data class PeriodeSpmDTO(
        val svar: JaNeiSvar,
        val periode: PeriodeSvarDTO?,
    ) {
        fun tilDomene(): Søknad.PeriodeSpm =
            when (this.svar) {
                JaNeiSvar.IKKE_BESVART -> Søknad.PeriodeSpm.IkkeBesvart

                JaNeiSvar.NEI -> Søknad.PeriodeSpm.Nei

                JaNeiSvar.JA -> {
                    Søknad.PeriodeSpm.Ja(
                        fraOgMed = this.periode?.fraOgMed,
                        tilOgMed = this.periode?.tilOgMed,
                    )
                }
            }
    }

    data class PeriodeSvarDTO(
        val fraOgMed: LocalDate?,
        val tilOgMed: LocalDate?,
    )

    data class FraOgMedDatoSpmDTO(
        val svar: JaNeiSvar,
        val fraOgMed: LocalDate?,
    ) {
        fun tilDomene(): Søknad.FraOgMedDatoSpm {
            return when (this.svar) {
                JaNeiSvar.IKKE_BESVART -> Søknad.FraOgMedDatoSpm.IkkeBesvart

                JaNeiSvar.NEI -> Søknad.FraOgMedDatoSpm.Nei

                JaNeiSvar.JA -> {
                    Søknad.FraOgMedDatoSpm.Ja(
                        fra = this.fraOgMed,
                    )
                }
            }
        }
    }

    data class ManueltRegistrertSøknadSvarDTO(
        val tiltak: SøknadsTiltakDTO?,
        val barnetilleggPdl: List<BarnetilleggDTO> = emptyList(),
        val barnetilleggManuelle: List<BarnetilleggDTO> = emptyList(),
        val harSøktPåTiltak: JaNeiSpmDTO,
        val harSøktOmBarnetillegg: JaNeiSpmDTO,
        val kvp: PeriodeSpmDTO,
        val intro: PeriodeSpmDTO,
        val institusjon: PeriodeSpmDTO,
        val etterlønn: JaNeiSpmDTO,
        val gjenlevendepensjon: PeriodeSpmDTO,
        val alderspensjon: FraOgMedDatoSpmDTO,
        val sykepenger: PeriodeSpmDTO,
        val supplerendeStønadAlder: PeriodeSpmDTO,
        val supplerendeStønadFlyktning: PeriodeSpmDTO,
        val jobbsjansen: PeriodeSpmDTO,
        val trygdOgPensjon: PeriodeSpmDTO,
    )
}
