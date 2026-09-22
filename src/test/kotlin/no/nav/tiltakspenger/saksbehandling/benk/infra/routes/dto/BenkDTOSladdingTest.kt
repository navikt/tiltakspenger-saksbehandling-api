package no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.common.januarDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommandoDTO
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Radene bygges direkte som wiretyper.
 * Benkens domenetyper leses ut av en aggregatspørring og har ingen ObjectMother, og sladdingen bryr seg bare om feltene.
 */
class BenkDTOSladdingTest {

    @Test
    fun `fødselsnummer og ventebegrunnelse sladdes i alle radtypene`() {
        val benkResponsDTO = benkResponsDTO()

        benkResponsDTO.sladdet().oversikt.behandlinger.forEach {
            it.fnr shouldBe SladdetVerdi
            it.ventestatus.begrunnelse shouldBe SladdetVerdi
        }
    }

    @Test
    fun `sladding fjerner muterende kommandoer i alle radtypene`() {
        val kommandoer = listOf(SaksbehandlerBehandlingKommandoDTO.LeggTilbakeSaksbehandler)
        val respons = benkResponsDTO(gyldigeKommandoer = kommandoer)

        respons.kommandoerPerRad() shouldBe List(4) { kommandoer }
        respons.sladdet().kommandoerPerRad() shouldBe List(4) { emptyList() }
    }

    @Test
    fun `alle radtypene er representert i testdataene`() {
        benkResponsDTO().oversikt.behandlinger.map { it.type } shouldBe listOf(
            BenkBehandlingstypeDTO.SØKNADSBEHANDLING,
            BenkBehandlingstypeDTO.REVURDERING,
            BenkBehandlingstypeDTO.MELDEKORTBEHANDLING,
            BenkBehandlingstypeDTO.KLAGEBEHANDLING,
            BenkBehandlingstypeDTO.TILBAKEKREVING,
        )
    }

    @Test
    fun `antall, identer, statuser og fanen forblir uendret`() {
        val benkResponsDTO = benkResponsDTO()

        benkResponsDTO.sladdet().let {
            it.harTilgang shouldBe benkResponsDTO.harTilgang
            it.tab shouldBe benkResponsDTO.tab
            it.antallPerTab shouldBe benkResponsDTO.antallPerTab
            it.oversikt.totalAntall shouldBe benkResponsDTO.oversikt.totalAntall
            it.oversikt.oppsummering shouldBe benkResponsDTO.oversikt.oppsummering
            it.oversikt.saksbehandlere shouldBe benkResponsDTO.oversikt.saksbehandlere
            it.oversikt.besluttere shouldBe benkResponsDTO.oversikt.besluttere
            it.oversikt.behandlinger.map { rad -> rad.tilgang } shouldBe
                benkResponsDTO.oversikt.behandlinger.map { rad -> rad.tilgang }
            it.oversikt.behandlinger.map { rad -> rad.personmarkører } shouldBe
                benkResponsDTO.oversikt.behandlinger.map { rad -> rad.personmarkører }
            it.oversikt.behandlinger.map { rad -> rad.id } shouldBe
                benkResponsDTO.oversikt.behandlinger.map { rad -> rad.id }
        }
    }

    @Test
    fun `ventebegrunnelse som mangler sladdes på linje med de andre`() {
        val utenVentebegrunnelse = benkResponsDTO().let { respons ->
            respons.copy(
                oversikt = respons.oversikt.copy(
                    behandlinger = listOf(
                        benkSøknadsbehandlingDTO().copy(
                            ventestatus = BenkVentestatusDTO(
                                erSattPåVent = false,
                                begrunnelse = null.ikkeSladdet(),
                                frist = null,
                            ),
                        ),
                    ),
                ),
            )
        }

        utenVentebegrunnelse.sladdet().oversikt.behandlinger.single().ventestatus.begrunnelse shouldBe SladdetVerdi
    }

