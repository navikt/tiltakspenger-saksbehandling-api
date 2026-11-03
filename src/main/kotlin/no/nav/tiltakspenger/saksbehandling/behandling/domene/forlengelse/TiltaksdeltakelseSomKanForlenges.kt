package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import java.time.LocalDate

/**
 * En tiltaksdeltakelse saksbehandler kan forlenge vedtaket for.
 *
 * @param tiltaksdeltakelse Deltakelsen slik den er i saksopplysningene til forlengelsen.
 * @param antallDagerPerMeldeperiode Antall dager i den siste perioden vedtaket innvilget for deltakelsen, som videreføres i forlengelsen.
 */
data class TiltaksdeltakelseSomKanForlenges(
    val tiltaksdeltakelse: TiltaksdeltakelseIntern,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
)

/**
 * Tiltaksdeltakelsene i vedtaket [forlenger] forlenger, som forlengelsen kan innvilges for.
 * Se [tiltaksdeltakelserSomKanForlenges].
 */
fun Sak.tiltaksdeltakelserSomKanForlenges(
    forlenger: ForlengerRammevedtak,
    saksopplysninger: Saksopplysninger,
): List<TiltaksdeltakelseSomKanForlenges> {
    return hentRammevedtakForId(forlenger.rammevedtakId).innvilgelsesperioder!!.tiltaksdeltakelserSomKanForlenges(
        forlengesFraOgMed = forlenger.forlengesFraOgMed,
        saksopplysninger = saksopplysninger,
    )
}

/**
 * Tiltaksdeltakelsene i innvilgelsesperiodene til vedtaket som forlenges, der deltakelsen i [saksopplysninger] fortsatt varer [forlengesFraOgMed].
 * Sortert etter når deltakelsen sist var innvilget i vedtaket, slik at deltakelsen fra slutten av vedtaket kommer først.
 */
fun Innvilgelsesperioder.tiltaksdeltakelserSomKanForlenges(
    forlengesFraOgMed: LocalDate,
    saksopplysninger: Saksopplysninger,
): List<TiltaksdeltakelseSomKanForlenges> {
    return periodisering.perioderMedVerdi
        .sortedByDescending { it.periode.tilOgMed }
        .distinctBy { it.verdi.valgtTiltaksdeltakelse.internDeltakelseId }
        .mapNotNull { innvilgelsesperiode ->
            saksopplysninger.getTiltaksdeltakelse(innvilgelsesperiode.verdi.valgtTiltaksdeltakelse.internDeltakelseId)
                ?.takeIf { it.lukketPeriode?.inneholder(forlengesFraOgMed) == true }
                ?.let { TiltaksdeltakelseSomKanForlenges(it, innvilgelsesperiode.verdi.antallDagerPerMeldeperiode) }
        }
}
