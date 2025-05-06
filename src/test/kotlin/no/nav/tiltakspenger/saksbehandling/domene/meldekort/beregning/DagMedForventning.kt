package no.nav.tiltakspenger.saksbehandling.domene.meldekort.beregning

import arrow.core.NonEmptyList
import arrow.core.flatten
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import java.time.LocalDate

data class DagMedForventning(
    val dag: LocalDate,
    val status: Status,
    val forventning: ReduksjonAvYtelsePåGrunnAvFravær,
)

suspend fun NonEmptyList<NonEmptyList<DagMedForventning>>.assertForventning(vurderingsperiode: Periode) {
    val actual = ObjectMother.beregnMeldekortperioder(
        vurderingsperiode = vurderingsperiode,
        meldeperioder = this.map { outer -> outer.map { OppdaterMeldekortKommando.Dager.Dag(it.dag, it.status) } },
    )
    actual.utfylteDager.forEachIndexed { index, it ->
        (it.dato to it.reduksjon) shouldBe (this.flatten()[index].dag to flatten()[index].forventning)
    }
}