    @Test
    fun `sakId og saksnummer sladdes i alle radtypene`() {
        benkResponsDTO().oversikt.behandlinger.forEach {
            it.sladdet().let { rad ->
                rad.sakId shouldBe SladdetVerdi
                rad.saksnummer shouldBe SladdetVerdi
            }
        }
    }

    @Test
    fun `veileder får urørt benk, mens utvikler får den sladdet`() {
        val benkResponsDTO = benkResponsDTO()

        benkResponsDTO.sladdetFor(ObjectMother.veileder()) shouldBe benkResponsDTO
        benkResponsDTO.sladdetFor(ObjectMother.utvikler()) shouldBe benkResponsDTO.sladdet()
    }

    private fun BenkResponsMedTilgangDTO.kommandoerPerRad(): List<List<SaksbehandlerBehandlingKommandoDTO>> =
        oversikt.behandlinger.mapNotNull {
            when (it) {
                is BenkSøknadsbehandlingDTO -> it.gyldigeKommandoer
                is BenkRevurderingDTO -> it.gyldigeKommandoer
                is BenkMeldekortDTO -> it.gyldigeKommandoer
                is BenkTilbakekrevingDTO -> it.gyldigeKommandoer
                is BenkKlagebehandlingDTO -> null
            }
        }

    private fun benkResponsDTO(
        gyldigeKommandoer: List<SaksbehandlerBehandlingKommandoDTO> = emptyList(),
    ): BenkResponsMedTilgangDTO = BenkResponsMedTilgangDTO(
        tab = BenkFaneDTO.SØKNADER,
        antallPerTab = mapOf(
            BenkFaneDTO.SØKNADER to 1,
            BenkFaneDTO.REVURDERINGER to 1,
            BenkFaneDTO.MELDEKORT to 1,
            BenkFaneDTO.KLAGE to 1,
            BenkFaneDTO.TILBAKEKREVING to 1,
        ),
        oversikt = BenkOversiktDTO(
            behandlinger = listOf(
                benkSøknadsbehandlingDTO(gyldigeKommandoer),
                BenkRevurderingDTO(
                    id = "revurdering",
                    sakId = "sakId".ikkeSladdet(),
                    fnr = Fnr.random().verdi.ikkeSladdet(),
                    saksnummer = "saksnummer".ikkeSladdet(),
                    startet = 1.januarDateTime(2025).toString(),
                    sistEndret = 1.januarDateTime(2025).toString(),
                    saksbehandler = "Z12345",
                    beslutter = null,
                    erUnderkjent = false,
                    ventestatus = ventestatusDTO(),
                    tilgang = tilgangDTO(),
                    personmarkører = personmarkørerDTO(),
                    status = BenkBehandlingsstatusDTO.UNDER_BEHANDLING,
                    resultat = BenkRevurderingResultatDTO.STANS,
                    gyldigeKommandoer = gyldigeKommandoer,
                ),
                BenkMeldekortDTO(
                    type = BenkBehandlingstypeDTO.MELDEKORTBEHANDLING,
                    id = "meldekort",
                    sakId = "sakId".ikkeSladdet(),
                    fnr = Fnr.random().verdi.ikkeSladdet(),
                    saksnummer = "saksnummer".ikkeSladdet(),
                    startet = 1.januarDateTime(2025).toString(),
                    sistEndret = 1.januarDateTime(2025).toString(),
                    saksbehandler = "Z12345",
                    beslutter = null,
                    erUnderkjent = false,
                    ventestatus = ventestatusDTO(),
                    tilgang = tilgangDTO(),
                    personmarkører = personmarkørerDTO(),
                    status = BenkBehandlingsstatusDTO.UNDER_BEHANDLING,
                    meldeperioder = listOf(PeriodeDTO(1.januar(2025).toString(), 14.januar(2025).toString())),
                    beløp = 1000,
                    gyldigeKommandoer = gyldigeKommandoer,
                ),
                BenkKlagebehandlingDTO(
                    id = "klage",
                    sakId = "sakId".ikkeSladdet(),
                    fnr = Fnr.random().verdi.ikkeSladdet(),
                    saksnummer = "saksnummer".ikkeSladdet(),
                    startet = 1.januarDateTime(2025).toString(),
                    sistEndret = 1.januarDateTime(2025).toString(),
                    saksbehandler = "Z12345",
                    beslutter = null,
                    erUnderkjent = false,
                    ventestatus = ventestatusDTO(),
                    tilgang = tilgangDTO(),
                    personmarkører = personmarkørerDTO(),
                    status = BenkKlagebehandlingStatusDTO.UNDER_BEHANDLING,
                    kravtidspunkt = 1.januarDateTime(2025).toString(),
                    resultat = null,
                ),
                BenkTilbakekrevingDTO(
                    id = "tilbakekreving",
                    sakId = "sakId".ikkeSladdet(),
                    fnr = Fnr.random().verdi.ikkeSladdet(),
                    saksnummer = "saksnummer".ikkeSladdet(),
                    startet = 1.januarDateTime(2025).toString(),
                    sistEndret = 1.januarDateTime(2025).toString(),
                    saksbehandler = "Z12345",
                    beslutter = null,
                    erUnderkjent = false,
                    ventestatus = ventestatusDTO(),
                    tilgang = tilgangDTO(),
                    personmarkører = personmarkørerDTO(),
                    status = BenkTilbakekrevingStatusDTO.UNDER_BEHANDLING,
                    beløp = BigDecimal.TEN,
                    kilde = BenkTilbakekrevingKildeDTO.MELDEKORT,
                    kravgrunnlagPeriode = PeriodeDTO(1.januar(2025).toString(), 14.januar(2025).toString()),
                    url = "https://tilbakekreving.example.test",
                    gyldigeKommandoer = gyldigeKommandoer,
                ),
            ),
            totalAntall = 5,
            totalAntallUfiltrert = 5,
            oppsummering = BenkOppsummeringDTO(
                antallMedTilgang = 5,
                antallUtenTilgang = 0,
                antallSkjermet = 0,
                antallKode6 = 0,
                antallKode7 = 0,
            ),
            side = 0,
            sideantall = 200,
            saksbehandlere = listOf("Z12345"),
            besluttere = emptyList(),
        ),
        error = null,
    )

