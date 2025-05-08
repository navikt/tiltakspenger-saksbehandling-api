package no.nav.tiltakspenger.saksbehandling.domene.meldekort.beregning

import arrow.core.toNonEmptyListOrNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.april
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort.BrukersMeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Dager
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.beregn
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

private typealias KommandoStatus = OppdaterMeldekortKommando.Status

/** Tester at beregning av brukers meldekort gir samme resultat som ved utfylling av saksbehandler */
class MeldekortberegningFraBrukersMeldekort {
    private val periodeSomStarterPåMandag = Periode(6.januar(2025), 31.mars(2025))
    private val periodeSomStarterPåTorsdag = Periode(9.januar(2025), 31.mars(2025))

    private fun kommandoDager(fraDato: LocalDate, statuser: List<KommandoStatus>) =
        statuser
            .mapIndexed { index, status ->
                Dager.Dag(
                    dag = fraDato.plusDays(index.toLong()),
                    status = status,
                )
            }
            .toNonEmptyListOrNull()!!

    private fun brukersMeldekortDager(fraDato: LocalDate, statuser: List<InnmeldtStatus>) =
        statuser
            .mapIndexed { index, status ->
                BrukersMeldekortDag(
                    dato = fraDato.plusDays(index.toLong()),
                    status = status,
                )
            }
            .toNonEmptyListOrNull()!!

    private fun sammenlign(
        kommandoStatuser: List<KommandoStatus>,
        brukersStatuser: List<InnmeldtStatus>,
        virkningsperiode: Periode,
    ) {
        val clock = fixedClockAt(1.april(2025))

        val (sak) = ObjectMother.nySakMedVedtak(
            virkningsperiode = virkningsperiode,
            clock = clock,
        ).first.genererMeldeperioder(clock)

        val meldeperiode = sak.meldeperiodeKjeder.first().first()

        val meldekortBehandlingId = MeldekortId.random()

        val saksbehandlerBehandling = ObjectMother.meldekortUnderBehandling(
            id = meldekortBehandlingId,
            sakId = sak.id,
            periode = meldeperiode.periode,
            meldeperiode = meldeperiode,
        )
        val sakMedÅpenMeldekortbehandling = sak.leggTilMeldekortbehandling(saksbehandlerBehandling)

        val dager = Dager(
            dager = kommandoDager(
                meldeperiode.periode.fraOgMed,
                kommandoStatuser,
            ),
        )
        val brukersMeldekort = ObjectMother.brukersMeldekort(
            sakId = sak.id,
            meldeperiode = meldeperiode,
            dager = brukersMeldekortDager(
                meldeperiode.periode.fraOgMed,
                brukersStatuser,
            ),
        )

        beregn(
            meldekortIdSomBeregnes = brukersMeldekort.id,
            meldeperiodeSomBeregnes = brukersMeldekort.tilMeldekortDager(),
            barnetilleggsPerioder = sakMedÅpenMeldekortbehandling.barnetilleggsperioder,
            tiltakstypePerioder = sakMedÅpenMeldekortbehandling.tiltakstypeperioder,
            meldekortBehandlinger = sakMedÅpenMeldekortbehandling.meldekortBehandlinger,

        ) shouldBe beregn(
            meldekortIdSomBeregnes = saksbehandlerBehandling.id,
            meldeperiodeSomBeregnes = dager.tilMeldekortDager(meldeperiode),
            barnetilleggsPerioder = sakMedÅpenMeldekortbehandling.barnetilleggsperioder,
            tiltakstypePerioder = sakMedÅpenMeldekortbehandling.tiltakstypeperioder,
            meldekortBehandlinger = sakMedÅpenMeldekortbehandling.meldekortBehandlinger,
        )
    }

