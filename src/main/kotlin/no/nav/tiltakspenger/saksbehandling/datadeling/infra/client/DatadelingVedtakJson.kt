package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingVedtakJson.DatadelingHjemmelHarIkkeRettighet
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingVedtakJson.DatadelingRettighet
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

private data class DatadelingVedtakJson(
    val vedtakId: String,
    val sakId: String,
    val vedtaksperiode: PeriodeDTO,
    // TODO: oppdater til periodisert innvilgelse
    val innvilgelsesperiode: PeriodeDTO?,
    // TODO: erstattes av [omgjørRammevedtak] - kan fjernes når datadeling ikke bruker dette feltet lenger.
    val omgjørRammevedtakId: String?,
    // TODO: erstattes av [omgjortAvRammevedtak] - kan fjernes når datadeling ikke bruker dette feltet lenger.
    val omgjortAvRammevedtakId: String?,
    val rettighet: DatadelingRettighet,
    val opprettet: String,
    val barnetillegg: Barnetillegg?,
    val valgteHjemlerHarIkkeRettighet: List<DatadelingHjemmelHarIkkeRettighet>?,
    // Alle periodene dette vedtaket omgjør, på tvers av ett eller flere tidligere vedtak.
    // Listen er tom dersom vedtaket ikke omgjør noe.
    val omgjørRammevedtak: List<DatadelingOmgjøringsperiode>,
    // Alle periodene av dette vedtaket som er omgjort av senere vedtak, på tvers av ett eller flere vedtak.
    // Gjenspeiler tilstanden da vedtaket ble sendt, siden et vedtak ikke sendes på nytt når det omgjøres.
    // Listen er tom dersom vedtaket ikke er omgjort.
    val omgjortAvRammevedtak: List<DatadelingOmgjøringsperiode>,
) {
    data class DatadelingOmgjøringsperiode(
        val vedtakId: String,
        val periode: PeriodeDTO,
        val omgjøringsgrad: DatadelingOmgjøringsgrad,
    )

    enum class DatadelingOmgjøringsgrad {
        HELT,
        DELVIS,
    }

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

    enum class DatadelingHjemmelHarIkkeRettighet {
        DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK,
        ALDER,
        LIVSOPPHOLDSYTELSER,
        KVALIFISERINGSPROGRAMMET,
        INTRODUKSJONSPROGRAMMET,
        LONN_FRA_TILTAKSARRANGOR,
        LONN_FRA_ANDRE,
        INSTITUSJONSOPPHOLD,
        FREMMET_FOR_SENT,
        IKKE_LOVLIG_OPPHOLD,
        RETT_TIL_DAGPENGER,
    }

    enum class DatadelingRettighet {
        TILTAKSPENGER,
        TILTAKSPENGER_OG_BARNETILLEGG,
        STANS,
        AVSLAG,
        OPPHØR,
    }
}

fun Rammevedtak.toDatadelingJson(): String {
    return DatadelingVedtakJson(
        vedtakId = this.id.toString(),
        sakId = this.sakId.toString(),
        vedtaksperiode = periode.toDTO(),
        // TODO abn: burde vi sende periodiseringen istedenfor totalperioden?
        innvilgelsesperiode = innvilgelsesperioder?.totalPeriode?.toDTO(),
        // TODO jah: omgjørRammevedtakId og omgjortAvRammevedtakId bør gjøres om etter vi har lagt på eksplisitt omgjøring på vedtakene.
        // Kommentar jah: Disse ble lagt til utelukkende for revurdering til omgjøring for å tydeliggjøre at vedtaket omgjør et annet vedtak i sin helhet.
        // Det vil være en ny avgjørelse dersom vi skal dele informasjon fra andre vedtak her.
        // Ved omgjøring av flere vedtak sender vi det eldste, slik at kontrakten er bakoverkompatibel.
        // Den fullstendige oversikten ligger i [DatadelingVedtakJson.omgjørRammevedtak].
        omgjørRammevedtakId = if (this.erOmgjøringsbehandling) this.omgjørRammevedtak.rammevedtakIDer.first().toString() else null,
        // Kommentar jah: Hvis vi skulle beholdt dagens logikk her, måtte vi sjekket om rammevedtaket som omgjorde dette vedtaket var en omgjøringsbehandling.
        // Istedenfor å gjøre det, deler vi det vedtaket som har omgjort dette vedtaket helt.
        // Den fullstendige oversikten, også ved delvis omgjøring, ligger i [DatadelingVedtakJson.omgjortAvRammevedtak].
        omgjortAvRammevedtakId = if (this.omgjortAvRammevedtak.size == 1 && this.omgjortAvRammevedtak.first().omgjøringsgrad == Omgjøringsgrad.HELT) this.omgjortAvRammevedtak.first().rammevedtakId.toString() else null,
        rettighet = when (this.rammebehandlingsresultat) {
            is Rammebehandlingsresultat.Innvilgelse -> {
                if (barnetillegg?.harBarnetillegg == true) {
                    DatadelingRettighet.TILTAKSPENGER_OG_BARNETILLEGG
                } else {
                    DatadelingRettighet.TILTAKSPENGER
                }
            }

            is Revurderingsresultat.Stans -> DatadelingRettighet.STANS

            is Søknadsbehandlingsresultat.Avslag -> DatadelingRettighet.AVSLAG

            is Omgjøringsresultat.OmgjøringOpphør -> DatadelingRettighet.OPPHØR

            is Rammebehandlingsresultat.IkkeValgt -> this.rammebehandlingsresultat.vedtakError()
        },
        opprettet = opprettet.toString(),
        barnetillegg = barnetillegg?.toDatadelingBarnetillegg(),
        valgteHjemlerHarIkkeRettighet = this.toValgteHjemlerHarIkkeRettighetListe(),
        omgjørRammevedtak = this.omgjørRammevedtak.map { it.toDatadelingOmgjøringsperiode() },
        omgjortAvRammevedtak = this.omgjortAvRammevedtak.map { it.toDatadelingOmgjøringsperiode() },
    ).let { serialize(it) }
}

