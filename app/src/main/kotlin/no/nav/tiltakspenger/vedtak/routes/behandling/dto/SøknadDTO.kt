package no.nav.tiltakspenger.vedtak.routes.behandling.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.behandling.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad.FraOgMedDatoSpm
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad.JaNeiSpm
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad.PeriodeSpm
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknadstiltak
import java.time.LocalDate
import java.time.LocalDateTime

data class SøknadDTO(
    val journalpostId: String,
    val tiltak: TiltaksdeltagelseFraSøknadDTO,
    val barnetillegg: List<BarnetilleggFraSøknad>,
    val opprettet: LocalDateTime,
    val tidsstempelHosOss: LocalDateTime,
    val kvp: PeriodeDTO?,
    val intro: PeriodeDTO?,
    val institusjon: PeriodeDTO?,
    val etterlønn: Boolean,
    val gjenlevendepensjon: PeriodeDTO?,
    val alderspensjon: LocalDate?,
    val sykepenger: PeriodeDTO?,
    val supplerendeStønadAlder: PeriodeDTO?,
    val supplerendeStønadFlyktning: PeriodeDTO?,
    val jobbsjansen: PeriodeDTO?,
    val trygdOgPensjon: PeriodeDTO?,
    val antallVedlegg: Int,
) {
    data class TiltaksdeltagelseFraSøknadDTO(
        val fraOgMed: String?,
        val tilOgMed: String?,
        val typeKode: String,
        val typeNavn: String,
    )
}

fun Søknad.toDTO(): SøknadDTO {
    return SøknadDTO(
        journalpostId = this.journalpostId.toString(),
        tiltak = this.tiltak.toDTO(),
        barnetillegg = this.barnetillegg,
        opprettet = this.opprettet,
        tidsstempelHosOss = this.tidsstempelHosOss,
        kvp = this.kvp.toDTO(),
        intro = this.intro.toDTO(),
        institusjon = this.institusjon.toDTO(),
        etterlønn = this.etterlønn.toDTO(),
        gjenlevendepensjon = this.gjenlevendepensjon.toDTO(),
        alderspensjon = this.alderspensjon.toDTO(),
        sykepenger = this.sykepenger.toDTO(),
        supplerendeStønadAlder = this.supplerendeStønadAlder.toDTO(),
        supplerendeStønadFlyktning = this.supplerendeStønadFlyktning.toDTO(),
        jobbsjansen = this.jobbsjansen.toDTO(),
        trygdOgPensjon = this.trygdOgPensjon.toDTO(),
        antallVedlegg = this.vedlegg,
    )
}

fun JaNeiSpm.toDTO(): Boolean {
    return when (this) {
        JaNeiSpm.Ja -> true
        JaNeiSpm.Nei -> false
    }
}

fun FraOgMedDatoSpm.toDTO(): LocalDate? {
    return when (this) {
        is FraOgMedDatoSpm.Nei -> null
        is FraOgMedDatoSpm.Ja -> this.fra
    }
}

fun PeriodeSpm.toDTO(): PeriodeDTO? {
    return when (this) {
        is PeriodeSpm.Nei -> null
        is PeriodeSpm.Ja -> this.periode.toDTO()
    }
}

fun Søknadstiltak.toDTO(): SøknadDTO.TiltaksdeltagelseFraSøknadDTO {
    return SøknadDTO.TiltaksdeltagelseFraSøknadDTO(
        fraOgMed = this.deltakelseFom.toString(),
        tilOgMed = this.deltakelseTom.toString(),
        typeKode = this.typeKode,
        typeNavn = this.typeNavn,
    )
}
