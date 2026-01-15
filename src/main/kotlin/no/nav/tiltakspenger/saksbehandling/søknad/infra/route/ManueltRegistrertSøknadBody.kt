package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.LocalDate

data class ManueltRegistrertSøknadBody(
    val journalpostId: String,
    val manueltSattSøknadsperiode: PeriodeDTO?,
    val manueltSattTiltak: String?,
    val antallVedlegg: Int,
    val søknadstype: SøknadstypeDTO,
    val svar: ManueltRegistrertSøknadSvarDTO,
) {
    fun tilKommando(internTiltaksdeltakelsesId: TiltaksdeltakerId?): StartBehandlingAvManueltRegistrertSøknadCommand {
        return StartBehandlingAvManueltRegistrertSøknadCommand(
            journalpostId = JournalpostId(journalpostId),
            manueltSattSøknadsperiode = manueltSattSøknadsperiode?.toDomain(),
            manueltSattTiltak = manueltSattTiltak,
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
        val arrangørNavn: String?,
        val typeKode: TiltakstypeSomGirRett,
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
        val oppholdInnenforEøs: JaNeiSpmDTO,
        val fnr: String?,
    )

    enum class JaNeiSvar {
        JA,
        NEI,
        IKKE_BESVART,
    }

    data class JaNeiSpmDTO(
        val svar: JaNeiSvar,
    )

    data class PeriodeSpmDTO(
        val svar: JaNeiSvar,
        @Deprecated("Bruk fraOgMed og tilOgMed feltene i stedet fordi en periode kan ikke mangle verken eller")
        val periode: PeriodeDTO?,
        val fraOgMed: String?,
        val tilOgMed: String?,
    )

    data class FraOgMedDatoSpmDTO(
        val svar: JaNeiSvar,
        val fraOgMed: LocalDate?,
    )

    fun PersonopplysningerDTO.tilDomene(): Søknad.Personopplysninger = Søknad.Personopplysninger(
        fnr = Fnr.fromString(this.ident),
        fornavn = this.fornavn,
        etternavn = this.etternavn,
    )

    fun PeriodeSpmDTO.tilDomene(): Søknad.PeriodeSpm =
        when (this.svar) {
            JaNeiSvar.IKKE_BESVART -> Søknad.PeriodeSpm.IkkeBesvart
            JaNeiSvar.NEI -> Søknad.PeriodeSpm.Nei
            JaNeiSvar.JA -> {
                Søknad.PeriodeSpm.Ja(
                    fraOgMed = this.fraOgMed?.let { LocalDate.parse(it) }
                        ?: this.periode?.fraOgMed?.let { LocalDate.parse(it) },
                    tilOgMed = this.tilOgMed?.let { LocalDate.parse(it) }
                        ?: this.periode?.tilOgMed?.let { LocalDate.parse(it) },
                )
            }
        }

    fun FraOgMedDatoSpmDTO.tilDomene(): Søknad.FraOgMedDatoSpm {
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

    fun SøknadsTiltakDTO.tilDomene(internTiltaksdeltakelsesId: TiltaksdeltakerId?): Søknadstiltak =
        Søknadstiltak(
            id = this.eksternDeltakelseId,
            deltakelseFom = this.deltakelseFraOgMed,
            deltakelseTom = this.deltakelseTilOgMed,
            typeKode = this.typeKode.tilTiltakstype(),
            typeNavn = this.typeNavn,
            tiltaksdeltakerId = internTiltaksdeltakelsesId ?: throw IllegalArgumentException("Mangler intern tiltaksdeltakerid for eksternid ${this.eksternDeltakelseId} for manuelt registrert søknad"),
        )

    fun BarnetilleggDTO.tilDomeneManuell(): BarnetilleggFraSøknad.Manuell {
        checkNotNull(this.fornavn) { "Fornavn kan ikke være null for barnetillegg, manuelle barn " }
        checkNotNull(this.etternavn) { "Etternavn kan ikke være null for barnetillegg, manuelle barn " }
        checkNotNull(this.fødselsdato) { "Fødselsdato kan ikke være null for barnetillegg, manuelle barn " }

        return BarnetilleggFraSøknad.Manuell(
            oppholderSegIEØS = this.oppholdInnenforEøs.tilDomene(),
            fornavn = this.fornavn,
            mellomnavn = this.mellomnavn,
            etternavn = this.etternavn,
            fødselsdato = this.fødselsdato,
        )
    }

    fun BarnetilleggDTO.tilDomenePdl(): BarnetilleggFraSøknad.FraPdl {
        checkNotNull(this.fødselsdato) { "Fødselsdato kan ikke være null for barnetillegg fra PDL" }
        return BarnetilleggFraSøknad.FraPdl(
            oppholderSegIEØS = this.oppholdInnenforEøs.tilDomene(),
            fornavn = this.fornavn,
            mellomnavn = this.mellomnavn,
            etternavn = this.etternavn,
            fødselsdato = this.fødselsdato,
            fnr = this.fnr?.let { Fnr.fromString(it) },
        )
    }

    fun JaNeiSpmDTO.tilDomene(): Søknad.JaNeiSpm =
        when (this.svar) {
            JaNeiSvar.IKKE_BESVART -> Søknad.JaNeiSpm.IkkeBesvart
            JaNeiSvar.NEI -> Søknad.JaNeiSpm.Nei
            JaNeiSvar.JA -> Søknad.JaNeiSpm.Ja
        }
}

fun TiltakstypeSomGirRett.tilTiltakstype(): TiltakResponsDTO.TiltakType {
    return when (this) {
        TiltakstypeSomGirRett.ARBEIDSFORBEREDENDE_TRENING -> TiltakResponsDTO.TiltakType.ARBFORB
        TiltakstypeSomGirRett.ARBEIDSRETTET_REHABILITERING -> TiltakResponsDTO.TiltakType.ARBRRHDAG
        TiltakstypeSomGirRett.ARBEIDSTRENING -> TiltakResponsDTO.TiltakType.ARBTREN
        TiltakstypeSomGirRett.AVKLARING -> TiltakResponsDTO.TiltakType.AVKLARAG
        TiltakstypeSomGirRett.DIGITAL_JOBBKLUBB -> TiltakResponsDTO.TiltakType.DIGIOPPARB
        TiltakstypeSomGirRett.ENKELTPLASS_AMO -> TiltakResponsDTO.TiltakType.ENKELAMO
        TiltakstypeSomGirRett.ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG -> TiltakResponsDTO.TiltakType.ENKFAGYRKE
        TiltakstypeSomGirRett.FORSØK_OPPLÆRING_LENGRE_VARIGHET -> TiltakResponsDTO.TiltakType.FORSOPPLEV
        TiltakstypeSomGirRett.GRUPPE_AMO -> TiltakResponsDTO.TiltakType.GRUPPEAMO
        TiltakstypeSomGirRett.GRUPPE_VGS_OG_HØYERE_YRKESFAG -> TiltakResponsDTO.TiltakType.GRUFAGYRKE
        TiltakstypeSomGirRett.HØYERE_UTDANNING -> TiltakResponsDTO.TiltakType.HOYEREUTD
        TiltakstypeSomGirRett.INDIVIDUELL_JOBBSTØTTE -> TiltakResponsDTO.TiltakType.INDJOBSTOT
        TiltakstypeSomGirRett.INDIVIDUELL_KARRIERESTØTTE_UNG -> TiltakResponsDTO.TiltakType.IPSUNG
        TiltakstypeSomGirRett.JOBBKLUBB -> TiltakResponsDTO.TiltakType.JOBBK
        TiltakstypeSomGirRett.OPPFØLGING -> TiltakResponsDTO.TiltakType.INDOPPFAG
        TiltakstypeSomGirRett.UTVIDET_OPPFØLGING_I_NAV -> TiltakResponsDTO.TiltakType.UTVAOONAV
        TiltakstypeSomGirRett.UTVIDET_OPPFØLGING_I_OPPLÆRING -> TiltakResponsDTO.TiltakType.UTVOPPFOPL
    }
}
