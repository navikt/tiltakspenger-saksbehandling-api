package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Behandlingsarsak
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
    val behandlingsarsak: Behandlingsarsak?,
) {
    fun tilKommando(internTiltaksdeltakelsesId: TiltaksdeltakerId?): StartBehandlingAvManueltRegistrertSøknadCommand {
        return StartBehandlingAvManueltRegistrertSøknadCommand(
            journalpostId = JournalpostId(journalpostId),
            manueltSattSøknadsperiode = manueltSattSøknadsperiode?.toDomain(),
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
        val typeKode: TiltakstypeSomGirRettDTO,
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
                    fraOgMed = this.fraOgMed?.let { LocalDate.parse(it) },
                    tilOgMed = this.tilOgMed?.let { LocalDate.parse(it) },
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
            tiltaksdeltakerId = internTiltaksdeltakelsesId
                ?: throw IllegalArgumentException("Mangler intern tiltaksdeltakerid for eksternid ${this.eksternDeltakelseId} for manuelt registrert søknad"),
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

fun TiltakstypeSomGirRettDTO.tilTiltakstype(): TiltakResponsDTO.TiltakTypeDTO {
    return when (this) {
        TiltakstypeSomGirRettDTO.ARBEIDSFORBEREDENDE_TRENING -> TiltakResponsDTO.TiltakTypeDTO.ARBFORB
        TiltakstypeSomGirRettDTO.ARBEIDSRETTET_REHABILITERING -> TiltakResponsDTO.TiltakTypeDTO.ARBRRHDAG
        TiltakstypeSomGirRettDTO.ARBEIDSTRENING -> TiltakResponsDTO.TiltakTypeDTO.ARBTREN
        TiltakstypeSomGirRettDTO.AVKLARING -> TiltakResponsDTO.TiltakTypeDTO.AVKLARAG
        TiltakstypeSomGirRettDTO.DIGITAL_JOBBKLUBB -> TiltakResponsDTO.TiltakTypeDTO.DIGIOPPARB
        TiltakstypeSomGirRettDTO.ENKELTPLASS_AMO -> TiltakResponsDTO.TiltakTypeDTO.ENKELAMO
        TiltakstypeSomGirRettDTO.ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG -> TiltakResponsDTO.TiltakTypeDTO.ENKFAGYRKE
        TiltakstypeSomGirRettDTO.FORSØK_OPPLÆRING_LENGRE_VARIGHET -> TiltakResponsDTO.TiltakTypeDTO.FORSOPPLEV
        TiltakstypeSomGirRettDTO.GRUPPE_AMO -> TiltakResponsDTO.TiltakTypeDTO.GRUPPEAMO
        TiltakstypeSomGirRettDTO.GRUPPE_VGS_OG_HØYERE_YRKESFAG -> TiltakResponsDTO.TiltakTypeDTO.GRUFAGYRKE
        TiltakstypeSomGirRettDTO.HØYERE_UTDANNING -> TiltakResponsDTO.TiltakTypeDTO.HOYEREUTD
        TiltakstypeSomGirRettDTO.INDIVIDUELL_JOBBSTØTTE -> TiltakResponsDTO.TiltakTypeDTO.INDJOBSTOT
        TiltakstypeSomGirRettDTO.INDIVIDUELL_KARRIERESTØTTE_UNG -> TiltakResponsDTO.TiltakTypeDTO.IPSUNG
        TiltakstypeSomGirRettDTO.JOBBKLUBB -> TiltakResponsDTO.TiltakTypeDTO.JOBBK
        TiltakstypeSomGirRettDTO.OPPFØLGING -> TiltakResponsDTO.TiltakTypeDTO.INDOPPFAG
        TiltakstypeSomGirRettDTO.UTVIDET_OPPFØLGING_I_NAV -> TiltakResponsDTO.TiltakTypeDTO.UTVAOONAV
        TiltakstypeSomGirRettDTO.UTVIDET_OPPFØLGING_I_OPPLÆRING -> TiltakResponsDTO.TiltakTypeDTO.UTVOPPFOPL
        TiltakstypeSomGirRettDTO.ARBEIDSMARKEDSOPPLAERING -> TiltakResponsDTO.TiltakTypeDTO.ARBEIDSMARKEDSOPPLAERING
        TiltakstypeSomGirRettDTO.FAG_OG_YRKESOPPLAERING -> TiltakResponsDTO.TiltakTypeDTO.FAG_OG_YRKESOPPLAERING
        TiltakstypeSomGirRettDTO.HOYERE_YRKESFAGLIG_UTDANNING -> TiltakResponsDTO.TiltakTypeDTO.HOYERE_YRKESFAGLIG_UTDANNING
        TiltakstypeSomGirRettDTO.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> TiltakResponsDTO.TiltakTypeDTO.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV
        TiltakstypeSomGirRettDTO.STUDIESPESIALISERING -> TiltakResponsDTO.TiltakTypeDTO.STUDIESPESIALISERING
    }
}
