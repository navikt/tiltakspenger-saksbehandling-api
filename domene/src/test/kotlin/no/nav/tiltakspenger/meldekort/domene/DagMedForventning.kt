package no.nav.tiltakspenger.meldekort.domene

import arrow.core.NonEmptyList
import arrow.core.flatten
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutningKommando.Status
import no.nav.tiltakspenger.objectmothers.ObjectMother
import java.time.LocalDate

internal data class DagMedForventning(
    val dag: LocalDate,
    val status: Status,
    val forventning: ReduksjonAvYtelsePåGrunnAvFravær,
)

internal fun NonEmptyList<NonEmptyList<DagMedForventning>>.assertForventning(vurderingsperiode: Periode) {
    val actual = ObjectMother.beregnMeldekortperioder(
        vurderingsperiode = vurderingsperiode,
        meldeperioder = this.map { outer -> outer.map { SendMeldekortTilBeslutningKommando.Dager.Dag(it.dag, it.status) } },

    )
    actual.utfylteDager.forEachIndexed { index, it ->
        (it.dato to it.reduksjon) shouldBe (this.flatten()[index].dag to flatten()[index].forventning)
    }
}
