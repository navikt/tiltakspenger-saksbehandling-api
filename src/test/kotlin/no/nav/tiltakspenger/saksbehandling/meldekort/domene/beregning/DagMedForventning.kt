package no.nav.tiltakspenger.saksbehandling.meldekort.domene.beregning

import arrow.core.NonEmptyList
import arrow.core.flatten
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.beregning.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.Status
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilMeldeperiodeBeregninger
import java.time.Clock
import java.time.LocalDate

data class DagMedForventning(
    val dag: LocalDate,
    val status: Status,
    val forventning: ReduksjonAvYtelsePåGrunnAvFravær,
)

suspend fun NonEmptyList<NonEmptyList<DagMedForventning>>.assertForventning(
    vedtaksperiode: Periode,
    clock: Clock = TikkendeKlokke(),
) {
    val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
        vedtaksperiode = vedtaksperiode,
        meldeperioder = this.map { outer -> outer.map { OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode.OppdatertDag(it.dag, it.status) } },
    )

    meldekortbehandlinger.tilMeldeperiodeBeregninger(clock).gjeldendeBeregningPerKjede.values
        .flatMap { it.dager }
        .forEachIndexed { index, it ->
            (it.dato to it.reduksjon) shouldBe (this.flatten()[index].dag to flatten()[index].forventning)
        }
}
