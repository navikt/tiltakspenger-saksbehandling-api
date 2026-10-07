package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.tiltakDeltakerstatus
import java.time.Clock
import java.time.LocalDate

/**
 * Feltene endringsvurderingen sammenligner, likt for kjent tilstand og nå-tilstanden fra tiltakshistorikk.
 */
data class Deltakelsestilstand(
    val deltakelseFraOgMed: LocalDate?,
    val deltakelseTilOgMed: LocalDate?,
    val deltakelseStatus: TiltakDeltakerstatus,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
)

fun TiltaksdeltakelseIntern.tilDeltakelsestilstand() = Deltakelsestilstand(
    deltakelseFraOgMed = deltakelseFraOgMed,
    deltakelseTilOgMed = deltakelseTilOgMed,
    deltakelseStatus = deltakelseStatus,
    deltakelseProsent = deltakelseProsent,
    antallDagerPerUke = antallDagerPerUke,
)

fun Tiltaksdeltakelse.GirRett.tilDeltakelsestilstand(clock: Clock) = Deltakelsestilstand(
    deltakelseFraOgMed = fraOgMed,
    deltakelseTilOgMed = tilOgMed,
    deltakelseStatus = tiltakDeltakerstatus(clock),
    deltakelseProsent = omfang.deltakelsesprosent,
    antallDagerPerUke = omfang.dagerPerUke,
)

/**
 * Tolker endringene mellom nå-tilstanden fra tiltakshistorikk og en kjent tilstand for deltakelsen, for eksempel fra en åpen behandling.
 * Utfallene prioriteres i rekkefølgen avbrutt, ikke aktuell, forlengelse og andre endringer.
 * Avsluttet som forventet gis bare når statusendringen er den eneste endringen.
 * Gir null dersom ingenting relevant er endret.
 */
fun Deltakelsestilstand.finnEndringer(
    oppdatertDeltakelse: Deltakelsestilstand,
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

private fun Deltakelsestilstand.harSammeFraOgMed(oppdatertDeltakelse: Deltakelsestilstand): Boolean {
    return deltakelseFraOgMed == oppdatertDeltakelse.deltakelseFraOgMed
}

private fun Deltakelsestilstand.harSammeTilOgMed(oppdatertDeltakelse: Deltakelsestilstand): Boolean {
    return deltakelseTilOgMed == oppdatertDeltakelse.deltakelseTilOgMed
}

fun Deltakelsestilstand.harSammeStatus(oppdatertDeltakelse: Deltakelsestilstand): Boolean {
    return deltakelseStatus == oppdatertDeltakelse.deltakelseStatus
}

fun Deltakelsestilstand.harSammeDeltakelsesmengde(oppdatertDeltakelse: Deltakelsestilstand): Boolean {
    return compareValues(deltakelseProsent ?: 0F, oppdatertDeltakelse.deltakelseProsent ?: 0F) == 0 &&
        compareValues(antallDagerPerUke ?: 0F, oppdatertDeltakelse.antallDagerPerUke ?: 0F) == 0
}

private fun Deltakelsestilstand.erUendret(oppdatertDeltakelse: Deltakelsestilstand): Boolean {
    return harSammeFraOgMed(oppdatertDeltakelse) &&
        harSammeTilOgMed(oppdatertDeltakelse) &&
        harSammeDeltakelsesmengde(oppdatertDeltakelse) &&
        harSammeStatus(oppdatertDeltakelse)
}

/**
 * Gjelder bare når statusen er den eneste endringen, og den nye statusen er en forventet avslutning med sluttdato i dag eller tidligere.
 */
private fun Deltakelsestilstand.erAvsluttetSomForventet(
    oppdatertDeltakelse: Deltakelsestilstand,
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

private fun Deltakelsestilstand.erAvbruttDeltakelse(
    oppdatertDeltakelse: Deltakelsestilstand,
    clock: Clock,
): Boolean {
    val statusEndretTilAvbrutt = !harSammeStatus(oppdatertDeltakelse) &&
        oppdatertDeltakelse.deltakelseStatus == TiltakDeltakerstatus.Avbrutt
    return statusEndretTilAvbrutt || erSluttdatoAvkortetTilFortiden(oppdatertDeltakelse, clock)
}

private fun Deltakelsestilstand.erSluttdatoAvkortetTilFortiden(
    oppdatertDeltakelse: Deltakelsestilstand,
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

private fun Deltakelsestilstand.erIkkeAktuellDeltakelse(oppdatertDeltakelse: Deltakelsestilstand): Boolean {
    return !harSammeStatus(oppdatertDeltakelse) &&
        oppdatertDeltakelse.deltakelseStatus == TiltakDeltakerstatus.IkkeAktuell
}

// En null tilOgMed i kjent tilstand betyr en åpen/uavsluttet deltakelse.
// Å sette en sluttdato på en åpen deltakelse er en innskrenking, ikke en forlengelse.
private fun Deltakelsestilstand.erForlengelse(oppdatertDeltakelse: Deltakelsestilstand): Boolean {
    val gammelSluttdato = deltakelseTilOgMed ?: return false
    return harSammeFraOgMed(oppdatertDeltakelse) &&
        oppdatertDeltakelse.deltakelseTilOgMed?.isAfter(gammelSluttdato) == true
}

fun Deltakelsestilstand.endretDeltakelsesmengde(
    oppdatertDeltakelse: Deltakelsestilstand,
): TiltaksdeltakerEndring.EndretDeltakelsesmengde? {
    if (harSammeDeltakelsesmengde(oppdatertDeltakelse)) {
        return null
    }

    return TiltaksdeltakerEndring.EndretDeltakelsesmengde(
        oppdatertDeltakelse.deltakelseProsent,
        oppdatertDeltakelse.antallDagerPerUke,
    )
}

private fun Deltakelsestilstand.endretStartdato(
    oppdatertDeltakelse: Deltakelsestilstand,
): TiltaksdeltakerEndring.EndretStartdato? {
    if (harSammeFraOgMed(oppdatertDeltakelse)) {
        return null
    }

    return TiltaksdeltakerEndring.EndretStartdato(oppdatertDeltakelse.deltakelseFraOgMed)
}

private fun Deltakelsestilstand.endretSluttdato(
    oppdatertDeltakelse: Deltakelsestilstand,
): TiltaksdeltakerEndring.EndretSluttdato? {
    if (harSammeTilOgMed(oppdatertDeltakelse)) {
        return null
    }

    return TiltaksdeltakerEndring.EndretSluttdato(oppdatertDeltakelse.deltakelseTilOgMed)
}

private fun Deltakelsestilstand.endretStatus(
    oppdatertDeltakelse: Deltakelsestilstand,
): TiltaksdeltakerEndring.EndretStatus? {
    if (harSammeStatus(oppdatertDeltakelse)) {
        return null
    }

    return TiltaksdeltakerEndring.EndretStatus(oppdatertDeltakelse.deltakelseStatus)
}
