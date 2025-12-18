package no.nav.tiltakspenger.saksbehandling.meldekort.domene.beregning

import arrow.core.NonEmptyList
import arrow.core.flatten
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilMeldeperiodeBeregninger
import java.time.Clock
import java.time.LocalDate

data class DagMedForventning(
    val dag: LocalDate,
    val status: Status,
    val forventning: ReduksjonAvYtelsePåGrunnAvFravær,
)

suspend fun NonEmptyList<NonEmptyList<DagMedForventning>>.assertForventning(vedtaksperiode: Periode, clock: Clock = TikkendeKlokke()) {
    val meldekortBehandlinger = ObjectMother.beregnMeldekortperioder(
        vedtaksperiode = vedtaksperiode,
        meldeperioder = this.map { outer -> outer.map { OppdaterMeldekortKommando.Dager.Dag(it.dag, it.status) } },
    )

    meldekortBehandlinger.tilMeldeperiodeBeregninger(clock).gjeldendeBeregningPerKjede.values
        .flatMap { it.dager }
        .forEachIndexed { index, it ->
            (it.dato to it.reduksjon) shouldBe (this.flatten()[index].dag to flatten()[index].forventning)
        }
}
