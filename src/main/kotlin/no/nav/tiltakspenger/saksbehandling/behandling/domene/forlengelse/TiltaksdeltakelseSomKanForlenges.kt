package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import java.time.LocalDate

/**
 * Tiltaksdeltakelsene i vedtaket [forlenger] forlenger, som forlengelsen kan innvilges for, slik de er i saksopplysningene til forlengelsen.
 * Se [tiltaksdeltakelserSomKanForlenges].
 */
fun Sak.tiltaksdeltakelserSomKanForlenges(
    forlenger: ForlengerRammevedtak,
    saksopplysninger: Saksopplysninger,
): List<TiltaksdeltakelseIntern> {
    return hentRammevedtakForId(forlenger.rammevedtakId).innvilgelsesperioder!!.tiltaksdeltakelserSomKanForlenges(
        forlengesFraOgMed = forlenger.forlengesFraOgMed,
        saksopplysninger = saksopplysninger,
    )
}

/**
 * Tiltaksdeltakelsene i innvilgelsesperiodene til vedtaket som forlenges, der deltakelsen i [saksopplysninger] fortsatt kan innvilges og varer [forlengesFraOgMed].
 * En deltakelse som for eksempel er feilregistrert eller ikke lenger aktuell, kan ikke forlenges selv om datoene dekker forlengelsen.
 * Sortert etter når deltakelsen sist var innvilget i vedtaket, slik at deltakelsen fra slutten av vedtaket kommer først.
 * Deltakelsene er slik de er i [saksopplysninger].
 */
fun Innvilgelsesperioder.tiltaksdeltakelserSomKanForlenges(
    forlengesFraOgMed: LocalDate,
    saksopplysninger: Saksopplysninger,
): List<TiltaksdeltakelseIntern> {
    return periodisering.perioderMedVerdi
        .sortedByDescending { it.periode.tilOgMed }
        .flatMap { (verdi) -> verdi.tiltaksdeltakelser.map { it.internDeltakelseId } }
        .distinct()
        .mapNotNull { internDeltakelseId ->
            saksopplysninger.getTiltaksdeltakelse(internDeltakelseId)
                ?.takeIf { it.kanInnvilges && it.lukketPeriode?.inneholder(forlengesFraOgMed) == true }
        }
}
