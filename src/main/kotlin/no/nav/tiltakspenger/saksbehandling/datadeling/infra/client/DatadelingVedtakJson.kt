package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingVedtakJson.ValgtHjemmelHarIkkeRettighet
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

private data class DatadelingVedtakJson(
    val vedtakId: String,
    val sakId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val virkningsperiode: PeriodeDTO,
    val innvilgelsesperiode: PeriodeDTO?,
    val omgjørRammevedtakId: String?,
    val omgjortAvRammevedtakId: String?,
    val rettighet: String,
    val opprettet: String,
    val barnetillegg: Barnetillegg?,
    val valgteHjemlerHarIkkeRettighet: List<String>?,
) {
    data class Barnetillegg(
        val perioder: List<BarnetilleggPeriode>,
    )

    data class BarnetilleggPeriode(
        val antallBarn: Int,
        val periode: Periode,
    ) {
        data class Periode(
            val fraOgMed: LocalDate,
            val tilOgMed: LocalDate,
        )
    }

    enum class ValgtHjemmelHarIkkeRettighet {
        DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK,
        ALDER,
        LIVSOPPHOLDSYTELSER,
        KVALIFISERINGSPROGRAMMET,
        INTRODUKSJONSPROGRAMMET,
        LONN_FRA_TILTAKSARRANGOR,
        LONN_FRA_ANDRE,
        INSTITUSJONSOPPHOLD,
        FREMMET_FOR_SENT,
    }
}

fun Rammevedtak.toDatadelingJson(): String {
    return DatadelingVedtakJson(
        vedtakId = this.id.toString(),
        sakId = this.sakId.toString(),
        // Kommentar jah: Deprekerer fom og tom (erstattes av virkningsperiode+innvilgelsesperiode).
        fom = periode.fraOgMed,
        tom = periode.tilOgMed,
        virkningsperiode = periode.toDTO(),
        innvilgelsesperiode = innvilgelsesperiode?.toDTO(),
        // TODO jah: omgjørRammevedtakId og omgjortAvRammevedtakId bør gjøres om etter vi har lagt på eksplisitt omgjøring på vedtakene.
        // Kommentar jah: Disse ble lagt til utelukkende for revurdering til omgjøring for å tydeliggjøre at vedtaket omgjør et annet vedtak i sin helhet.
        // Det vil være en ny avgjørelse dersom vi skal dele informasjon fra andre vedtak her. Det vil være redundant med virkningsperioden/vurderingsperioden.
        omgjørRammevedtakId = if (this.erOmgjøringsbehandling) this.omgjørRammevedtak.single().rammevedtakId.toString() else null,
        // Kommentar jah: Hvis vi skulle beholdt dagens logikk her, måtte vi sjekket om rammevedtaket som omgjorde dette vedtaket var en omgjøringsbehandling. Istedenfor å gjøre det, deler vi det vedtaket som har omgjort dette vedtaket helt.
        omgjortAvRammevedtakId = if (this.omgjortAvRammevedtak.size == 1 && this.omgjortAvRammevedtak.first().omgjøringsgrad == Omgjøringsgrad.HELT) this.omgjortAvRammevedtak.first().rammevedtakId.toString() else null,
        rettighet = when (this.resultat) {
            is BehandlingResultat.Innvilgelse -> {
                if (barnetillegg?.harBarnetillegg == true) {
                    "TILTAKSPENGER_OG_BARNETILLEGG"
                } else {
                    "TILTAKSPENGER"
                }
            }

            is RevurderingResultat.Stans -> "STANS"
            is SøknadsbehandlingResultat.Avslag -> "AVSLAG"
        },
        opprettet = opprettet.toString(),
        barnetillegg = barnetillegg?.toDatadelingBarnetillegg(),
        valgteHjemlerHarIkkeRettighet = this.toValgteHjemlerHarIkkeRettighetListe(),
    ).let { serialize(it) }
}

