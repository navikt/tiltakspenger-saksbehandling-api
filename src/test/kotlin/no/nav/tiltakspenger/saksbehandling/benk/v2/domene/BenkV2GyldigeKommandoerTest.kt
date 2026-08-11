package no.nav.tiltakspenger.saksbehandling.benk.v2.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.Avbryt
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.Gjenoppta
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.LeggTilbakeBeslutter
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.LeggTilbakeSaksbehandler
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.OvertaBeslutter
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.OvertaSaksbehandler
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.SettPåVent
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.TildelBeslutter
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.TildelSaksbehandler
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Matrisen over statuser og roller for kommandoene på en benkrad.
 * Reglene speiler domenets `finnGyldigeKommandoer`-funksjoner, og er i tillegg pinnet mot dem gjennom prodstien i `BenkV2AggregatTest`.
 */
class BenkV2GyldigeKommandoerTest {

    private val saksbehandler = ObjectMother.saksbehandler()
    private val beslutter = ObjectMother.beslutter()

    private fun felles(
        saksbehandler: String? = null,
        beslutter: String? = null,
        erSattPåVent: Boolean = false,
    ) = BenkV2Behandlingsfelles(
        sakId = SakId.random(),
        fnr = Fnr.random(),
        saksnummer = Saksnummer("202501011001"),
        startet = LocalDateTime.of(2025, 1, 1, 12, 0),
        sistEndret = LocalDateTime.of(2025, 1, 2, 12, 0),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        erUnderkjent = false,
        ventestatus = BenkV2Ventestatus(erSattPåVent = erSattPåVent, begrunnelse = null, frist = null),
    )

    private fun søknadsbehandling(
        status: BenkV2Behandlingsstatus,
        felles: BenkV2Behandlingsfelles,
    ) = BenkSøknadsbehandling(
        felles = felles,
        id = RammebehandlingId.random(),
        status = status,
        søknadstype = BenkSøknadstype.DIGITAL,
        kravtidspunkt = felles.startet,
        resultat = null,
    )

