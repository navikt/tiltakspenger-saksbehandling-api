package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans

import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Prioritering
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

/**
 * Har oppdragssystemet utbetalt flere dager enn dette etter at deltakelsen ble avbrutt, opphøres tiltakspengene i stedet for å stanses.
 * En stans kan ikke gi feilutbetaling, og ville blitt flyttet forbi alle de utbetalte dagene.
 */
const val MAKS_UTBETALTE_DAGER_FOR_AUTOMATISK_STANS = 5

/**
 * Om en avbrutt deltakelse skal stanses automatisk, eller opphøres og overlates til en saksbehandler.
 * Se [vurderOpphørIStedenforStans].
 */
sealed interface AutomatiskStansEllerOpphør {
    data object Stans : AutomatiskStansEllerOpphør

    /**
     * Det systemet har utledet for å fylle ut opphøret.
     * Opphøret behandles ikke automatisk, men overlates til en saksbehandler når det er fylt ut.
     *
     * @param opphørsperiode Fra og med dagen etter sluttdatoen, til og med siste innvilgede dag.
     */
    data class Opphør(
        val opphørsperiode: Periode,
        val omgjørRammevedtak: Rammevedtak,
        val hjemmel: HjemmelForOpphør,
        val begrunnelse: Begrunnelse,
        val prioritering: Prioritering,
    ) : AutomatiskStansEllerOpphør

    /** Opphøret ville omgjort mer enn ett vedtak, og må følges opp manuelt. */
    data object OpphørOmgjørFlereVedtak : AutomatiskStansEllerOpphør
}

/**
 * Vurderer om stansen skal erstattes av et opphør, ut fra simuleringen av stansen fra dagen etter sluttdatoen.
 * Simuleringen sier hvor langt oppdragssystemet har utbetalt ([sisteDagMedFeilutbetaling]), og dagene med beløp i de gjeldende beregningene sier hvilke av dagene som er utbetalingsdager.
 * Er flere enn [MAKS_UTBETALTE_DAGER_FOR_AUTOMATISK_STANS] av dagene i [stansperiode] utbetalt, gis et opphør av hele [stansperiode].
 * Ellers stanses deltakelsen automatisk, og stansdatoen flyttes forbi de utbetalte dagene, se [utledAutomatiskStans].
 *
 * @param stansperiode Perioden en stans fra dagen etter sluttdatoen ville gjeldt for.
 */
fun Sak.vurderOpphørIStedenforStans(
    stansperiode: Periode,
    sisteDagMedFeilutbetaling: LocalDate?,
): AutomatiskStansEllerOpphør {
    if (sisteDagMedFeilutbetaling == null || sisteDagMedFeilutbetaling.isBefore(stansperiode.fraOgMed)) {
        return AutomatiskStansEllerOpphør.Stans
    }

    val utbetalteDager = meldeperiodeBeregninger.antallDagerMedUtbetaling(
        stansperiode.fraOgMed til minOf(sisteDagMedFeilutbetaling, stansperiode.tilOgMed),
    )
    if (utbetalteDager <= MAKS_UTBETALTE_DAGER_FOR_AUTOMATISK_STANS) {
        return AutomatiskStansEllerOpphør.Stans
    }

    val omgjørRammevedtak = vedtaksliste.finnRammevedtakSomOmgjøres(stansperiode).rammevedtakIDer.singleOrNull()
        ?.let { rammevedtaksliste.hentRammevedtakForId(it) }
        ?: return AutomatiskStansEllerOpphør.OpphørOmgjørFlereVedtak

    val fraOgMed = stansperiode.fraOgMed.format(norskDatoFormatter)
    return AutomatiskStansEllerOpphør.Opphør(
        opphørsperiode = stansperiode,
        omgjørRammevedtak = omgjørRammevedtak,
        hjemmel = HjemmelForOpphør.DeltarIkkePåArbeidsmarkedstiltak,
        begrunnelse = Begrunnelse.create(
            "Automatisk opprettet. Tiltaksdeltakelsen er avbrutt, og tiltakspengene opphøres fra og med $fraOgMed fordi bruker ikke lenger deltar på arbeidsmarkedstiltak. " +
                "Oppdragssystemet har allerede utbetalt tiltakspenger for $utbetalteDager dager fra og med $fraOgMed, og utbetalingen for disse dagene blir feilutbetaling.",
        )!!,
        prioritering = Prioritering.create(
            "Opphør må behandles omgående for å begrense feilutbetaling. " +
                "Oppdragssystemet har allerede utbetalt tiltakspenger for $utbetalteDager dager etter at deltakelsen ble avbrutt.",
        )!!,
    )
}