    private fun benkSøknadsbehandlingDTO(
        gyldigeKommandoer: List<SaksbehandlerBehandlingKommandoDTO> = emptyList(),
    ): BenkSøknadsbehandlingDTO = BenkSøknadsbehandlingDTO(
        id = "søknadsbehandling",
        sakId = "sakId".ikkeSladdet(),
        fnr = Fnr.random().verdi.ikkeSladdet(),
        saksnummer = "saksnummer".ikkeSladdet(),
        startet = 1.januarDateTime(2025).toString(),
        sistEndret = 1.januarDateTime(2025).toString(),
        saksbehandler = "Z12345",
        beslutter = null,
        erUnderkjent = false,
        ventestatus = ventestatusDTO(),
        tilgang = tilgangDTO(),
        personmarkører = personmarkørerDTO(),
        status = BenkBehandlingsstatusDTO.UNDER_BEHANDLING,
        søknadstype = BenkSøknadstypeDTO.DIGITAL,
        kravtidspunkt = 1.januarDateTime(2025).toString(),
        resultat = BenkSøknadsbehandlingResultatDTO.IKKE_VALGT,
        gyldigeKommandoer = gyldigeKommandoer,
    )

    private fun tilgangDTO(): BenkTilgangDTO = BenkTilgangDTO(
        vurdering = BenkTilgangsvurderingDTO.HAR_TILGANG,
        grunn = null,
    )

    private fun personmarkørerDTO(): BenkPersonmarkørerDTO = BenkPersonmarkørerDTO(
        skjermet = false,
        kode6 = false,
        kode7 = false,
    )

    private fun ventestatusDTO(): BenkVentestatusDTO = BenkVentestatusDTO(
        erSattPåVent = true,
        begrunnelse = "Venter på dokumentasjon fra bostedet".ikkeSladdet(),
        frist = 1.januar(2025).toString(),
    )
}