    private fun meldekort(
        status: BenkV2Behandlingsstatus,
        felles: BenkV2Behandlingsfelles,
        type: BenkMeldekortType = BenkMeldekortType.MELDEKORTBEHANDLING,
    ) = BenkMeldekort(
        felles = felles,
        id = MeldekortId.random(),
        status = status,
        type = type,
        meldeperioder = listOf(Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 19))),
        beløp = null,
        mottattTidspunkt = null,
    )

    private fun tilbakekreving(
        status: BenkTilbakekrevingStatus,
        felles: BenkV2Behandlingsfelles,
    ) = BenkTilbakekreving(
        felles = felles,
        id = TilbakekrevingId.random(),
        status = status,
        beløp = BigDecimal(5000),
        kilde = BenkTilbakekrevingKilde.MELDEKORT,
        kravgrunnlagPeriode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 19)),
        url = "https://tilbakekreving.example.com",
    )

    @Nested
    inner class Rammebehandlinger {

        @Test
        fun `klar til behandling kan tildeles og avbrytes`() {
            søknadsbehandling(BenkV2Behandlingsstatus.KLAR_TIL_BEHANDLING, felles())
                .finnGyldigeKommandoer(saksbehandler) shouldBe listOf(TildelSaksbehandler, Avbryt)
        }

        @Test
        fun `klar til behandling kan ikke tildeles en ren beslutter`() {
            søknadsbehandling(BenkV2Behandlingsstatus.KLAR_TIL_BEHANDLING, felles())
                .finnGyldigeKommandoer(beslutter) shouldBe listOf(Avbryt)
        }

        @Test
        fun `under behandling kan legges tilbake, settes på vent og avbrytes av saksbehandleren på saken`() {
            søknadsbehandling(
                BenkV2Behandlingsstatus.UNDER_BEHANDLING,
                felles(saksbehandler = saksbehandler.navIdent),
            ).finnGyldigeKommandoer(saksbehandler) shouldBe listOf(LeggTilbakeSaksbehandler, SettPåVent, Avbryt)
        }

        @Test
        fun `under behandling kan overtas av en annen saksbehandler`() {
            søknadsbehandling(
                BenkV2Behandlingsstatus.UNDER_BEHANDLING,
                felles(saksbehandler = "Z999999"),
            ).finnGyldigeKommandoer(saksbehandler) shouldBe listOf(OvertaSaksbehandler, Avbryt)
        }

        @Test
        fun `klar til beslutning kan tildeles en beslutter`() {
            søknadsbehandling(
                BenkV2Behandlingsstatus.KLAR_TIL_BESLUTNING,
                felles(saksbehandler = saksbehandler.navIdent),
            ).finnGyldigeKommandoer(beslutter) shouldBe listOf(TildelBeslutter)
        }

        @Test
        fun `klar til beslutning kan ikke tildeles saksbehandleren på saken, men kan avbrytes av hen`() {
            søknadsbehandling(
                BenkV2Behandlingsstatus.KLAR_TIL_BESLUTNING,
                felles(saksbehandler = saksbehandler.navIdent),
            ).finnGyldigeKommandoer(saksbehandler) shouldBe listOf(Avbryt)
        }

        @Test
        fun `under beslutning kan legges tilbake, settes på vent og avbrytes av beslutteren på saken`() {
            søknadsbehandling(
                BenkV2Behandlingsstatus.UNDER_BESLUTNING,
                felles(saksbehandler = "Z999999", beslutter = beslutter.navIdent),
            ).finnGyldigeKommandoer(beslutter) shouldBe listOf(LeggTilbakeBeslutter, SettPåVent, Avbryt)
        }

        @Test
        fun `under beslutning kan overtas av en annen beslutter`() {
            søknadsbehandling(
                BenkV2Behandlingsstatus.UNDER_BESLUTNING,
                felles(saksbehandler = "Z999999", beslutter = "B999999"),
            ).finnGyldigeKommandoer(beslutter) shouldBe listOf(OvertaBeslutter)
        }

        @Test
        fun `satt på vent kan gjenopptas, men ikke settes på vent igjen`() {
            søknadsbehandling(
                BenkV2Behandlingsstatus.UNDER_BEHANDLING,
                felles(saksbehandler = saksbehandler.navIdent, erSattPåVent = true),
            ).finnGyldigeKommandoer(saksbehandler) shouldBe listOf(LeggTilbakeSaksbehandler, Gjenoppta, Avbryt)
        }

        @Test
        fun `under automatisk behandling kan settes på vent og avbrytes`() {
            søknadsbehandling(BenkV2Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING, felles())
                .finnGyldigeKommandoer(saksbehandler) shouldBe listOf(SettPåVent, Avbryt)
        }
    }

    @Nested
    inner class Meldekort {

        @Test
        fun `innsendt meldekort har ingen kommandoer`() {
            meldekort(
                status = BenkV2Behandlingsstatus.KLAR_TIL_BEHANDLING,
                felles = felles(),
                type = BenkMeldekortType.INNSENDT_MELDEKORT,
            ).finnGyldigeKommandoer(saksbehandler) shouldBe emptyList()
        }

        @Test
        fun `klar til behandling kan tildeles, men ikke avbrytes`() {
            meldekort(BenkV2Behandlingsstatus.KLAR_TIL_BEHANDLING, felles())
                .finnGyldigeKommandoer(saksbehandler) shouldBe listOf(TildelSaksbehandler)
        }

        @Test
        fun `under behandling kan legges tilbake, settes på vent og avbrytes av saksbehandleren på saken`() {
            meldekort(
                BenkV2Behandlingsstatus.UNDER_BEHANDLING,
                felles(saksbehandler = saksbehandler.navIdent),
            ).finnGyldigeKommandoer(saksbehandler) shouldBe listOf(LeggTilbakeSaksbehandler, SettPåVent, Avbryt)
        }

        @Test
        fun `klar til beslutning kan avbrytes av saksbehandleren på saken`() {
            meldekort(
                BenkV2Behandlingsstatus.KLAR_TIL_BESLUTNING,
                felles(saksbehandler = saksbehandler.navIdent),
            ).finnGyldigeKommandoer(saksbehandler) shouldBe listOf(Avbryt)
        }

        @Test
        fun `klar til beslutning kan tildeles en beslutter`() {
            meldekort(
                BenkV2Behandlingsstatus.KLAR_TIL_BESLUTNING,
                felles(saksbehandler = saksbehandler.navIdent),
            ).finnGyldigeKommandoer(beslutter) shouldBe listOf(TildelBeslutter)
        }

        @Test
        fun `satt på vent under behandling kan bare gjenopptas av saksbehandleren på saken`() {
            val rad = meldekort(
                BenkV2Behandlingsstatus.UNDER_BEHANDLING,
                felles(saksbehandler = "Z999999", erSattPåVent = true),
            )
            rad.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(OvertaSaksbehandler)
        }
    }

    @Nested
    inner class Tilbakekrevinger {

        @Test
        fun `til behandling kan tildeles en saksbehandler`() {
            tilbakekreving(BenkTilbakekrevingStatus.TIL_BEHANDLING, felles())
                .finnGyldigeKommandoer(saksbehandler) shouldBe listOf(TildelSaksbehandler)
        }

        @Test
        fun `under behandling kan legges tilbake av saksbehandleren på saken og overtas av andre`() {
            val rad = tilbakekreving(
                BenkTilbakekrevingStatus.UNDER_BEHANDLING,
                felles(saksbehandler = saksbehandler.navIdent),
            )
            rad.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(LeggTilbakeSaksbehandler)
            rad.finnGyldigeKommandoer(ObjectMother.saksbehandler(navIdent = "Z999999")) shouldBe
                listOf(OvertaSaksbehandler)
        }

        @Test
        fun `til godkjenning kan tildeles en beslutter, men ikke saksbehandleren på saken`() {
            val rad = tilbakekreving(
                BenkTilbakekrevingStatus.TIL_GODKJENNING,
                felles(saksbehandler = saksbehandler.navIdent),
            )
            rad.finnGyldigeKommandoer(beslutter) shouldBe listOf(TildelBeslutter)
            rad.finnGyldigeKommandoer(saksbehandler) shouldBe emptyList<SaksbehandlerBehandlingKommando>()
        }

        @Test
        fun `under godkjenning kan legges tilbake av beslutteren på saken og overtas av andre besluttere`() {
            val rad = tilbakekreving(
                BenkTilbakekrevingStatus.UNDER_GODKJENNING,
                felles(saksbehandler = "Z999999", beslutter = beslutter.navIdent),
            )
            rad.finnGyldigeKommandoer(beslutter) shouldBe listOf(LeggTilbakeBeslutter)
            rad.finnGyldigeKommandoer(ObjectMother.beslutter(navIdent = "B999999")) shouldBe listOf(OvertaBeslutter)
        }

        @Test
        fun `opprettet har ingen kommandoer`() {
            tilbakekreving(BenkTilbakekrevingStatus.OPPRETTET, felles())
                .finnGyldigeKommandoer(saksbehandler) shouldBe emptyList()
        }
    }
}