private fun Barnetillegg.toDatadelingBarnetillegg(): DatadelingVedtakJson.Barnetillegg? = if (harBarnetillegg) {
    DatadelingVedtakJson.Barnetillegg(
        perioder = this.periodisering.perioderMedVerdi.toList().map {
            DatadelingVedtakJson.BarnetilleggPeriode(
                antallBarn = it.verdi.value,
                periode = DatadelingVedtakJson.BarnetilleggPeriode.Periode(
                    fraOgMed = it.periode.fraOgMed,
                    tilOgMed = it.periode.tilOgMed,
                ),
            )
        },
    )
} else {
    null
}

private fun Rammevedtak.toValgteHjemlerHarIkkeRettighetListe(): List<String>? {
    return when (this.resultat) {
        is BehandlingResultat.Innvilgelse -> null
        is RevurderingResultat.Stans -> (this.behandling as Revurdering).toValgteHjemlerHarIkkeRettighetListe()
        is SøknadsbehandlingResultat.Avslag -> (this.behandling as Søknadsbehandling).toValgteHjemlerHarIkkeRettighetListe()
    }
}

private fun Revurdering.toValgteHjemlerHarIkkeRettighetListe() =
    (this.resultat as RevurderingResultat.Stans).valgtHjemmel.map { it.toValgtHjemmelHarIkkeRettighetString() }

private fun Søknadsbehandling.toValgteHjemlerHarIkkeRettighetListe() =
    (this.resultat as SøknadsbehandlingResultat.Avslag).avslagsgrunner.map { it.toValgtHjemmelHarIkkeRettighetString() }

private fun ValgtHjemmelForStans.toValgtHjemmelHarIkkeRettighetString() =
    when (this) {
        ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak -> ValgtHjemmelHarIkkeRettighet.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK.name
        ValgtHjemmelForStans.Alder -> ValgtHjemmelHarIkkeRettighet.ALDER.name
        ValgtHjemmelForStans.Institusjonsopphold -> ValgtHjemmelHarIkkeRettighet.INSTITUSJONSOPPHOLD.name
        ValgtHjemmelForStans.Introduksjonsprogrammet -> ValgtHjemmelHarIkkeRettighet.INTRODUKSJONSPROGRAMMET.name
        ValgtHjemmelForStans.Kvalifiseringsprogrammet -> ValgtHjemmelHarIkkeRettighet.KVALIFISERINGSPROGRAMMET.name
        ValgtHjemmelForStans.Livsoppholdytelser -> ValgtHjemmelHarIkkeRettighet.LIVSOPPHOLDSYTELSER.name
        ValgtHjemmelForStans.LønnFraAndre -> ValgtHjemmelHarIkkeRettighet.LONN_FRA_ANDRE.name
        ValgtHjemmelForStans.LønnFraTiltaksarrangør -> ValgtHjemmelHarIkkeRettighet.LONN_FRA_TILTAKSARRANGOR.name
    }

private fun Avslagsgrunnlag.toValgtHjemmelHarIkkeRettighetString() =
    when (this) {
        Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak -> ValgtHjemmelHarIkkeRettighet.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK.name
        Avslagsgrunnlag.Alder -> ValgtHjemmelHarIkkeRettighet.ALDER.name
        Avslagsgrunnlag.FremmetForSent -> ValgtHjemmelHarIkkeRettighet.FREMMET_FOR_SENT.name
        Avslagsgrunnlag.Institusjonsopphold -> ValgtHjemmelHarIkkeRettighet.INSTITUSJONSOPPHOLD.name
        Avslagsgrunnlag.Introduksjonsprogrammet -> ValgtHjemmelHarIkkeRettighet.INTRODUKSJONSPROGRAMMET.name
        Avslagsgrunnlag.Kvalifiseringsprogrammet -> ValgtHjemmelHarIkkeRettighet.KVALIFISERINGSPROGRAMMET.name
        Avslagsgrunnlag.Livsoppholdytelser -> ValgtHjemmelHarIkkeRettighet.LIVSOPPHOLDSYTELSER.name
        Avslagsgrunnlag.LønnFraAndre -> ValgtHjemmelHarIkkeRettighet.LONN_FRA_ANDRE.name
        Avslagsgrunnlag.LønnFraTiltaksarrangør -> ValgtHjemmelHarIkkeRettighet.LONN_FRA_TILTAKSARRANGOR.name
    }
