package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering.OppsummeringForPeriode
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
        oppsummeringForPerioder: List<OppsummeringForPeriode> = listOf(
            OppsummeringForPeriode(
                periode = periode,
                tidligereUtbetalt = 0,
                nyUtbetaling = 0,
                totalEtterbetaling = 0,
                totalFeilutbetaling = 0,
            ),
        ),
        detaljer: Simulering.Detaljer = Simulering.Detaljer(
            datoBeregnet = periode.tilOgMed,
            totalBeløp = 100,
            perioder = listOf(
                Simulering.Detaljer.Simuleringsperiode(
                    periode = periode,
                    posteringer = listOf(
                        Simulering.Detaljer.Simuleringsperiode.Postering(
                            fagområde = "fagområde-todo",
                            periode = periode,
                            beløp = 100,
                            type = "fagområde-todo",
                            klassekode = "fagområde-todo",
                        ),
                    ),
                ),
            ),
        ),
    ): Simulering {
        return Simulering(
            oppsummeringForPerioder = oppsummeringForPerioder,
            detaljer = detaljer,
        )
    }
}