    @Test
    fun `Skal beregne full deltagelse likt`() {
        sammenlign(
            listOf(
                List(5) { KommandoStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
                List(2) { KommandoStatus.IKKE_DELTATT },
                List(5) { KommandoStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
                List(2) { KommandoStatus.IKKE_DELTATT },
            ).flatten(),

            listOf(
                List(5) { InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
                List(2) { InnmeldtStatus.IKKE_REGISTRERT },
                List(5) { InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
                List(2) { InnmeldtStatus.IKKE_REGISTRERT },
            ).flatten(),

            periodeSomStarterPåMandag,
        )
    }

    @Test
    fun `Skal beregne med sykedager likt`() {
        sammenlign(
            listOf(
                List(5) { KommandoStatus.FRAVÆR_SYK },
                List(2) { KommandoStatus.IKKE_DELTATT },
                List(5) { KommandoStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
                List(2) { KommandoStatus.IKKE_DELTATT },
            ).flatten(),

            listOf(
                List(5) { InnmeldtStatus.FRAVÆR_SYK },
                List(2) { InnmeldtStatus.IKKE_REGISTRERT },
                List(5) { InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
                List(2) { InnmeldtStatus.IKKE_REGISTRERT },
            ).flatten(),

            periodeSomStarterPåMandag,
        )
    }

    @Test
    fun `Skal beregne med sykt barn likt`() {
        sammenlign(
            listOf(
                List(5) { KommandoStatus.FRAVÆR_SYKT_BARN },
                List(2) { KommandoStatus.IKKE_DELTATT },
                List(5) { KommandoStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
                List(2) { KommandoStatus.IKKE_DELTATT },
            ).flatten(),

            listOf(
                List(5) { InnmeldtStatus.FRAVÆR_SYKT_BARN },
                List(2) { InnmeldtStatus.IKKE_REGISTRERT },
                List(5) { InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
                List(2) { InnmeldtStatus.IKKE_REGISTRERT },
            ).flatten(),

            periodeSomStarterPåMandag,
        )
    }

    @Test
    fun `Skal beregne gyldig fravær likt`() {
        sammenlign(
            listOf(
                List(5) { KommandoStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV },
                List(2) { KommandoStatus.IKKE_DELTATT },
                List(5) { KommandoStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
                List(2) { KommandoStatus.IKKE_DELTATT },
            ).flatten(),

            listOf(
                List(5) { InnmeldtStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV },
                List(2) { InnmeldtStatus.IKKE_REGISTRERT },
                List(5) { InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
                List(2) { InnmeldtStatus.IKKE_REGISTRERT },
            ).flatten(),

            periodeSomStarterPåMandag,
        )
    }

    @Test
    fun `Skal beregne ugyldig fravær likt`() {
        sammenlign(
            listOf(
                List(5) { KommandoStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV },
                List(2) { KommandoStatus.IKKE_DELTATT },
                List(5) { KommandoStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
                List(2) { KommandoStatus.IKKE_DELTATT },
            ).flatten(),

            listOf(
                List(5) { InnmeldtStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV },
                List(2) { InnmeldtStatus.IKKE_REGISTRERT },
                List(5) { InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
                List(2) { InnmeldtStatus.IKKE_REGISTRERT },
            ).flatten(),

            periodeSomStarterPåMandag,
        )
    }

    @Test
    fun `Skal beregne med dager uten rett likt`() {
        sammenlign(
            listOf(
                List(3) { KommandoStatus.SPERRET },
                List(2) { KommandoStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV },
                List(2) { KommandoStatus.IKKE_DELTATT },
                List(5) { KommandoStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
                List(2) { KommandoStatus.IKKE_DELTATT },
            ).flatten(),

            listOf(
                List(3) { InnmeldtStatus.IKKE_REGISTRERT },
                List(2) { InnmeldtStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV },
                List(2) { InnmeldtStatus.IKKE_REGISTRERT },
                List(5) { InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
                List(2) { InnmeldtStatus.IKKE_REGISTRERT },
            ).flatten(),

            periodeSomStarterPåTorsdag,
        )
    }
}
