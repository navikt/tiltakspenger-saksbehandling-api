package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AvbruttDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAvbruttDTO
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Behandlingsarsak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.IkkeInnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad.FraOgMedDatoSpm
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad.JaNeiSpm
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad.PeriodeSpm
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import java.time.LocalDate
import java.time.LocalDateTime

data class SøknadDTO(
    val id: String,
    val journalpostId: String,
    val tiltak: TiltaksdeltagelseFraSøknadDTO?,
    val tiltaksdeltakelseperiodeDetErSøktOm: PeriodeDTO?,
    val manueltSattTiltak: String?,
    val søknadstype: SøknadstypeDTO,
    val barnetillegg: List<BarnetilleggFraSøknadDTO>,
    val opprettet: LocalDateTime,
    val tidsstempelHosOss: LocalDateTime,
    val antallVedlegg: Int,
    val avbrutt: AvbruttDTO?,
    val kanInnvilges: Boolean,
    val svar: SøknadSvarDTO,
    val behandlingsarsak: Behandlingsarsak?,
) {
    data class TiltaksdeltagelseFraSøknadDTO(
        val id: String,
        val fraOgMed: String?,
        val tilOgMed: String?,
        val typeKode: String,
        val typeNavn: String,
    )

    data class BarnetilleggFraSøknadDTO(
        val oppholderSegIEØSSpm: JaNeiSpmDTO,
        val fornavn: String?,
        val mellomnavn: String?,
        val etternavn: String?,
        val fødselsdato: LocalDate,
        val kilde: BarnetilleggFraSøknadKilde,
        val fnr: String?,
    )

    enum class BarnetilleggFraSøknadKilde {
        PDL,
        Manuell,
    }

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
        val periode: PeriodeDTO?,
    )

    data class FraOgMedDatoSpmDTO(
        val svar: JaNeiSvar,
        val fraOgMed: LocalDate?,
    )

    data class SøknadSvarDTO(
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

fun Søknad.toSøknadDTO(): SøknadDTO {
    return when (this) {
        is InnvilgbarSøknad -> this.toSøknadDTO()
        is IkkeInnvilgbarSøknad -> this.toSøknadDTO()
    }
}

fun InnvilgbarSøknad.toSøknadDTO(): SøknadDTO {
    return SøknadDTO(
        id = this.id.toString(),
        journalpostId = this.journalpostId,
        tiltak = this.tiltak.toDTO(),
        manueltSattTiltak = this.manueltSattTiltak,
        tiltaksdeltakelseperiodeDetErSøktOm = this.tiltaksdeltakelseperiodeDetErSøktOm().toDTO(),
        barnetillegg = this.barnetillegg.toDTO(),
        søknadstype = this.søknadstype.toDTO(),
        opprettet = this.opprettet,
        tidsstempelHosOss = this.tidsstempelHosOss,
        antallVedlegg = this.vedlegg,
        avbrutt = avbrutt?.toAvbruttDTO(),
        kanInnvilges = this.kanInnvilges(),
        svar = SøknadDTO.SøknadSvarDTO(
            harSøktPåTiltak = harSøktPåTiltak.toDTO(),
            harSøktOmBarnetillegg = harSøktOmBarnetillegg.toDTO(),
            kvp = kvp.toDTO(),
            intro = intro.toDTO(),
            institusjon = institusjon.toDTO(),
            etterlønn = etterlønn.toDTO(),
            gjenlevendepensjon = gjenlevendepensjon.toDTO(),
            alderspensjon = alderspensjon.toDTO(),
            sykepenger = sykepenger.toDTO(),
            supplerendeStønadAlder = supplerendeStønadAlder.toDTO(),
            supplerendeStønadFlyktning = supplerendeStønadFlyktning.toDTO(),
            jobbsjansen = jobbsjansen.toDTO(),
            trygdOgPensjon = trygdOgPensjon.toDTO(),
        ),
        behandlingsarsak = behandlingsarsak,
    )
}

fun IkkeInnvilgbarSøknad.toSøknadDTO(): SøknadDTO {
    return SøknadDTO(
        id = this.id.toString(),
        journalpostId = this.journalpostId,
        tiltak = this.tiltak?.toDTO(),
        manueltSattTiltak = this.manueltSattTiltak,
        tiltaksdeltakelseperiodeDetErSøktOm = this.tiltaksdeltakelseperiodeDetErSøktOm()?.toDTO(),
        barnetillegg = this.barnetillegg.toDTO(),
        søknadstype = this.søknadstype.toDTO(),
        opprettet = this.opprettet,
        tidsstempelHosOss = this.tidsstempelHosOss,
        antallVedlegg = this.vedlegg,
        avbrutt = avbrutt?.toAvbruttDTO(),
        kanInnvilges = this.kanInnvilges(),
        svar = SøknadDTO.SøknadSvarDTO(
            harSøktPåTiltak = harSøktPåTiltak.toDTO(),
            harSøktOmBarnetillegg = harSøktOmBarnetillegg.toDTO(),
            kvp = kvp.toDTO(),
            intro = intro.toDTO(),
            institusjon = institusjon.toDTO(),
            etterlønn = etterlønn.toDTO(),
            gjenlevendepensjon = gjenlevendepensjon.toDTO(),
            alderspensjon = alderspensjon.toDTO(),
            sykepenger = sykepenger.toDTO(),
            supplerendeStønadAlder = supplerendeStønadAlder.toDTO(),
            supplerendeStønadFlyktning = supplerendeStønadFlyktning.toDTO(),
            jobbsjansen = jobbsjansen.toDTO(),
            trygdOgPensjon = trygdOgPensjon.toDTO(),
        ),
        behandlingsarsak = behandlingsarsak,
    )
}

@JvmName("søknadToDTO")
fun List<Søknad>.toSøknadDTO(): List<SøknadDTO> {
    return this.map { it.toSøknadDTO() }
}

fun JaNeiSpm.toDTO(): SøknadDTO.JaNeiSpmDTO {
    return SøknadDTO.JaNeiSpmDTO(
        svar = when (this) {
            JaNeiSpm.Ja -> SøknadDTO.JaNeiSvar.JA
            JaNeiSpm.Nei -> SøknadDTO.JaNeiSvar.NEI
            JaNeiSpm.IkkeBesvart -> SøknadDTO.JaNeiSvar.IKKE_BESVART
        },
    )
}

fun FraOgMedDatoSpm.toDTO(): SøknadDTO.FraOgMedDatoSpmDTO {
    val svar = when (this) {
        is FraOgMedDatoSpm.IkkeBesvart -> SøknadDTO.JaNeiSvar.IKKE_BESVART
        is FraOgMedDatoSpm.Nei -> SøknadDTO.JaNeiSvar.NEI
        is FraOgMedDatoSpm.Ja -> SøknadDTO.JaNeiSvar.JA
    }

    return SøknadDTO.FraOgMedDatoSpmDTO(
        svar = svar,
        fraOgMed = when (this) {
            is FraOgMedDatoSpm.Nei, FraOgMedDatoSpm.IkkeBesvart -> null
            is FraOgMedDatoSpm.Ja -> this.fra
        },
    )
}

fun PeriodeSpm.toDTO(): SøknadDTO.PeriodeSpmDTO {
    return SøknadDTO.PeriodeSpmDTO(
        svar = when (this) {
            is PeriodeSpm.IkkeBesvart -> SøknadDTO.JaNeiSvar.IKKE_BESVART
            is PeriodeSpm.Nei -> SøknadDTO.JaNeiSvar.NEI
            is PeriodeSpm.Ja -> SøknadDTO.JaNeiSvar.JA
        },
        periode = when (this) {
            is PeriodeSpm.Nei, PeriodeSpm.IkkeBesvart -> null

            is PeriodeSpm.Ja -> PeriodeDTO(
                fraOgMed = this.fraOgMed?.toString() ?: "",
                tilOgMed = this.tilOgMed?.toString() ?: "",
            )
        },
    )
}

fun Søknadstiltak.toDTO(): SøknadDTO.TiltaksdeltagelseFraSøknadDTO {
    return SøknadDTO.TiltaksdeltagelseFraSøknadDTO(
        id = id,
        fraOgMed = this.deltakelseFom.toString(),
        tilOgMed = this.deltakelseTom.toString(),
        typeKode = this.typeKode.name,
        typeNavn = this.typeNavn,
    )
}

fun List<BarnetilleggFraSøknad>.toDTO(): List<SøknadDTO.BarnetilleggFraSøknadDTO> = this.map {
    SøknadDTO.BarnetilleggFraSøknadDTO(
        oppholderSegIEØSSpm = it.oppholderSegIEØS.toDTO(),
        fornavn = it.fornavn,
        mellomnavn = it.mellomnavn,
        etternavn = it.etternavn,
        fødselsdato = it.fødselsdato,
        kilde = when (it) {
            is BarnetilleggFraSøknad.FraPdl -> SøknadDTO.BarnetilleggFraSøknadKilde.PDL
            is BarnetilleggFraSøknad.Manuell -> SøknadDTO.BarnetilleggFraSøknadKilde.Manuell
        },
        fnr = when (it) {
            is BarnetilleggFraSøknad.FraPdl -> it.fnr?.verdi
            is BarnetilleggFraSøknad.Manuell -> null
        },
    )
}
