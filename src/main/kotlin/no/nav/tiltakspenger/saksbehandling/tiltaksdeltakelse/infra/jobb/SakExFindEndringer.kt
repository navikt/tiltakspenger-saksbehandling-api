package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseLegacy
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.Clock
import java.time.LocalDate

/**
 * Sammenligner nå-tilstanden fra tiltakshistorikk med den ferskeste kjente tilstanden for deltakelsen i saken, og tolker forskjellen til ett utfall.
 * Gir null dersom deltakelsen ikke er kjent fra noen vedtatt eller åpen manuell behandling, eller dersom ingenting er endret.
 */
fun Sak.finnEndringer(
    tiltaksdeltakerId: TiltaksdeltakerId,
    oppdatertDeltakelse: TiltaksdeltakelseLegacy,
    clock: Clock,
): TiltaksdeltakerEndring? {
    val kjentTilstand = finnSisteRelevanteTiltaksdeltakelse(tiltaksdeltakerId, clock) ?: return null
    return kjentTilstand.finnEndringer(oppdatertDeltakelse, clock)
}

/**
 * Ignorerer vedtak som allerede er stanset eller opphørt i relevant periode.
 * Utløpte innvilgelser er fortsatt relevante dersom det var rett på den opprinnelige sluttdatoen.
 */
