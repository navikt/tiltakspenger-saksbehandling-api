package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseLegacy
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndringer.Companion.tilEndringer
import java.time.Clock
import java.time.LocalDate

fun Sak.finnTiltaksdeltakerStatus(tiltaksdeltakerId: TiltaksdeltakerId) {
    rammevedtaksliste
}

/**
 * Sammenligner nå-tilstanden fra tiltakshistorikk med den ferskeste kjente tilstanden for deltakelsen i saken.
 * Gir null dersom deltakelsen ikke er kjent fra noen vedtatt eller åpen manuell behandling, eller dersom ingenting er endret.
 */
fun Sak.finnEndringer(
    tiltaksdeltakerId: TiltaksdeltakerId,
    nåtilstand: TiltaksdeltakelseLegacy,
    clock: Clock,
): TiltaksdeltakerEndringer? {
    val kjentTilstand = finnSisteRelevanteTiltaksdeltakelse(tiltaksdeltakerId, clock) ?: return null
    return finnEndringer(nåtilstand, kjentTilstand, clock)
}

/**
 * Ignorerer vedtak som allerede er stanset eller opphørt i relevant periode.
 * Utløpte innvilgelser er fortsatt relevante dersom det var rett på den opprinnelige sluttdatoen.
 */
internal fun Sak.finnSisteRelevanteTiltaksdeltakelse(
    tiltaksdeltakerId: TiltaksdeltakerId,
    clock: Clock,
): TiltaksdeltakelseIntern? {
    val vedtatteBehandlingerMedRelevantTiltaksdeltakelse = rammevedtaksliste.innvilgetTidslinje.verdier
        .filter { vedtak ->
            val harInnvilgetForTiltaket = vedtak.valgteTiltaksdeltakelser!!.any {
                it.verdi.internDeltakelseId == tiltaksdeltakerId
            }

            val harRettIRelevantPeriode by lazy {
                val sisteInnvilgetDato = vedtak.innvilgelsesperioder!!.tilOgMed
                val dagensDato = LocalDate.now(clock)

                if (sisteInnvilgetDato.isBefore(dagensDato)) {
                    rammevedtaksliste.harInnvilgetTiltakspengerPåDato(sisteInnvilgetDato)
                } else {
                    rammevedtaksliste.innvilgelsesperioder.overlapper(dagensDato til sisteInnvilgetDato)
                }
            }

            harInnvilgetForTiltaket && harRettIRelevantPeriode
        }
        .map { it.rammebehandling }

    val åpneBehandlingerMedRelevantTiltaksdeltakelse = rammebehandlinger.åpneBehandlinger
        .filter { !it.erUnderAutomatiskBehandling && it.getTiltaksdeltakelse(tiltaksdeltakerId) != null }

    val behandlingerMedRelevantTiltaksdeltakelse = vedtatteBehandlingerMedRelevantTiltaksdeltakelse
        .plus(åpneBehandlingerMedRelevantTiltaksdeltakelse)

    if (behandlingerMedRelevantTiltaksdeltakelse.isEmpty()) {
        return null
    }

    return behandlingerMedRelevantTiltaksdeltakelse
        .maxBy { it.sistEndret }
        .getTiltaksdeltakelse(tiltaksdeltakerId)!!
}

/**
 * Finner endringene mellom nå-tilstanden fra tiltakshistorikk og tilstanden saken kjenner til.
 * Gir null dersom ingenting relevant er endret.
 */
