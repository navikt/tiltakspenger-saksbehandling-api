package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.januarDateTime
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.createOrThrow
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.VentestatusHendelseDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import no.nav.tiltakspenger.saksbehandling.infra.route.sladdet
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbruttKlagebehandlingStatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt.KlagebehandlingAvbruttDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class KlagebehandlingDTOSladdingTest {

    private val brevtekster = Brevtekster(
        listOf(TittelOgTekst(tittel = "Vurderingen vår", tekst = "Du bor i Storgata og deltok ikke på tiltaket")),
    )

    @Test
    fun `alle personopplysningsfelter i KlagebehandlingDTO sladdes mens øvrige felter forblir uendret`() {
        val klagebehandlingDTO = klagebehandlingDTO()

        klagebehandlingDTO.sladdet() shouldBe klagebehandlingDTO.copy(
            fnr = SladdetVerdi,
            avbrutt = klagebehandlingDTO.avbrutt?.sladdet(),
            ventestatus = klagebehandlingDTO.ventestatus.map { it.sladdet() },
            resultat = klagebehandlingDTO.resultat?.sladdet(),
        )
    }

    @Test
    fun `saksnummer, journalpostId og formkrav beholdes`() {
        val klagebehandlingDTO = klagebehandlingDTO()

        klagebehandlingDTO.sladdet().let {
            it.saksnummer shouldBe klagebehandlingDTO.saksnummer
            it.klagensJournalpostId shouldBe klagebehandlingDTO.klagensJournalpostId
            it.formkrav shouldBe klagebehandlingDTO.formkrav
        }
    }

    @Test
    fun `brevteksten i et avvist resultat sladdes mens tittelen beholdes`() {
        val resultat = ObjectMother.klagebehandlingresultatAvvist(brevtekster = brevtekster)
            .tilKlagebehandlingsresultatDTO() as KlagebehandlingsresultatDTO.Avvist

        (resultat.sladdet() as KlagebehandlingsresultatDTO.Avvist).brevtekst.single().let {
            it.tittel shouldBe resultat.brevtekst.single().tittel
            it.tekst shouldBe SladdetVerdi
        }
    }

    @Test
    fun `begrunnelsene i et omgjort resultat sladdes`() {
        val resultat = ObjectMother.klagebehandlingresultatOmgjør(
            begrunnelse = Begrunnelse.createOrThrow("Feil lovanvendelse for barnetillegget"),
        ).tilKlagebehandlingsresultatDTO() as KlagebehandlingsresultatDTO.Omgjør

        (resultat.sladdet() as KlagebehandlingsresultatDTO.Omgjør).let {
            it.begrunnelse shouldBe SladdetVerdi
            it.begrunnelseFerdigstilling shouldBe SladdetVerdi
            it.årsak shouldBe resultat.årsak
        }
    }

    @Test
    fun `brevtekst og begrunnelse i et opprettholdt resultat sladdes mens hjemlene beholdes`() {
        val resultat = ObjectMother.klagebehandlingresultatOpprettholdt(
            brevtekst = brevtekster,
            begrunnelseFerdigstilling = Begrunnelse.createOrThrow("Sendt til klageinstansen"),
        ).tilKlagebehandlingsresultatDTO() as KlagebehandlingsresultatDTO.Opprettholdt

        (resultat.sladdet() as KlagebehandlingsresultatDTO.Opprettholdt).let {
            it.brevtekst.single().tekst shouldBe SladdetVerdi
            it.brevtekst.single().tittel shouldBe resultat.brevtekst.single().tittel
            it.begrunnelseFerdigstilling shouldBe SladdetVerdi
            it.hjemler shouldBe resultat.hjemler
        }
    }

    @Test
    fun `veileder får urørt klagebehandling, mens utvikler får den sladdet`() {
        val klagebehandlingDTO = klagebehandlingDTO()

        klagebehandlingDTO.sladdetFor(ObjectMother.veileder()) shouldBe klagebehandlingDTO
        klagebehandlingDTO.sladdetFor(ObjectMother.utvikler()) shouldBe klagebehandlingDTO.sladdet()
    }

    /**
     * Avbruddet og ventestatusen legges på wiretypen etter mappingen.
     * Domenet krever en full behandlingsflyt for å få dem satt, og sladdingen bryr seg bare om feltene.
     */
    private fun klagebehandlingDTO(): KlagebehandlingDTO =
        ObjectMother.opprettKlagebehandling().tilKlagebehandlingDTO().copy(
            resultat = ObjectMother.klagebehandlingresultatOpprettholdt(
                brevtekst = brevtekster,
                begrunnelseFerdigstilling = Begrunnelse.createOrThrow("Sendt til klageinstansen"),
            ).tilKlagebehandlingsresultatDTO(),
            avbrutt = KlagebehandlingAvbruttDTO(
                avbruttAv = "Z12345",
                avbruttTidspunkt = 1.januarDateTime(2025),
                status = AvbruttKlagebehandlingStatus.KLAGE_TRUKKET,
                begrunnelse = "Klagen er trukket av søker".ikkeSladdet(),
            ),
            ventestatus = listOf(
                VentestatusHendelseDTO(
                    sattPåVentAv = "Z12345",
                    tidspunkt = 1.januarDateTime(2025).toString(),
                    status = "UNDER_BEHANDLING",
                    begrunnelse = "Venter på dokumentasjon fra søker".ikkeSladdet(),
                    erSattPåVent = true,
                    frist = null,
                ),
            ),
        )
}
