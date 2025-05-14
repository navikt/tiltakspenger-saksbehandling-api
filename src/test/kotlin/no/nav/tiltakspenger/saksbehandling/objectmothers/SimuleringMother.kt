package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata

interface SimuleringMother {

    fun simuleringMedMetadata(
        simulering: Simulering = simulering(),
        originalJson: String = "{}",
    ): SimuleringMedMetadata {
        return SimuleringMedMetadata(
            simulering = simulering,
            originalJson = originalJson,
        )
    }

    fun simulering(
        periode: Periode = ObjectMother.virkningsperiode(),
        meldeperiodeKjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
        oppsummering: Simulering.Endring.Oppsummering = Simulering.Endring.Oppsummering(
            periode = periode,
            tidligereUtbetalt = 0,
            nyUtbetaling = 0,
            totalEtterbetaling = 0,
            totalFeilutbetaling = 0,
            perMeldeperiode = nonEmptyListOf(
                Simulering.Endring.OppsummeringForMeldeperiode(
                    meldeperiode = periode,
                    meldeperiodeKjedeId = meldeperiodeKjedeId,
                    tidligereUtbetalt = 0,
                    nyUtbetaling = 0,
                    totalEtterbetaling = 0,
                    totalFeilutbetaling = 0,
                ),
            ),
        ),
        detaljer: Simulering.Endring.Detaljer = Simulering.Endring.Detaljer(
            datoBeregnet = periode.tilOgMed,
            totalBeløp = 100,
            perioder = nonEmptyListOf(
                Simulering.Endring.Detaljer.Simuleringsperiode(
                    periode = periode,
                    delperioder = listOf(
                        Simulering.Endring.Detaljer.Simuleringsperiode.Delperiode(
                            fagområde = "fagområde-todo",
                            periode = periode,
                            beløp = 100,
                            type = Simulering.Endring.PosteringType.YTELSE,
                            klassekode = "fagområde-todo",
                        ),
                    ),
                ),
            ),
        ),
    ): Simulering.Endring {
        return Simulering.Endring(
            oppsummering = oppsummering,
            detaljer = detaljer,
        )
    }
}