private fun finnEndringer(
    nåtilstand: TiltaksdeltakelseLegacy,
    kjentTilstand: TiltaksdeltakelseLegacy,
    clock: Clock,
): TiltaksdeltakerEndringer? {
    val endringer = mutableListOf<TiltaksdeltakerEndring>()
    val sammeFom = nåtilstand.deltakelseFraOgMed == kjentTilstand.deltakelseFraOgMed
    val sammeTom = nåtilstand.deltakelseTilOgMed == kjentTilstand.deltakelseTilOgMed

    val sammeAntallDagerPerUke = floatIsEqual(nåtilstand.antallDagerPerUke, kjentTilstand.antallDagerPerUke)
    val sammeDeltakelsesprosent = floatIsEqual(nåtilstand.deltakelseProsent, kjentTilstand.deltakelseProsent)
    val sammeStatus = nåtilstand.deltakelseStatus == kjentTilstand.deltakelseStatus

    if (sammeFom &&
        sammeTom &&
        sammeAntallDagerPerUke &&
        sammeDeltakelsesprosent &&
        (sammeStatus || deltakelsenErAvsluttetSomForventet(nåtilstand, clock))
    ) {
        return null
    }

    if (erAvbruttDeltakelse(nåtilstand, sammeStatus, sammeTom, kjentTilstand, clock)) {
        endringer.add(TiltaksdeltakerEndring.AvbruttDeltakelse)
        return endringer.tilEndringer()
    }

    if (!sammeStatus && nåtilstand.deltakelseStatus == TiltakDeltakerstatus.IkkeAktuell) {
        endringer.add(TiltaksdeltakerEndring.IkkeAktuellDeltakelse)
        return endringer.tilEndringer()
    }

    if (!sammeDeltakelsesprosent || !sammeAntallDagerPerUke) {
        endringer.add(
            TiltaksdeltakerEndring.EndretDeltakelsesmengde(
                nåtilstand.deltakelseProsent,
                nåtilstand.antallDagerPerUke,
            ),
        )
    }

    if (erForlengelse(nåtilstand, sammeFom, kjentTilstand)) {
        endringer.add(TiltaksdeltakerEndring.Forlengelse(nåtilstand.deltakelseTilOgMed!!))
        return endringer.tilEndringer()
    }

    if (!sammeFom) {
        endringer.add(TiltaksdeltakerEndring.EndretStartdato(nåtilstand.deltakelseFraOgMed))
    }
    if (!sammeTom) {
        endringer.add(TiltaksdeltakerEndring.EndretSluttdato(nåtilstand.deltakelseTilOgMed))
    }
    if (!sammeStatus) {
        endringer.add(TiltaksdeltakerEndring.EndretStatus(nåtilstand.deltakelseStatus))
    }

    return endringer.tilEndringer()
}

private fun floatIsEqual(a: Float?, b: Float?): Boolean {
    return if (a == null && b == 0F) {
        true
    } else if (b == null && a == 0F) {
        true
    } else {
        compareValues(a, b) == 0
    }
}

private fun deltakelsenErAvsluttetSomForventet(
    nåtilstand: TiltaksdeltakelseLegacy,
    clock: Clock,
): Boolean {
    // Interface-propertyen kan ikke smart-castes, så den bindes lokalt først.
    val tilOgMed = nåtilstand.deltakelseTilOgMed
    return (
        (nåtilstand.deltakelseStatus == TiltakDeltakerstatus.HarSluttet || nåtilstand.deltakelseStatus == TiltakDeltakerstatus.Fullført) &&
            tilOgMed != null &&
            !tilOgMed.isAfter(LocalDate.now(clock))
        )
}

private fun erAvbruttDeltakelse(
    nåtilstand: TiltaksdeltakelseLegacy,
    sammeStatus: Boolean,
    sammeTom: Boolean,
    kjentTilstand: TiltaksdeltakelseLegacy,
    clock: Clock,
): Boolean {
    val statusEndretTilAvbrutt = !sammeStatus && nåtilstand.deltakelseStatus == TiltakDeltakerstatus.Avbrutt
    if (statusEndretTilAvbrutt) return true

    return !sammeTom && erSluttdatoAvkortetTilFortiden(nåtilstand, kjentTilstand, clock)
}

private fun erSluttdatoAvkortetTilFortiden(
    nåtilstand: TiltaksdeltakelseLegacy,
    kjentTilstand: TiltaksdeltakelseLegacy,
    clock: Clock,
): Boolean {
    val nySluttdato = nåtilstand.deltakelseTilOgMed ?: return false
    val gammelSluttdato = kjentTilstand.deltakelseTilOgMed

    // En null tilOgMed i kjent tilstand betyr en åpen/uavsluttet deltakelse.
    // Å sette en tilOgMed i fortiden er da en avkorting og regnes som avbrutt.
    val erAvkortet = gammelSluttdato == null || nySluttdato.isBefore(gammelSluttdato)
    val erIFortiden = !nySluttdato.isAfter(LocalDate.now(clock))
    return erAvkortet && erIFortiden
}

// En null tilOgMed i kjent tilstand betyr en åpen/uavsluttet deltakelse.
// Å sette en sluttdato på en åpen deltakelse er en innskrenking, ikke en forlengelse.
private fun erForlengelse(
    nåtilstand: TiltaksdeltakelseLegacy,
    sammeFom: Boolean,
    kjentTilstand: TiltaksdeltakelseLegacy,
): Boolean {
    val gammelSluttdato = kjentTilstand.deltakelseTilOgMed ?: return false
    return sammeFom && nåtilstand.deltakelseTilOgMed?.isAfter(gammelSluttdato) == true
}
