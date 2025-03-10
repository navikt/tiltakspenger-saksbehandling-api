package no.nav.tiltakspenger.domene

import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.felles.februar
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.felles.mars
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.FRAVÆR_SYK
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.IKKE_DELTATT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.SPERRET
import org.junit.jupiter.api.Test

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=597368022
 * Eksempel 1
 */
internal class Meldekortberegning5DagerIUkaEksempel1Test {
    private val meldekort1 = nonEmptyListOf(
        DagMedForventning(29.januar(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(30.januar(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(31.januar(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(1.februar(2024), FRAVÆR_SYK, IngenReduksjon),
        DagMedForventning(2.februar(2024), FRAVÆR_SYK, IngenReduksjon),
        DagMedForventning(3.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(4.februar(2024), IKKE_DELTATT, YtelsenFallerBort),

        DagMedForventning(5.februar(2024), FRAVÆR_SYK, IngenReduksjon),
        DagMedForventning(6.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(7.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(8.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(9.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(10.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(11.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
    )
    private val meldekort2 = nonEmptyListOf(
        DagMedForventning(12.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(13.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(14.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(15.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(16.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(17.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(18.februar(2024), IKKE_DELTATT, YtelsenFallerBort),

        DagMedForventning(19.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(20.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(21.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(22.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(23.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(24.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(25.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
    )
    private val meldekort3 = nonEmptyListOf(
        DagMedForventning(26.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(27.februar(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(28.februar(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(29.februar(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(1.mars(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(2.mars(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(3.mars(2024), SPERRET, YtelsenFallerBort),

        DagMedForventning(4.mars(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(5.mars(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(6.mars(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(7.mars(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(8.mars(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(9.mars(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(10.mars(2024), SPERRET, YtelsenFallerBort),
    )

    @Test
    fun `to sykeperioder med 16 dagers mellomrom`() {
        nonEmptyListOf(
            meldekort1,
            meldekort2,
            meldekort3,
        ).assertForventning(vurderingsperiode = Periode(1.februar(2024), 26.februar(2024)))
    }
}
