package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.OppsummeringGenerator
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.LocalDate

/**
 * Kommentar jah: Ser ikke simuleringstypene i kontrakter: https://github.com/navikt/utsjekk-kontrakter/
 * Se også: https://github.com/navikt/helved-utbetaling/blob/main/apps/utsjekk/main/utsjekk/simulering/SimuleringDto.kt#L90
 */
private data class SimuleringResponseDTO(
    val oppsummeringer: List<OppsummeringForPeriode>,
    val detaljer: SimuleringDetaljer,
) {

    /**
     * Se også: Se også: https://github.com/navikt/helved-utbetaling/blob/main/apps/utsjekk/main/utsjekk/simulering/SimuleringDto.kt#L95
     */
    data class OppsummeringForPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
        val tidligereUtbetalt: Int,
        val nyUtbetaling: Int,
        val totalEtterbetaling: Int,
        val totalFeilutbetaling: Int,
    )

    data class SimuleringDetaljer(
        val gjelderId: String,
        val datoBeregnet: LocalDate,
        val totalBeløp: Int,
        val perioder: List<Periode>,
    ) {
        data class Periode(
            val fom: LocalDate,
            val tom: LocalDate,
            val posteringer: List<Postering>,
        ) {
            data class Postering(
                val fagområde: String,
                val sakId: String,
                val fom: LocalDate,
                val tom: LocalDate,
                val beløp: Int,
                val type: String,
                val klassekode: String,
            ) {
                fun toDomain(): Simulering.Endring.Detaljer.Simuleringsperiode.Delperiode {
                    return Simulering.Endring.Detaljer.Simuleringsperiode.Delperiode(
                        fagområde = this.fagområde,
                        periode = Periode(this.fom, this.tom),
                        beløp = this.beløp,
                        type = typeToDomain(),
                        klassekode = this.klassekode,
                    )
                }

                private fun typeToDomain(): Simulering.Endring.PosteringType {
                    return when (type) {
                        "YTELSE" -> Simulering.Endring.PosteringType.YTELSE
                        "FEILUTBETALING" -> Simulering.Endring.PosteringType.FEILUTBETALING
                        "FORSKUDSSKATT" -> Simulering.Endring.PosteringType.FORSKUDSSKATT
                        "JUSTERING" -> Simulering.Endring.PosteringType.JUSTERING
                        "TREKK" -> Simulering.Endring.PosteringType.TREKK
                        "MOTPOSTERING" -> Simulering.Endring.PosteringType.MOTPOSTERING
                        else -> error("Ukjent posteringstype: $type")
                    }
                }
            }
        }
    }
}

fun String.toSimulering(
    validerSaksnummer: Saksnummer,
    validerFnr: Fnr,
    meldeperiodeKjeder: MeldeperiodeKjeder,
): Simulering.Endring {
    return deserialize<SimuleringResponseDTO>(this).let { res ->
        check(Fnr.fromString(res.detaljer.gjelderId) == validerFnr) {
            "Simulering sin gjelderId: ${res.detaljer.gjelderId} er ulik behandlingens fnr $validerFnr"
        }
        // TODO jah: Her må vi nok filtrere på vårt fagområde. Antar vi kan få utbetalinger fra flere enn vårt fagområde.
//        res.detaljer.perioder.flatMap { it.posteringer }.map { Saksnummer(it.sakId) }.distinct().let {
//            check(it.size == 1 && it.first() == validerSaksnummer) {
//                "Simulering sin sakId: ${it.joinToString()} er ulik behandlingens saksnummer $validerSaksnummer"
//            }
//        }
        val detaljer = Simulering.Endring.Detaljer(
            datoBeregnet = res.detaljer.datoBeregnet,
            totalBeløp = res.detaljer.totalBeløp,
            perioder = res.detaljer.perioder.map { periode ->
                Simulering.Endring.Detaljer.Simuleringsperiode(
                    periode = Periode(periode.fom, periode.tom),
                    delperioder = periode.posteringer.map { postering ->
                        postering.toDomain()
                    },
                )
            }.toNonEmptyListOrNull()!!,
        )
        Simulering.Endring(
            detaljer = detaljer,
            oppsummering = OppsummeringGenerator.lagOppsummering(detaljer, meldeperiodeKjeder),
        )
    }
}