private fun Omgjøringsperiode.toDatadelingOmgjøringsperiode() = DatadelingVedtakJson.DatadelingOmgjøringsperiode(
    vedtakId = rammevedtakId.toString(),
    periode = periode.toDTO(),
    omgjøringsgrad = when (omgjøringsgrad) {
        Omgjøringsgrad.HELT -> DatadelingVedtakJson.DatadelingOmgjøringsgrad.HELT
        Omgjøringsgrad.DELVIS -> DatadelingVedtakJson.DatadelingOmgjøringsgrad.DELVIS
    },
)

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

private fun Rammevedtak.toValgteHjemlerHarIkkeRettighetListe(): List<DatadelingHjemmelHarIkkeRettighet>? {
    return when (this.rammebehandlingsresultat) {
        is Rammebehandlingsresultat.Innvilgelse -> null
        is Omgjøringsresultat.OmgjøringOpphør -> null
        is Revurderingsresultat.Stans -> (this.rammebehandling as Revurdering).toValgteHjemlerHarIkkeRettighetListe()
        is Søknadsbehandlingsresultat.Avslag -> (this.rammebehandling as Søknadsbehandling).toValgteHjemlerHarIkkeRettighetListe()
        is Rammebehandlingsresultat.IkkeValgt -> this.rammebehandlingsresultat.vedtakError()
    }
}

private fun Revurdering.toValgteHjemlerHarIkkeRettighetListe(): List<DatadelingHjemmelHarIkkeRettighet> =
    (this.resultat as Revurderingsresultat.Stans).valgtHjemmel?.map { it.toValgtHjemmelHarIkkeRettighetString() }
        ?: emptyList()

private fun Søknadsbehandling.toValgteHjemlerHarIkkeRettighetListe(): List<DatadelingHjemmelHarIkkeRettighet> =
    (this.resultat as Søknadsbehandlingsresultat.Avslag).avslagsgrunner.map { it.toValgtHjemmelHarIkkeRettighetString() }

private fun HjemmelForStans.toValgtHjemmelHarIkkeRettighetString(): DatadelingHjemmelHarIkkeRettighet =
    when (this) {
        HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak -> DatadelingHjemmelHarIkkeRettighet.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK
        HjemmelForStans.Alder -> DatadelingHjemmelHarIkkeRettighet.ALDER
        HjemmelForStans.Institusjonsopphold -> DatadelingHjemmelHarIkkeRettighet.INSTITUSJONSOPPHOLD
        HjemmelForStans.Introduksjonsprogrammet -> DatadelingHjemmelHarIkkeRettighet.INTRODUKSJONSPROGRAMMET
        HjemmelForStans.Kvalifiseringsprogrammet -> DatadelingHjemmelHarIkkeRettighet.KVALIFISERINGSPROGRAMMET
        HjemmelForStans.Livsoppholdytelser -> DatadelingHjemmelHarIkkeRettighet.LIVSOPPHOLDSYTELSER
        HjemmelForStans.LønnFraAndre -> DatadelingHjemmelHarIkkeRettighet.LONN_FRA_ANDRE
        HjemmelForStans.LønnFraTiltaksarrangør -> DatadelingHjemmelHarIkkeRettighet.LONN_FRA_TILTAKSARRANGOR
        HjemmelForStans.IkkeLovligOpphold -> DatadelingHjemmelHarIkkeRettighet.IKKE_LOVLIG_OPPHOLD
        HjemmelForStans.RettTilDagpenger -> DatadelingHjemmelHarIkkeRettighet.RETT_TIL_DAGPENGER
    }

private fun Avslagsgrunnlag.toValgtHjemmelHarIkkeRettighetString(): DatadelingHjemmelHarIkkeRettighet =
    when (this) {
        Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak -> DatadelingHjemmelHarIkkeRettighet.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK
        Avslagsgrunnlag.Alder -> DatadelingHjemmelHarIkkeRettighet.ALDER
        Avslagsgrunnlag.FremmetForSent -> DatadelingHjemmelHarIkkeRettighet.FREMMET_FOR_SENT
        Avslagsgrunnlag.Institusjonsopphold -> DatadelingHjemmelHarIkkeRettighet.INSTITUSJONSOPPHOLD
        Avslagsgrunnlag.Introduksjonsprogrammet -> DatadelingHjemmelHarIkkeRettighet.INTRODUKSJONSPROGRAMMET
        Avslagsgrunnlag.Kvalifiseringsprogrammet -> DatadelingHjemmelHarIkkeRettighet.KVALIFISERINGSPROGRAMMET
        Avslagsgrunnlag.Livsoppholdytelser -> DatadelingHjemmelHarIkkeRettighet.LIVSOPPHOLDSYTELSER
        Avslagsgrunnlag.LønnFraAndre -> DatadelingHjemmelHarIkkeRettighet.LONN_FRA_ANDRE
        Avslagsgrunnlag.LønnFraTiltaksarrangør -> DatadelingHjemmelHarIkkeRettighet.LONN_FRA_TILTAKSARRANGOR
        Avslagsgrunnlag.RettTilDagpenger -> DatadelingHjemmelHarIkkeRettighet.RETT_TIL_DAGPENGER
    }
