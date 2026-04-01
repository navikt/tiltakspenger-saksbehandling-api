package no.nav.tiltakspenger.saksbehandling.meldekort.domene.beregning

import arrow.core.nonEmptyListOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode.OppdatertDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.Status.FRAVÆR_SYK
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.Status.FRAVÆR_SYKT_BARN
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.Status.IKKE_TILTAKSDAG
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class MeldekortberegningManuellTest {
    private val meldekort1 = nonEmptyListOf(
        OppdatertDag(29.januar(2024), IKKE_TILTAKSDAG),
        OppdatertDag(30.januar(2024), IKKE_TILTAKSDAG),
        OppdatertDag(31.januar(2024), IKKE_TILTAKSDAG),
        OppdatertDag(1.februar(2024), FRAVÆR_SYK),
        OppdatertDag(2.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(3.februar(2024), IKKE_TILTAKSDAG),
        OppdatertDag(4.februar(2024), IKKE_TILTAKSDAG),

        OppdatertDag(5.februar(2024), FRAVÆR_SYK),
        OppdatertDag(6.februar(2024), FRAVÆR_SYK),
        OppdatertDag(7.februar(2024), FRAVÆR_SYK),
        OppdatertDag(8.februar(2024), FRAVÆR_SYK),
        OppdatertDag(9.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(10.februar(2024), IKKE_TILTAKSDAG),
        OppdatertDag(11.februar(2024), IKKE_TILTAKSDAG),
    )
    private val meldekort2 = nonEmptyListOf(
        OppdatertDag(12.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(13.februar(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(14.februar(2024), FRAVÆR_SYK),
        OppdatertDag(15.februar(2024), FRAVÆR_SYK),
        OppdatertDag(16.februar(2024), FRAVÆR_SYK),
        OppdatertDag(17.februar(2024), IKKE_TILTAKSDAG),
        OppdatertDag(18.februar(2024), IKKE_TILTAKSDAG),

        OppdatertDag(19.februar(2024), FRAVÆR_SYK),
        OppdatertDag(20.februar(2024), FRAVÆR_SYK),
        OppdatertDag(21.februar(2024), FRAVÆR_SYK),
        OppdatertDag(22.februar(2024), FRAVÆR_SYK),
        OppdatertDag(23.februar(2024), FRAVÆR_SYK),
        OppdatertDag(24.februar(2024), IKKE_TILTAKSDAG),
        OppdatertDag(25.februar(2024), IKKE_TILTAKSDAG),
    )
    private val meldekort3 = nonEmptyListOf(
        OppdatertDag(26.februar(2024), FRAVÆR_SYK),
        OppdatertDag(27.februar(2024), FRAVÆR_SYK),
        OppdatertDag(28.februar(2024), FRAVÆR_SYK),
        OppdatertDag(29.februar(2024), FRAVÆR_SYK),
        OppdatertDag(1.mars(2024), FRAVÆR_SYK),
        OppdatertDag(2.mars(2024), IKKE_TILTAKSDAG),
        OppdatertDag(3.mars(2024), IKKE_TILTAKSDAG),

        OppdatertDag(4.mars(2024), FRAVÆR_SYK),
        OppdatertDag(5.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(6.mars(2024), FRAVÆR_SYK),
        OppdatertDag(7.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(8.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(9.mars(2024), IKKE_TILTAKSDAG),
        OppdatertDag(10.mars(2024), IKKE_TILTAKSDAG),
    )
    private val meldekort4 = nonEmptyListOf(
        OppdatertDag(11.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(12.mars(2024), FRAVÆR_SYKT_BARN),
        OppdatertDag(13.mars(2024), FRAVÆR_SYKT_BARN),
        OppdatertDag(14.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(15.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(16.mars(2024), IKKE_TILTAKSDAG),
        OppdatertDag(17.mars(2024), IKKE_TILTAKSDAG),

        OppdatertDag(18.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(19.mars(2024), FRAVÆR_SYKT_BARN),
        OppdatertDag(20.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(21.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(22.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(23.mars(2024), IKKE_TILTAKSDAG),
        OppdatertDag(24.mars(2024), IKKE_TILTAKSDAG),
    )
    private val meldekort5 = nonEmptyListOf(
        OppdatertDag(25.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(26.mars(2024), FRAVÆR_SYK),
        OppdatertDag(27.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(28.mars(2024), FRAVÆR_SYK),
        OppdatertDag(29.mars(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(30.mars(2024), IKKE_TILTAKSDAG),
        OppdatertDag(31.mars(2024), IKKE_TILTAKSDAG),

        OppdatertDag(1.april(2024), FRAVÆR_SYK),
        OppdatertDag(2.april(2024), FRAVÆR_SYK),
        OppdatertDag(3.april(2024), FRAVÆR_SYKT_BARN),
        OppdatertDag(4.april(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(5.april(2024), DELTATT_UTEN_LØNN_I_TILTAKET),
        OppdatertDag(6.april(2024), IKKE_TILTAKSDAG),
        OppdatertDag(7.april(2024), IKKE_TILTAKSDAG),
    )

    @Test
    @Disabled
    fun `manuell test av meldekortberegning`() {
        runTest {
            val meldekortBeregning = ObjectMother.beregnMeldekortperioder(
                vedtaksperiode = Periode(29.januar(2024), 7.april(2024)),
                meldeperioder = nonEmptyListOf(meldekort1, meldekort2, meldekort3, meldekort4, meldekort5),
            )
            for (dag in meldekortBeregning) {
                println(dag)
            }
        }
    }
}
