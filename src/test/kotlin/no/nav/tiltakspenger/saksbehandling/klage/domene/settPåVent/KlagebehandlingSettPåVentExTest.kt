package no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent

import arrow.core.left
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Pinner sjekkene i [settPåVentOgNullstillSaksbehandler] for begge verdiene av [SettKlagebehandlingPåVentKommando.oppdaterPåVent].
 * Oppdateringsgrenen nås i dag bare via rammebehandlingen, som stopper de fleste ugyldige kall før de kommer hit.
 */
class KlagebehandlingSettPåVentExTest {

    private val clock = TikkendeKlokke()

    private fun Klagebehandling.settPåVentEllerOppdater(
        oppdaterPåVent: Boolean,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        begrunnelse: String = "Begrunnelse for å sette klagen på vent",
        frist: LocalDate = 14.januar(2026),
    ) = settPåVentOgNullstillSaksbehandler(
        kommando = SettKlagebehandlingPåVentKommando(
            sakId = sakId,
            klagebehandlingId = id,
            saksbehandler = saksbehandler,
            oppdaterPåVent = oppdaterPåVent,
            begrunnelse = begrunnelse,
            frist = frist,
        ),
        clock = clock,
    )

    private fun klagebehandlingPåVent(): Klagebehandling =
        ObjectMother.opprettKlagebehandling(clock = clock)
            .settPåVentEllerOppdater(oppdaterPåVent = false)
            .getOrNull()!!
            .first

    @Test
    fun `kan oppdatere begrunnelse og frist på en klagebehandling som er satt på vent`() {
        val annenSaksbehandler = ObjectMother.saksbehandler("annenSaksbehandler")

        val (oppdatert, _) = klagebehandlingPåVent().settPåVentEllerOppdater(
            oppdaterPåVent = true,
            saksbehandler = annenSaksbehandler,
            begrunnelse = "Oppdatert begrunnelse",
            frist = 21.januar(2026),
        ).getOrNull()!!

        oppdatert.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
        oppdatert.saksbehandler shouldBe null
        oppdatert.ventestatus.erSattPåVent shouldBe true
        oppdatert.ventestatus.ventestatusHendelser.size shouldBe 2
        oppdatert.ventestatus.ventestatusHendelser.last().let { hendelse ->
            hendelse.endretAv shouldBe annenSaksbehandler.navIdent
            hendelse.begrunnelse shouldBe "Oppdatert begrunnelse"
            hendelse.frist shouldBe 21.januar(2026)
            hendelse.erSattPåVent shouldBe true
            hendelse.status shouldBe "KLAR_TIL_BEHANDLING"
        }
    }

    @Test
    fun `kan ikke oppdatere en klagebehandling som ikke er satt på vent`() {
        ObjectMother.opprettKlagebehandling(clock = clock)
            .settPåVentEllerOppdater(oppdaterPåVent = true) shouldBe
            KanIkkeSetteKlagebehandlingPåVent.KlagenMåVæreSattPåVentForÅKunneOppdateres.left()
    }

    @Test
    fun `kan ikke sette en klagebehandling på vent på nytt uten å oppdatere`() {
        klagebehandlingPåVent().settPåVentEllerOppdater(oppdaterPåVent = false) shouldBe
            KanIkkeSetteKlagebehandlingPåVent.KanIkkeOppdateres(KanIkkeOppdatereKlagebehandling.BehandlingenErSattPåVent).left()
    }
}
