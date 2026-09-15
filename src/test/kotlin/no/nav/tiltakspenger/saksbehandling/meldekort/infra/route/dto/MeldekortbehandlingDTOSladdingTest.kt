package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.common.januarDateTime
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.createOrThrow
import no.nav.tiltakspenger.saksbehandling.infra.route.AttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.VentestatusHendelseDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import no.nav.tiltakspenger.saksbehandling.infra.route.sladdet
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import org.junit.jupiter.api.Test

class MeldekortbehandlingDTOSladdingTest {

    @Test
    fun `navkontor og fritekstene i MeldekortbehandlingDTO sladdes mens øvrige felter forblir uendret`() {
        val meldekortbehandlingDTO = meldekortbehandlingDTO()

        meldekortbehandlingDTO.sladdet() shouldBe meldekortbehandlingDTO.copy(
            navkontor = SladdetVerdi,
            navkontorNavn = SladdetVerdi,
            begrunnelse = SladdetVerdi,
            tekstTilVedtaksbrev = SladdetVerdi,
            attesteringer = meldekortbehandlingDTO.attesteringer.map { it.sladdet() },
            avbrutt = meldekortbehandlingDTO.avbrutt?.sladdet(),
            ventestatus = meldekortbehandlingDTO.ventestatus.map { it.sladdet() },
        )
    }

    @Test
    fun `identer, perioder og status beholdes`() {
        val meldekortbehandlingDTO = meldekortbehandlingDTO()

        meldekortbehandlingDTO.sladdet().let {
            it.id shouldBe meldekortbehandlingDTO.id
            it.saksbehandler shouldBe meldekortbehandlingDTO.saksbehandler
            it.periode shouldBe meldekortbehandlingDTO.periode
            it.status shouldBe meldekortbehandlingDTO.status
            it.attesteringer.single().endretAv shouldBe meldekortbehandlingDTO.attesteringer.single().endretAv
        }
    }

    @Test
    fun `fritekster som mangler sladdes på linje med de utfylte`() {
        val utenFritekster = meldekortbehandlingDTO().copy(
            begrunnelse = null.ikkeSladdet(),
            tekstTilVedtaksbrev = null.ikkeSladdet(),
        )

        utenFritekster.sladdet().begrunnelse shouldBe SladdetVerdi
        utenFritekster.sladdet().tekstTilVedtaksbrev shouldBe SladdetVerdi
    }

    @Test
    fun `veileder får urørt meldekortbehandling, mens utvikler får den sladdet`() {
        val meldekortbehandlingDTO = meldekortbehandlingDTO()

        meldekortbehandlingDTO.sladdetFor(ObjectMother.veileder()) shouldBe meldekortbehandlingDTO
        meldekortbehandlingDTO.sladdetFor(ObjectMother.utvikler()) shouldBe meldekortbehandlingDTO.sladdet()
    }

    /**
     * Attesteringene og ventestatusen legges på wiretypen etter mappingen.
     * Domenet krever en full behandlingsflyt for å få dem satt, og sladdingen bryr seg bare om feltene.
     */
    private fun meldekortbehandlingDTO(): MeldekortbehandlingDTO = ObjectMother.meldekortbehandlingAvbrutt(
        begrunnelse = Begrunnelse.createOrThrow("Bruker var i tiltak hos arrangøren i Storgata"),
        fritekstTilVedtaksbrev = FritekstTilVedtaksbrev.create("Du får utbetalt tiltakspenger for perioden"),
    ).tilMeldekortbehandlingDTO(
        beregninger = MeldeperiodeBeregningerVedtatt.fraVedtaksliste(Vedtaksliste.empty()),
        hentVedtak = { null },
        hentTilbakekreving = { null },
        kallendeSaksbehandler = ObjectMother.saksbehandler(),
    ).copy(
        attesteringer = listOf(
            AttesteringDTO(
                endretAv = "B12345",
                status = Attesteringsstatus.SENDT_TILBAKE,
                begrunnelse = "Feil antall dager for barnet".ikkeSladdet(),
                endretTidspunkt = 1.januarDateTime(2025),
            ),
        ),
        ventestatus = listOf(
            VentestatusHendelseDTO(
                sattPåVentAv = "Z12345",
                tidspunkt = 1.januarDateTime(2025).toString(),
                status = "UNDER_BEHANDLING",
                begrunnelse = "Venter på dokumentasjon fra arrangøren".ikkeSladdet(),
                erSattPåVent = true,
                frist = null,
            ),
        ),
    )
}
