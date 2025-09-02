package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.maksAntallDager
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtakstype
import java.time.LocalDate

private data class DatadelingVedtakJson(
    val vedtakId: String,
    val sakId: String,
    val saksnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val antallDagerPerMeldeperiode: Int,
    val rettighet: String,
    val fnr: String,
    val opprettet: String,
    val barnetillegg: Barnetillegg?,
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
}

fun Rammevedtak.toDatadelingJson(): String {
    return DatadelingVedtakJson(
        vedtakId = this.id.toString(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.verdi,
        fom = periode.fraOgMed,
        tom = periode.tilOgMed,
        // TODO abn: sett en periodisering istedenfor bare maks. Evt fjern dersom denne ikke deles med noen.
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode.maksAntallDager(),
        rettighet = when (this.vedtakstype) {
            Vedtakstype.INNVILGELSE -> {
                if (barnetillegg != null) {
                    "TILTAKSPENGER_OG_BARNETILLEGG"
                } else {
                    "TILTAKSPENGER"
                }
            }
            Vedtakstype.STANS -> "INGENTING"
            // Denne skal helst ikke bli truffet da servicen ikke skal prøve å sende for avlsag
            Vedtakstype.AVSLAG -> throw IllegalArgumentException("Vi dropper sende noe til datadeling nå for avslag")
        },
        fnr = fnr.verdi,
        opprettet = opprettet.toString(),
        barnetillegg = barnetillegg?.toDatadelingBarnetillegg(),
    ).let { serialize(it) }
}

private fun Barnetillegg.toDatadelingBarnetillegg() =
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
