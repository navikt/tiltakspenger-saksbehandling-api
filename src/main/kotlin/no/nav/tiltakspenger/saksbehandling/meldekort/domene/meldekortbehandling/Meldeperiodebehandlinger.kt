package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrThrow
import arrow.core.toNonEmptySetOrThrow
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.UtfyltMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.tilUtfyltMeldeperiode
import java.time.LocalDate

data class Meldeperiodebehandlinger(
    val meldeperioder: NonEmptyList<Meldeperiodebehandling>,
    val beregning: Beregning?,
) : List<Meldeperiodebehandling> by meldeperioder {

    constructor(
        meldeperiode: Meldeperiodebehandling,
        beregning: Beregning?,
    ) : this(nonEmptyListOf(meldeperiode), beregning)

    constructor(
        meldeperiode: UtfyltMeldeperiode,
        beregning: Beregning?,
        brukersMeldekort: BrukersMeldekort?,
        type: MeldeperiodebehandlingType,
        meldekortbehandlingId: MeldekortId,
    ) : this(
        nonEmptyListOf(
            Meldeperiodebehandling(
                dager = meldeperiode,
                brukersMeldekort = brukersMeldekort,
                type = type,
                meldekortbehandlingId = meldekortbehandlingId,
            ),
        ),
        beregning,
    )

    val fraOgMed: LocalDate = this.first().fraOgMed
    val tilOgMed: LocalDate = this.last().tilOgMed

    val totalPeriode: Periode = Periode(fraOgMed, tilOgMed)

    val kjedeIder: NonEmptySet<MeldeperiodeKjedeId> = this.map { it.kjedeId }.toNonEmptySetOrThrow()

    val rammevedtakIder: NonEmptyList<VedtakId> by lazy {
        this.flatMap { it.meldeperiode.rammevedtak.verdier }.distinct().toNonEmptyListOrThrow()
    }

    val brukersMeldekort: List<BrukersMeldekort> by lazy { this.mapNotNull { it.brukersMeldekort } }

    val ingenDagerGirRett: Boolean by lazy { this.all { it.meldeperiode.ingenDagerGirRett } }

    /**
     * Meldeperiodebehandlingene paret med tilhørende [MeldeperiodeBeregning] (matchet på `kjedeId`).
     * Init-blokken garanterer at parringen er entydig.
     *
     * Dersom det ikke er beregnet enda, vil [MeldeperiodebehandlingMedBeregning.meldeperiodeberegning] være `null`.
     */
    val meldeperioderMedBeregninger: List<MeldeperiodebehandlingMedBeregning> by lazy {
        meldeperioder.map { meldeperiodebehandling ->
            MeldeperiodebehandlingMedBeregning(
                meldeperiodebehandling = meldeperiodebehandling,
                meldeperiodeberegning = beregning?.singleOrNull { it.kjedeId == meldeperiodebehandling.kjedeId },
            )
        }
    }

    val erFullstendigUtfylt: Boolean by lazy { meldeperioder.all { it.erFullstendigUtfylt } }

    // Returnerer null dersom ingen kjeder har nyere meldeperioder (ingenting å oppdatere)
    fun oppdaterMedNyeKjeder(
        oppdaterteKjeder: MeldeperiodeKjeder,
    ): Meldeperiodebehandlinger? {
        val (erEndret, oppdaterteMeldeperioder) = this
            .fold(false to emptyList<Meldeperiodebehandling>()) { (erEndret, oppdaterteMeldeperioder), meldeperiode ->
                val sisteMeldeperiode = oppdaterteKjeder.hentSisteMeldeperiodeForKjede(meldeperiode.kjedeId)

                if (sisteMeldeperiode.versjon <= meldeperiode.meldeperiode.versjon) {
                    erEndret to oppdaterteMeldeperioder.plus(meldeperiode)
                } else {
                    true to oppdaterteMeldeperioder.plus(meldeperiode.copy(dager = sisteMeldeperiode.tilUtfyltMeldeperiode()))
                }
            }

        return if (erEndret) {
            Meldeperiodebehandlinger(oppdaterteMeldeperioder.toNonEmptyListOrThrow(), null)
        } else {
            null
        }
    }

    init {
        require(
            meldeperioder.zipWithNext().all { (a, b) ->
                a.periode.tilOgMed.isBefore(b.periode.fraOgMed)
            },
        ) { "Meldeperiodene for en behandling må være sortert" }

        if (beregning != null) {
            // Beregningen kan inkludere flere/andre perioder enn behandlingen.
            // Dette kan f.eks. være påfølgende perioder ved korrigering av sykedager.
            // Det kan også være perioder som faller i et hull mellom behandlingens meldeperioder.
            // Det er ok; vi sjekker kun beregningene som gjelder kjedene i denne behandlingen.
            val beregnedeKjedeIder = beregning.beregninger
                .map { it.kjedeId }
                .filter { it in kjedeIder }
                .toSet()

            require(kjedeIder == beregnedeKjedeIder) {
                "Beregningen må omfatte alle kjedene i behandlingen - Forventet $kjedeIder, fant $beregnedeKjedeIder"
            }
        }
    }
}
