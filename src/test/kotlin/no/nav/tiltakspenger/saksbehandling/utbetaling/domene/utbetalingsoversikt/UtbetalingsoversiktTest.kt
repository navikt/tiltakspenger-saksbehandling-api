package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UtbetalingsoversiktTest {
    private val sakId = SakId.random()
    private val hentet = 5.mai(2025).atTime(12, 0)
    private val oppslag = Oppslag(Periode(1.januar(2025), 31.januar(2025)), Oppslagsperiodetype.YTELSESPERIODE)

    private fun vellykket(
        sakId: SakId = this.sakId,
        hentet: LocalDateTime = this.hentet,
        plan: Oppslagsplan = Oppslagsplan(hentet.plusDays(1), antallFeilPåRad = 0),
        id: UtbetalingsoversiktId = UtbetalingsoversiktId.random(),
    ) = Utbetalingsoversikt.Vellykket(id, sakId, hentet, oppslag, plan, utbetalinger = emptyList())

    private fun feilet(
        hentet: LocalDateTime = this.hentet,
        plan: Oppslagsplan = Oppslagsplan(hentet.plusMinutes(1), antallFeilPåRad = 1),
        id: UtbetalingsoversiktId = UtbetalingsoversiktId.random(),
    ) = Utbetalingsoversikt.Feilet(id, sakId, hentet, oppslag, plan, Oppslagsfeiltype.TJENESTEFEIL)

    @Test
    fun `vellykket oversikt kan ikke ha feil på rad`() {
        shouldThrow<IllegalArgumentException> { vellykket(plan = Oppslagsplan(hentet.plusDays(1), antallFeilPåRad = 1)) }
    }

    @Test
    fun `feilet oversikt må telle minst én feil`() {
        shouldThrow<IllegalArgumentException> { feilet(plan = Oppslagsplan(hentet.plusMinutes(1), antallFeilPåRad = 0)) }
    }

    @Test
    fun `neste oppslag kan ikke være før oversikten ble hentet`() {
        shouldThrow<IllegalArgumentException> { vellykket(plan = Oppslagsplan(hentet.minusSeconds(1), antallFeilPåRad = 0)) }
        shouldThrow<IllegalArgumentException> { feilet(plan = Oppslagsplan(hentet.minusSeconds(1), antallFeilPåRad = 1)) }
    }

    @Test
    fun `status gir antall feil på rad fra siste oppslag`() {
        Utbetalingsoversiktstatus.IkkeHentet.antallFeilPåRad shouldBe 0
        Utbetalingsoversiktstatus.SisteOppslagVellykket(vellykket()).antallFeilPåRad shouldBe 0
        Utbetalingsoversiktstatus.SisteOppslagFeilet(
            oversikt = feilet(plan = Oppslagsplan(hentet.plusMinutes(15), antallFeilPåRad = 3)),
            sisteVellykkede = null,
        ).antallFeilPåRad shouldBe 3
    }

    @Test
    fun `siste vellykkede må gjelde samme sak og ikke være nyere enn det feilede oppslaget`() {
        Utbetalingsoversiktstatus.SisteOppslagFeilet(feilet(), sisteVellykkede = vellykket(hentet = hentet.minusDays(1)))
        shouldThrow<IllegalArgumentException> {
            Utbetalingsoversiktstatus.SisteOppslagFeilet(feilet(), sisteVellykkede = vellykket(sakId = SakId.random(), hentet = hentet.minusDays(1)))
        }
        shouldThrow<IllegalArgumentException> {
            Utbetalingsoversiktstatus.SisteOppslagFeilet(feilet(), sisteVellykkede = vellykket(hentet = hentet.plusDays(1), plan = Oppslagsplan(hentet.plusDays(2), 0)))
        }
    }

    @Test
    fun `ved likt tidspunkt avgjør id-en hvilket oppslag som er sist`() {
        val (lavId, høyId) = listOf(UtbetalingsoversiktId.random(), UtbetalingsoversiktId.random()).sortedBy { it.toString() }

        Utbetalingsoversiktstatus.SisteOppslagFeilet(feilet(id = høyId), sisteVellykkede = vellykket(id = lavId))
        shouldThrow<IllegalArgumentException> {
            Utbetalingsoversiktstatus.SisteOppslagFeilet(feilet(id = lavId), sisteVellykkede = vellykket(id = høyId))
        }
    }

    @Test
    fun `id må ha riktig prefiks`() {
        val id = UtbetalingsoversiktId.random()

        UtbetalingsoversiktId.fromString(id.toString()) shouldBe id
        shouldThrow<IllegalArgumentException> { UtbetalingsoversiktId.fromString("sak_01K00000000000000000000000") }
    }
}
