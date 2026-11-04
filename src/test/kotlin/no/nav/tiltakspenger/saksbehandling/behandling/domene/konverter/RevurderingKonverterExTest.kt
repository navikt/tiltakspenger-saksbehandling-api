package no.nav.tiltakspenger.saksbehandling.behandling.domene.konverter

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class RevurderingKonverterExTest {
    private val saksbehandler = ObjectMother.saksbehandler()

    @Test
    fun `tildelt saksbehandler kan konvertere en revurdering under behandling`() {
        ObjectMother.nyOpprettetRevurderingStans(saksbehandler = saksbehandler)
            .kanKonvertere(saksbehandler) shouldBe Unit.right()
    }

    @Test
    fun `søknadsbehandling kan ikke konverteres`() {
        ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)
            .kanKonvertere(saksbehandler) shouldBe KanIkkeKonvertereRevurdering.ErIkkeRevurdering.left()
    }

    @Test
    fun `beslutter uten saksbehandlerrollen kan ikke konvertere`() {
        val beslutter = ObjectMother.beslutter(navIdent = saksbehandler.navIdent)
        ObjectMother.nyOpprettetRevurderingStans(saksbehandler = saksbehandler)
            .kanKonvertere(beslutter) shouldBe KanIkkeKonvertereRevurdering.MåVæreSaksbehandler.left()
    }

    @Test
    fun `revurdering som ikke er under behandling kan ikke konverteres`() {
        val vedtaksperiode = ObjectMother.revurderingVedtaksperiode()
        val revurdering = ObjectMother.nyRevurderingStansKlarTilBeslutning(
            saksbehandler = saksbehandler,
            stansFraOgMed = vedtaksperiode.fraOgMed,
            førsteDagSomGirRett = vedtaksperiode.fraOgMed,
            sisteDagSomGirRett = vedtaksperiode.tilOgMed,
        )
        revurdering.kanKonvertere(saksbehandler) shouldBe
            KanIkkeKonvertereRevurdering.MåVæreUnderBehandling(Rammebehandlingsstatus.KLAR_TIL_BESLUTNING).left()
    }

    @Test
    fun `bare den tildelte saksbehandleren kan konvertere`() {
        val annenSaksbehandler = ObjectMother.saksbehandler(navIdent = "Z99999")
        ObjectMother.nyOpprettetRevurderingStans(saksbehandler = saksbehandler)
            .kanKonvertere(annenSaksbehandler) shouldBe
            KanIkkeKonvertereRevurdering.EiesAvAnnenSaksbehandler(saksbehandler.navIdent).left()
    }

    @Test
    fun `revurdering knyttet til en klagebehandling kan ikke konverteres`() {
        val revurdering = ObjectMother.nyOpprettetRevurderingInnvilgelse(saksbehandler = saksbehandler)
        val klagebehandling = ObjectMother.opprettKlagebehandling(
            sakId = revurdering.sakId,
            saksnummer = revurdering.saksnummer,
            fnr = revurdering.fnr,
            vedtakDetKlagesPå = VedtakId.random(),
            behandlingDetKlagesPå = RammebehandlingId.random(),
        ).copy(
            resultat = ObjectMother.klagebehandlingresultatOmgjør(
                behandlingId = listOf(revurdering.id),
                åpenBehandlingId = revurdering.id,
            ),
        )

        revurdering.copy(klagebehandling = klagebehandling)
            .kanKonvertere(saksbehandler) shouldBe KanIkkeKonvertereRevurdering.HarKlagebehandling.left()
    }
}
