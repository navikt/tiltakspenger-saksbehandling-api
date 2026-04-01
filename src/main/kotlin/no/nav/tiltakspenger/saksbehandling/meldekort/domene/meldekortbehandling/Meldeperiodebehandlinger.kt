package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.UtfyltMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.tilMeldekortDager
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
        dager: UtfyltMeldeperiode,
        beregning: Beregning?,
        brukersMeldekort: BrukersMeldekort?,
    ) : this(nonEmptyListOf(Meldeperiodebehandling(dager, brukersMeldekort)), beregning)

    val fraOgMed: LocalDate = this.first().fraOgMed
    val tilOgMed: LocalDate = this.last().tilOgMed

    val totalPeriode: Periode = Periode(fraOgMed, tilOgMed)

    val kjedeIder: NonEmptyList<MeldeperiodeKjedeId> by lazy {
        this.map { it.kjedeId }.toNonEmptyListOrThrow()
    }

    val rammevedtak: NonEmptyList<VedtakId> by lazy {
        this.flatMap { it.meldeperiode.rammevedtak.verdier }.distinct().toNonEmptyListOrThrow()
    }

    val brukersMeldekort: List<BrukersMeldekort> by lazy { this.mapNotNull { it.brukersMeldekort } }

    val ingenDagerGirRett: Boolean by lazy { this.all { it.meldeperiode.ingenDagerGirRett } }

    fun oppdaterMeldeperioder(
        oppdaterteKjeder: MeldeperiodeKjeder,
    ): Meldeperiodebehandlinger {
        return this.map {
            val sisteMeldeperiode = oppdaterteKjeder.hentSisteMeldeperiodeForKjede(it.kjedeId)

            if (sisteMeldeperiode.versjon < it.meldeperiode.versjon) {
                it
            } else {
                it.copy(dager = sisteMeldeperiode.tilMeldekortDager())
            }
        }.let { Meldeperiodebehandlinger(it.toNonEmptyListOrThrow(), null) }
    }

    init {
        require(
            meldeperioder.size == 1 || meldeperioder.zipWithNext().all { (a, b) ->
                a.periode.tilOgMed.plusDays(1) == b.periode.fraOgMed
            },
        ) { "Meldeperiodene for en behandling må være sammenhengende og sortert" }
    }
}
