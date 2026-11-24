package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDate

/**
 * Har oppdragssystemet utbetalt flere dager enn dette etter at deltakelsen ble avbrutt, må tiltakspengene opphøres i stedet for å stanses.
 * En stans kan ikke gi feilutbetaling, og ville blitt flyttet forbi alle de utbetalte dagene.
 */
const val MAKS_UTBETALTE_DAGER_FOR_AUTOMATISK_STANS = 5

/**
 * Om en avbrutt deltakelse skal stanses automatisk, eller opphøres av en saksbehandler.
 * Se [vurderOpphørIStedenforStans].
 */
sealed interface AutomatiskStansEllerOpphør {
    data object Stans : AutomatiskStansEllerOpphør

    /**
     * Systemet oppretter en omgjøring av [omgjørRammevedtak] klar til behandling, og saksbehandler fyller ut opphøret.
     * Omgjøringen fylles ikke ut og behandles ikke automatisk.
     */
    data class Opphør(
        val omgjørRammevedtak: OmgjørRammevedtak,
    ) : AutomatiskStansEllerOpphør
}

/**
 * Vurderer om stansen skal erstattes av et opphør, ut fra simuleringen av stansen fra dagen etter sluttdatoen.
 * Simuleringen sier hvor langt oppdragssystemet har utbetalt ([sisteDagMedFeilutbetaling]), og dagene med beløp i de gjeldende beregningene sier hvilke av dagene som er utbetalingsdager.
 * Er flere enn [MAKS_UTBETALTE_DAGER_FOR_AUTOMATISK_STANS] av dagene i [stansperiode] utbetalt, må saksbehandler opphøre i stedet.
 * Berører [stansperiode] bare ett vedtak, omgjøres hele det vedtaket.
 * Berører den flere vedtak, omgjøres periodene [stansperiode] dekker i dem, slik at omgjøringen kan forhåndsutfylles med opphørsperioden.
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

    val omgjøringsperioder = vedtaksliste.finnRammevedtakSomOmgjøres(stansperiode)
    val omgjørRammevedtak = when (val rammevedtakId = omgjøringsperioder.rammevedtakIDer.singleOrNull()) {
        null -> omgjøringsperioder
        else -> OmgjørRammevedtak.create(rammevedtaksliste.hentRammevedtakForId(rammevedtakId))
    }

    return AutomatiskStansEllerOpphør.Opphør(omgjørRammevedtak = omgjørRammevedtak)
}
