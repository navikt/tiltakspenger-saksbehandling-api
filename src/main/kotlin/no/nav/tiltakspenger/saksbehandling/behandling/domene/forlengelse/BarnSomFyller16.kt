package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

/**
 * Barn som fyller 16 år i en forlengelse, slik at barnetillegget stopper for dem.
 * Forklares for bruker i vedtaksbrevet.
 *
 * @param antall Hvor mange av barna i vedtaket som forlenges, som fyller 16 år i forlengelsen.
 * @param antallBarnIVedtaket Antall barn med barnetillegg på siste innvilgede dag i vedtaket som forlenges.
 * @param sisteDagMedBarnetillegg Siste dag med barnetillegg for minst ett barn.
 * Er det siste innvilgede dag i vedtaket som forlenges, har forlengelsen ikke barnetillegg.
 */
data class BarnSomFyller16(
    val antall: Int,
    val antallBarnIVedtaket: Int,
    val sisteDagMedBarnetillegg: LocalDate,
) {
    companion object {
        /**
         * Barn kan bare falle bort når barnetillegget er utledet, så antall barn som fyller 16 år er differansen mellom vedtaket og slutten av forlengelsen.
         * Er det ingen slik differanse, returneres null.
         */
        fun utled(
            sisteInnvilgedeDag: LocalDate,
            antallBarnIVedtaket: AntallBarn,
            antallBarn: IkkeTomPeriodisering<AntallBarn>,
        ): BarnSomFyller16? {
            val antall = antallBarnIVedtaket.value - antallBarn.perioderMedVerdi.last().verdi.value
            if (antall <= 0) return null

            return BarnSomFyller16(
                antall = antall,
                antallBarnIVedtaket = antallBarnIVedtaket.value,
                sisteDagMedBarnetillegg = antallBarn.perioderMedVerdi.lastOrNull { it.verdi != AntallBarn.ZERO }?.periode?.tilOgMed
                    ?: sisteInnvilgedeDag,
            )
        }
    }
}

/**
 * Barn som fyller 16 år i forlengelsen av [forlengetVedtak], gitt barnetillegget [antallBarn] som er utledet for forlengelsen.
 * Barnetillegg saksbehandler har endret, kan avvike fra vedtaket av andre grunner, og skal derfor ikke brukes her.
 */
fun ForlengerRammevedtak.barnSomFyller16(
    forlengetVedtak: Rammevedtak,
    antallBarn: IkkeTomPeriodisering<AntallBarn>,
): BarnSomFyller16? {
    require(forlengetVedtak.id == rammevedtakId) {
        "Forlengelsen forlenger $rammevedtakId, men fikk vedtaket ${forlengetVedtak.id}"
    }
    return BarnSomFyller16.utled(
        sisteInnvilgedeDag = sisteInnvilgedeDag,
        antallBarnIVedtaket = forlengetVedtak.barnetillegg?.periodisering?.hentVerdiForDag(sisteInnvilgedeDag) ?: AntallBarn.ZERO,
        antallBarn = antallBarn,
    )
}