private fun Sak.finnSisteRelevanteTiltaksdeltakelse(
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
 * Tolker endringene mellom nå-tilstanden fra tiltakshistorikk og tilstanden saken kjenner til.
 * Utfallene prioriteres i rekkefølgen avbrutt, ikke aktuell, forlengelse og andre endringer.
 * Avsluttet som forventet gis bare når statusendringen er den eneste endringen.
 * Gir null dersom ingenting relevant er endret.
 */
private fun TiltaksdeltakelseLegacy.finnEndringer(
    oppdatertDeltakelse: TiltaksdeltakelseLegacy,
    clock: Clock,
): TiltaksdeltakerEndring? {
    return when {
        erUendret(oppdatertDeltakelse) -> null

        erAvsluttetSomForventet(oppdatertDeltakelse, clock) -> TiltaksdeltakerEndring.AvsluttetSomForventet

        erAvbruttDeltakelse(oppdatertDeltakelse, clock) -> TiltaksdeltakerEndring.AvbruttDeltakelse

        erIkkeAktuellDeltakelse(oppdatertDeltakelse) -> TiltaksdeltakerEndring.IkkeAktuellDeltakelse

        erForlengelse(oppdatertDeltakelse) -> TiltaksdeltakerEndring.Forlengelse(
            nySluttdato = oppdatertDeltakelse.deltakelseTilOgMed!!,
            endretDeltakelsesmengde = endretDeltakelsesmengde(oppdatertDeltakelse),
        )

        else -> TiltaksdeltakerEndring.AndreEndringer(
            endretDeltakelsesmengde = endretDeltakelsesmengde(oppdatertDeltakelse),
            endretStartdato = endretStartdato(oppdatertDeltakelse),
            endretSluttdato = endretSluttdato(oppdatertDeltakelse),
            endretStatus = endretStatus(oppdatertDeltakelse),
        )
    }
}

private fun TiltaksdeltakelseLegacy.harSammeFraOgMed(oppdatertDeltakelse: TiltaksdeltakelseLegacy): Boolean {
    return deltakelseFraOgMed == oppdatertDeltakelse.deltakelseFraOgMed
}

private fun TiltaksdeltakelseLegacy.harSammeTilOgMed(oppdatertDeltakelse: TiltaksdeltakelseLegacy): Boolean {
    return deltakelseTilOgMed == oppdatertDeltakelse.deltakelseTilOgMed
}

private fun TiltaksdeltakelseLegacy.harSammeStatus(oppdatertDeltakelse: TiltaksdeltakelseLegacy): Boolean {
    return deltakelseStatus == oppdatertDeltakelse.deltakelseStatus
}

private fun TiltaksdeltakelseLegacy.harSammeDeltakelsesmengde(oppdatertDeltakelse: TiltaksdeltakelseLegacy): Boolean {
    return compareValues(deltakelseProsent ?: 0F, oppdatertDeltakelse.deltakelseProsent ?: 0F) == 0 &&
        compareValues(antallDagerPerUke ?: 0F, oppdatertDeltakelse.antallDagerPerUke ?: 0F) == 0
}

private fun TiltaksdeltakelseLegacy.erUendret(oppdatertDeltakelse: TiltaksdeltakelseLegacy): Boolean {
    return harSammeFraOgMed(oppdatertDeltakelse) &&
        harSammeTilOgMed(oppdatertDeltakelse) &&
        harSammeDeltakelsesmengde(oppdatertDeltakelse) &&
        harSammeStatus(oppdatertDeltakelse)
}

/**
 * Gjelder bare når statusen er den eneste endringen, og den nye statusen er en forventet avslutning med sluttdato i dag eller tidligere.
 */
private fun TiltaksdeltakelseLegacy.erAvsluttetSomForventet(
    oppdatertDeltakelse: TiltaksdeltakelseLegacy,
    clock: Clock,
): Boolean {
    val bareStatusErEndret = harSammeFraOgMed(oppdatertDeltakelse) &&
        harSammeTilOgMed(oppdatertDeltakelse) &&
        harSammeDeltakelsesmengde(oppdatertDeltakelse) &&
        !harSammeStatus(oppdatertDeltakelse)

    if (!bareStatusErEndret) {
        return false
    }

    val nyStatusErAvsluttet = oppdatertDeltakelse.deltakelseStatus == TiltakDeltakerstatus.HarSluttet ||
        oppdatertDeltakelse.deltakelseStatus == TiltakDeltakerstatus.Fullført
    val nySluttdato = oppdatertDeltakelse.deltakelseTilOgMed

    return nyStatusErAvsluttet && nySluttdato != null && !nySluttdato.isAfter(LocalDate.now(clock))
}

private fun TiltaksdeltakelseLegacy.erAvbruttDeltakelse(
    oppdatertDeltakelse: TiltaksdeltakelseLegacy,
    clock: Clock,
): Boolean {
    val statusEndretTilAvbrutt = !harSammeStatus(oppdatertDeltakelse) &&
        oppdatertDeltakelse.deltakelseStatus == TiltakDeltakerstatus.Avbrutt
    return statusEndretTilAvbrutt || erSluttdatoAvkortetTilFortiden(oppdatertDeltakelse, clock)
}

private fun TiltaksdeltakelseLegacy.erSluttdatoAvkortetTilFortiden(
    oppdatertDeltakelse: TiltaksdeltakelseLegacy,
    clock: Clock,
): Boolean {
    val nySluttdato = oppdatertDeltakelse.deltakelseTilOgMed ?: return false
    val gammelSluttdato = deltakelseTilOgMed

    // En null tilOgMed i kjent tilstand betyr en åpen/uavsluttet deltakelse.
    // Å sette en tilOgMed i fortiden er da en avkorting og regnes som avbrutt.
    val erAvkortet = gammelSluttdato == null || nySluttdato.isBefore(gammelSluttdato)
    val erIFortiden = !nySluttdato.isAfter(LocalDate.now(clock))
    return erAvkortet && erIFortiden
}

private fun TiltaksdeltakelseLegacy.erIkkeAktuellDeltakelse(oppdatertDeltakelse: TiltaksdeltakelseLegacy): Boolean {
    return !harSammeStatus(oppdatertDeltakelse) &&
        oppdatertDeltakelse.deltakelseStatus == TiltakDeltakerstatus.IkkeAktuell
}

// En null tilOgMed i kjent tilstand betyr en åpen/uavsluttet deltakelse.
// Å sette en sluttdato på en åpen deltakelse er en innskrenking, ikke en forlengelse.
private fun TiltaksdeltakelseLegacy.erForlengelse(oppdatertDeltakelse: TiltaksdeltakelseLegacy): Boolean {
    val gammelSluttdato = deltakelseTilOgMed ?: return false
    return harSammeFraOgMed(oppdatertDeltakelse) &&
        oppdatertDeltakelse.deltakelseTilOgMed?.isAfter(gammelSluttdato) == true
}

private fun TiltaksdeltakelseLegacy.endretDeltakelsesmengde(
    oppdatertDeltakelse: TiltaksdeltakelseLegacy,
): TiltaksdeltakerEndring.EndretDeltakelsesmengde? {
    if (harSammeDeltakelsesmengde(oppdatertDeltakelse)) {
        return null
    }

    return TiltaksdeltakerEndring.EndretDeltakelsesmengde(
        oppdatertDeltakelse.deltakelseProsent,
        oppdatertDeltakelse.antallDagerPerUke,
    )
}

private fun TiltaksdeltakelseLegacy.endretStartdato(
    oppdatertDeltakelse: TiltaksdeltakelseLegacy,
): TiltaksdeltakerEndring.EndretStartdato? {
    if (harSammeFraOgMed(oppdatertDeltakelse)) {
        return null
    }

    return TiltaksdeltakerEndring.EndretStartdato(oppdatertDeltakelse.deltakelseFraOgMed)
}

private fun TiltaksdeltakelseLegacy.endretSluttdato(
    oppdatertDeltakelse: TiltaksdeltakelseLegacy,
): TiltaksdeltakerEndring.EndretSluttdato? {
    if (harSammeTilOgMed(oppdatertDeltakelse)) {
        return null
    }

    return TiltaksdeltakerEndring.EndretSluttdato(oppdatertDeltakelse.deltakelseTilOgMed)
}

private fun TiltaksdeltakelseLegacy.endretStatus(
    oppdatertDeltakelse: TiltaksdeltakelseLegacy,
): TiltaksdeltakerEndring.EndretStatus? {
    if (harSammeStatus(oppdatertDeltakelse)) {
        return null
    }

    return TiltaksdeltakerEndring.EndretStatus(oppdatertDeltakelse.deltakelseStatus)
}
