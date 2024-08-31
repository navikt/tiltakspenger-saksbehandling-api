package no.nav.tiltakspenger.meldekort.domene

import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.felles.april
import no.nav.tiltakspenger.felles.februar
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.mars
import no.nav.tiltakspenger.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.DelvisReduksjon
import no.nav.tiltakspenger.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon
import no.nav.tiltakspenger.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.FRAVÆR_SYK
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.FRAVÆR_SYKT_BARN
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.IKKE_DELTATT
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.SPERRET
import org.junit.jupiter.api.Test

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=597363679
 * Eksempel 2
 */
internal class Meldekortberegning5DagerIUkaEksempel5ArenaTest {
    private val meldekort1 = nonEmptyListOf(
        DagMedForventning(29.januar(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(30.januar(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(31.januar(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(1.februar(2024), FRAVÆR_SYK, IngenReduksjon),
        DagMedForventning(2.februar(2024), FRAVÆR_SYK, IngenReduksjon),
        DagMedForventning(3.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(4.februar(2024), IKKE_DELTATT, YtelsenFallerBort),

        DagMedForventning(5.februar(2024), FRAVÆR_SYK, IngenReduksjon),
        DagMedForventning(6.februar(2024), FRAVÆR_SYK, DelvisReduksjon),
        DagMedForventning(7.februar(2024), FRAVÆR_SYK, DelvisReduksjon),
        DagMedForventning(8.februar(2024), FRAVÆR_SYK, DelvisReduksjon),
        DagMedForventning(9.februar(2024), FRAVÆR_SYK, DelvisReduksjon),
        DagMedForventning(10.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(11.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
    )
    private val meldekort2 = nonEmptyListOf(
        DagMedForventning(12.februar(2024), FRAVÆR_SYK, DelvisReduksjon),
        DagMedForventning(13.februar(2024), FRAVÆR_SYK, DelvisReduksjon),
        DagMedForventning(14.februar(2024), FRAVÆR_SYK, DelvisReduksjon),
        DagMedForventning(15.februar(2024), FRAVÆR_SYK, DelvisReduksjon),
        DagMedForventning(16.februar(2024), FRAVÆR_SYK, DelvisReduksjon),
        DagMedForventning(17.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(18.februar(2024), IKKE_DELTATT, YtelsenFallerBort),

        DagMedForventning(19.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(20.februar(2024), FRAVÆR_SYK, DelvisReduksjon),
        DagMedForventning(21.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(22.februar(2024), FRAVÆR_SYK, DelvisReduksjon),
        DagMedForventning(23.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(24.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(25.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
    )
    private val meldekort3 = nonEmptyListOf(
        DagMedForventning(26.februar(2024), FRAVÆR_SYK, DelvisReduksjon),
        DagMedForventning(27.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(28.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(29.februar(2024), FRAVÆR_SYKT_BARN, IngenReduksjon),
        DagMedForventning(1.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(2.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(3.mars(2024), IKKE_DELTATT, YtelsenFallerBort),

        DagMedForventning(4.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(5.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(6.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(7.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(8.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(9.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(10.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
    )

    private val meldekort4 = nonEmptyListOf(
        DagMedForventning(11.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(12.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        // TODO jah: 13.mars fra confluence, så er vi usikre på om denne skal være 75% eller 100%.
        DagMedForventning(13.mars(2024), FRAVÆR_SYK, DelvisReduksjon),
        DagMedForventning(14.mars(2024), FRAVÆR_SYK, YtelsenFallerBort),
        DagMedForventning(15.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(16.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(17.mars(2024), IKKE_DELTATT, YtelsenFallerBort),

        DagMedForventning(18.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(19.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(20.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(21.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(22.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(23.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(24.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
    )
    private val meldekort5 = nonEmptyListOf(
        DagMedForventning(25.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(26.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(27.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(28.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(29.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(30.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(31.mars(2024), IKKE_DELTATT, YtelsenFallerBort),

        DagMedForventning(1.april(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(2.april(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(3.april(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(4.april(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(5.april(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(6.april(2024), SPERRET, YtelsenFallerBort),
        DagMedForventning(7.april(2024), SPERRET, YtelsenFallerBort),
    )

    @Test
    fun `to arbeidsgiverperioder - den første er fullt utbetalt - nytt sykefravær før opptjeningsperioden er fullført`() {
        nonEmptyListOf(
            meldekort1,
            meldekort2,
            meldekort3,
            meldekort4,
            meldekort5,
        ).assertForventning()
    }
}
