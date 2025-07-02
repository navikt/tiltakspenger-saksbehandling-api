package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.sammenlign
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.PosteringerForDag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Posteringstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsdag
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max

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

/**
 * Ment brukt både når man kjører lokalt og i testene for å lage litt mer realistiske testdata.
 */
fun Sak.genererSimuleringFraBeregning(
    behandling: MeldekortBehandling,
    meldeperiodeKjeder: MeldeperiodeKjeder = this.meldeperiodeKjeder,
): SimuleringMedMetadata {
    val simuleringForMeldeperioder = behandling.beregning!!.beregninger.map { beregningEtter ->
        val beregningFør = this.meldeperiodeBeregninger.sisteBeregningFør(
            beregningEtter.id,
            beregningEtter.kjedeId,
        )
        val sammenligning = sammenlign(
            forrigeBeregning = beregningFør,
            gjeldendeBeregning = beregningEtter,
        )
        SimuleringForMeldeperiode(
            // TODO jah: [MeldeperiodeBeregning] bør ha en meldeperiodeId. Blir mer riktig å bruke den enn kjedeId.
            meldeperiode = meldeperiodeKjeder.hentSisteMeldeperiodeForKjede(beregningEtter.kjedeId),
            simuleringsdager = sammenligning.dager.mapNotNull {
                if (it.erEndret) {
                    val erFeilutbetaling = it.totalbeløpEndring < 0
                    Simuleringsdag(
                        dato = it.dato,
                        tidligereUtbetalt = it.forrigeTotalbeløp,
                        nyUtbetaling = max(it.totalbeløpEndring, 0),
                        totalEtterbetaling = max(it.totalbeløpEndring, 0),
                        totalFeilutbetaling = if (erFeilutbetaling) abs(it.totalbeløpEndring) else 0,
                        posteringsdag = PosteringerForDag(
                            dato = it.dato,
                            posteringer = nonEmptyListOf(
                                PosteringForDag(
                                    dato = it.dato,
                                    fagområde = "TILTAKSPENGER",
                                    beløp = it.nyttTotalbeløp,
                                    // TODO jah: Legger inn denne enkel/feil. Utvid med https://github.com/navikt/helved-utbetaling/blob/main/dokumentasjon/simulering.md hvis du trenger.
                                    type = if (erFeilutbetaling) Posteringstype.FEILUTBETALING else Posteringstype.YTELSE,
                                    // TODO jah: Denne forblir enkelt. Utvid hvis du trenger
                                    klassekode = "test_klassekode",
                                ),
                            ),
                        ),
                    )
                } else {
                    null
                }
            }.toNonEmptyListOrNull()!!,
        )
    }
    return SimuleringMedMetadata(
        simulering = if (simuleringForMeldeperioder.isEmpty()) {
            Simulering.IngenEndring
        } else {
            Simulering.Endring(
                simuleringPerMeldeperiode = simuleringForMeldeperioder,
                datoBeregnet = LocalDate.now(),
                // TODO jah: Litt usikker på hva denne kommer som fra OS.
                totalBeløp = simuleringForMeldeperioder.sumOf { it.nyUtbetaling },
            )
        },
        originalJson = "{}",
    )
}
