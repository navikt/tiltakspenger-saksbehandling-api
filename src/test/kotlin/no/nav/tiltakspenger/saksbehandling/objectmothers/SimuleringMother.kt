package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringerForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Posteringstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsdag

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
        periode: Periode = Periode(6.januar(2025), 19.januar(2025)),
        meldeperiodeKjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
        meldeperiode: Meldeperiode = ObjectMother.meldeperiode(
            periode = periode,
            kjedeId = meldeperiodeKjedeId,
        ),
        simuleringsdager: NonEmptyList<Simuleringsdag> = nonEmptyListOf(
            Simuleringsdag(
                dato = periode.fraOgMed,
                tidligereUtbetalt = 0,
                nyUtbetaling = 0,
                totalEtterbetaling = 0,
                totalFeilutbetaling = 0,
                posteringsdag = PosteringerForDag(
                    dato = periode.fraOgMed,
                    posteringer = nonEmptyListOf(
                        PosteringForDag(
                            dato = periode.fraOgMed,
                            fagområde = "TILTAKSPENGER",
                            beløp = 0,
                            type = Posteringstype.YTELSE,
                            klassekode = "test_klassekode",
                        ),
                    ),
                ),
            ),
        ),
    ): Simulering.Endring {
        return Simulering.Endring(
            datoBeregnet = periode.tilOgMed,
            totalBeløp = 0,
            simuleringPerMeldeperiode = nonEmptyListOf(
                SimuleringForMeldeperiode(
                    meldeperiode = meldeperiode,
                    simuleringsdager = simuleringsdager,
                ),
            ),
        )
    }
}
