package no.nav.tiltakspenger.saksbehandling.meldekort.domene.beregning

import arrow.core.nonEmptyListOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status.FRAVÆR_SYK
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status.FRAVÆR_SYKT_BARN
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Status.IKKE_TILTAKSDAG
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort
import org.junit.jupiter.api.Test

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=597368022
 * Eksempel 2
 */
internal class Meldekortberegning5DagerIUkaEksempel4Test {
    private val meldekort1 = nonEmptyListOf(
        DagMedForventning(29.januar(2024), IKKE_RETT_TIL_TILTAKSPENGER, YtelsenFallerBort),
        DagMedForventning(30.januar(2024), IKKE_RETT_TIL_TILTAKSPENGER, YtelsenFallerBort),
        DagMedForventning(31.januar(2024), IKKE_RETT_TIL_TILTAKSPENGER, YtelsenFallerBort),
        DagMedForventning(1.februar(2024), FRAVÆR_SYK, IngenReduksjon),
        DagMedForventning(2.februar(2024), FRAVÆR_SYK, IngenReduksjon),
        DagMedForventning(3.februar(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
        DagMedForventning(4.februar(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),

        DagMedForventning(5.februar(2024), FRAVÆR_SYK, IngenReduksjon),
        DagMedForventning(6.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(7.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(8.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(9.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(10.februar(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
        DagMedForventning(11.februar(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
    )
    private val meldekort2 = nonEmptyListOf(
        DagMedForventning(12.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(13.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(14.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(15.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(16.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(17.februar(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
        DagMedForventning(18.februar(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),

        DagMedForventning(19.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(20.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(21.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(22.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(23.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(24.februar(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
        DagMedForventning(25.februar(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
    )
    private val meldekort3 = nonEmptyListOf(
        DagMedForventning(26.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(27.februar(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
        DagMedForventning(28.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(29.februar(2024), FRAVÆR_SYKT_BARN, IngenReduksjon),
        DagMedForventning(1.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(2.mars(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
        DagMedForventning(3.mars(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),

        DagMedForventning(4.mars(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
        DagMedForventning(5.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(6.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(7.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(8.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(9.mars(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
        DagMedForventning(10.mars(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
    )

    private val meldekort4 = nonEmptyListOf(
        DagMedForventning(11.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(12.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(13.mars(2024), FRAVÆR_SYK, YtelsenFallerBort),
        DagMedForventning(14.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(15.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(16.mars(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
        DagMedForventning(17.mars(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),

        DagMedForventning(18.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(19.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(20.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(21.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(22.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(23.mars(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
        DagMedForventning(24.mars(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
    )
    private val meldekort5 = nonEmptyListOf(
        DagMedForventning(25.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(26.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(27.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(28.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(29.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(30.mars(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),
        DagMedForventning(31.mars(2024), IKKE_TILTAKSDAG, YtelsenFallerBort),

        DagMedForventning(1.april(2024), FRAVÆR_SYK, IngenReduksjon),
        DagMedForventning(2.april(2024), FRAVÆR_SYK, IngenReduksjon),
        DagMedForventning(3.april(2024), IKKE_RETT_TIL_TILTAKSPENGER, YtelsenFallerBort),
        DagMedForventning(4.april(2024), IKKE_RETT_TIL_TILTAKSPENGER, YtelsenFallerBort),
        DagMedForventning(5.april(2024), IKKE_RETT_TIL_TILTAKSPENGER, YtelsenFallerBort),
        DagMedForventning(6.april(2024), IKKE_RETT_TIL_TILTAKSPENGER, YtelsenFallerBort),
        DagMedForventning(7.april(2024), IKKE_RETT_TIL_TILTAKSPENGER, YtelsenFallerBort),
    )

    @Test
    fun `to arbeidsgiverperioder - den første er fullt utbetalt - nytt sykefravær før opptjeningsperioden er fullført`() {
        runTest {
            nonEmptyListOf(
                meldekort1,
                meldekort2,
                meldekort3,
                meldekort4,
                meldekort5,
            ).assertForventning(vurderingsperiode = Periode(1.februar(2024), 2.april(2024)))
        }
    }
}
