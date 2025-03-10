package no.nav.tiltakspenger.domene

import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.felles.april
import no.nav.tiltakspenger.saksbehandling.felles.februar
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.felles.mars
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.FRAVÆR_SYK
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.FRAVÆR_SYKT_BARN
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.IKKE_DELTATT
import org.junit.jupiter.api.Test

internal class MeldekortberegningKaranteneTest {

    private val meldekort1 = nonEmptyListOf(
        DagMedForventning(29.januar(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(30.januar(2024), FRAVÆR_SYK, IngenReduksjon),
        DagMedForventning(31.januar(2024), FRAVÆR_SYK, IngenReduksjon),
        DagMedForventning(1.februar(2024), FRAVÆR_SYK, IngenReduksjon),
        // 1. er siste dag med 100%, og 2. februar er første dag med 75%
        DagMedForventning(2.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(3.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(4.februar(2024), IKKE_DELTATT, YtelsenFallerBort),

        DagMedForventning(5.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(6.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(7.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(8.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(9.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(10.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(11.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
    )

    // Totalt 9 sykedager for bruker etter første meldekort
    private val meldekort2 = nonEmptyListOf(
        DagMedForventning(12.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(13.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(14.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(15.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(16.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(17.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(18.februar(2024), IKKE_DELTATT, YtelsenFallerBort),

        DagMedForventning(19.februar(2024), FRAVÆR_SYK, Reduksjon),
        DagMedForventning(20.februar(2024), FRAVÆR_SYK, Reduksjon),
        // 20. var 16 dagen med sykdom og siste dag med 75% fra nå er det karantene i 16 dager + dager man er syk.
        DagMedForventning(21.februar(2024), FRAVÆR_SYK, YtelsenFallerBort),
        DagMedForventning(22.februar(2024), FRAVÆR_SYK, YtelsenFallerBort),
        // Fremdeles 16 dager karantene. Trekker kun fra ikke-sykedager.
        DagMedForventning(23.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(24.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(25.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
    )

    // 13 dager igjen av karantenen. Merk at helgene også vil minske karantenen.
    private val meldekort3 = nonEmptyListOf(
        DagMedForventning(26.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(27.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(28.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(29.februar(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(1.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(2.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(3.mars(2024), IKKE_DELTATT, YtelsenFallerBort),

        DagMedForventning(4.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(5.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(6.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(7.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(8.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(9.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        // Karantenen er resatt etter 9. mars.
        DagMedForventning(10.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
    )

    private val meldekort4 = nonEmptyListOf(
        DagMedForventning(11.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(12.mars(2024), FRAVÆR_SYKT_BARN, IngenReduksjon),
        DagMedForventning(13.mars(2024), FRAVÆR_SYKT_BARN, IngenReduksjon),
        // 1 dag igjen med 100% pga. sykt barn før reduksjon.
        DagMedForventning(14.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(15.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(16.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(17.mars(2024), IKKE_DELTATT, YtelsenFallerBort),

        DagMedForventning(18.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(19.mars(2024), FRAVÆR_SYKT_BARN, IngenReduksjon),
        // Neste dag med sykt barn gir 75%
        DagMedForventning(20.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(21.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(22.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(23.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(24.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
    )
    private val meldekort5 = nonEmptyListOf(
        DagMedForventning(25.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(26.mars(2024), FRAVÆR_SYKT_BARN, Reduksjon),
        // 4 dagen med sykt barn - 12 dager til karantene pga. sykt barn.
        DagMedForventning(27.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(28.mars(2024), FRAVÆR_SYK, IngenReduksjon),
        // 2 dager igjen med egenmelding (syk bruker)
        DagMedForventning(29.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(30.mars(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(31.mars(2024), IKKE_DELTATT, YtelsenFallerBort),

        DagMedForventning(1.april(2024), FRAVÆR_SYK, IngenReduksjon),
        DagMedForventning(2.april(2024), FRAVÆR_SYK, IngenReduksjon),
        // egenmeldingsperioden for syk bruker er over. 13 dager igjen med delvis reduksjon.
        DagMedForventning(3.april(2024), FRAVÆR_SYKT_BARN, Reduksjon),
        // 3. april er 5. dagen med sykt barn og gir 75%
        DagMedForventning(4.april(2024), DELTATT_UTEN_LØNN_I_TILTAKET, IngenReduksjon),
        DagMedForventning(5.april(2024), FRAVÆR_SYK, Reduksjon),
        // 12 dager igjen av arbeidsgiverperioden før karantene pga. syk bruker
        DagMedForventning(6.april(2024), IKKE_DELTATT, YtelsenFallerBort),
        DagMedForventning(7.april(2024), IKKE_DELTATT, YtelsenFallerBort),
    )

    @Test
    fun `sjekk at karantene fungerer med nøyaktig 16 dager fravær`() {
        nonEmptyListOf(
            meldekort1,
            meldekort2,
            meldekort3,
            meldekort4,
            meldekort5,
        ).assertForventning(vurderingsperiode = Periode(29.januar(2024), 7.april(2024)))
    }
}
