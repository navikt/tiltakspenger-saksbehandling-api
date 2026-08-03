package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.avbryt

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.shouldBeSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgAvbryt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderAutomatiskBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.søknad.shouldBeSøknadDTO
import org.junit.jupiter.api.Test

class AvbrytRammebehandlingRouteTest {
    @Test
    fun `oppretter søknadsbehandling og deretter avbryter`() {
        withTestApplicationContext { tac ->
            val (sak, søknad, søknadsbehandling, json) = opprettSøknadsbehandlingOgAvbryt(
                tac = tac,
            )!!

            json.get("søknader").single().toString().shouldBeSøknadDTO(
                søknadId = søknad.id,
                journalpostId = "123456789",
                tiltakId = søknad.tiltak!!.id,
                tiltakFraOgMed = "2023-01-01",
                tiltakTilOgMed = "2023-03-31",
                tiltakTypeKode = "GRUPPEAMO",
                tiltakTypeNavn = "Arbeidsmarkedsoppfølging gruppe",
                manueltSattTiltak = null,
                søknadstype = "DIGITAL",
                barnetillegg = emptyList(),
                antallVedlegg = 0,
                avbruttAv = "Z12345",
                avbruttBegrunnelse = "begrunnelse for avbryt søknad og/eller rammebehandling",
                kanInnvilges = true,
                behandlingsarsak = null,
            )

            json.get("rammebehandlinger").single().toString().shouldBeSøknadsbehandlingDTO(
                behandlingId = søknadsbehandling!!.id,
                sakId = sak.id,
                klagebehandlingId = null,
                søknadId = søknad.id,
                saksnummer = sak.saksnummer,
                iverksattTidspunkt = null,
                vedtaksperiode = null,
                saksbehandler = "Z12345",
                resultat = RammebehandlingResultatTypeDTO.IKKE_VALGT,
                beslutter = null,
                ventestatus = emptyList(),
                status = "AVBRUTT",
                eksternDeltagelseId = "ekstern_tiltaksdeltakelse_id_1",
                internDeltakelseId = "${søknad.tiltak!!.tiltaksdeltakerId}",
                søknadTiltakId = "ekstern_tiltaksdeltakelse_id_1",
                innvilgelsesperiode = false,
                barnetillegg = false,
                avbrutt = """{"avbruttAv": "Z12345","avbruttTidspunkt": "2025-05-01T01:02:13.456789","begrunnelse": "begrunnelse for avbryt søknad og/eller rammebehandling"}""",
            )
        }
    }

    @Test
    fun `avbryter en automatisk opprettet behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(tac)

            val (_, søknad, avbruttBehandling) = avbrytRammebehandling(
                tac = tac,
                saksnummer = sak.saksnummer,
                sakId = sak.id,
                rammebehandlingId = behandling.id,
            )!!

            avbruttBehandling!!.status shouldBe Rammebehandlingsstatus.AVBRUTT
            avbruttBehandling.avbrutt!!.saksbehandler shouldBe "Z12345"
            avbruttBehandling.avbrutt!!.begrunnelse.value shouldBe "begrunnelse for avbryt søknad og/eller rammebehandling"
            søknad.avbrutt shouldBe avbruttBehandling.avbrutt
        }
    }

    @Test
    fun `tildelt saksbehandler kan avbryte en behandling som er klar til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)

            avbrytRammebehandling(
                tac = tac,
                saksnummer = sak.saksnummer,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
            )!!

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).status shouldBe Rammebehandlingsstatus.AVBRUTT
        }
    }

    @Test
    fun `en annen saksbehandler kan ikke avbryte en behandling som er klar til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)

            avbrytRammebehandling(
                tac = tac,
                saksnummer = sak.saksnummer,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                saksbehandler = ObjectMother.saksbehandler123(),
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Behandlingen tilhører en annen saksbehandler. Overta behandlingen og prøv igjen.",
                      "kode": "behandlingen_tildelt_annen_saksbehandler"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        }
    }

    @Test
    fun `tildelt beslutter kan avbryte en behandling som er under beslutning`() {
        withTestApplicationContext { tac ->
            // Avbryt-ruta krever saksbehandlerrolle, så beslutteren må ha begge rollene.
            val beslutter = ObjectMother.saksbehandlerOgBeslutter()
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            taBehandling(tac, sak.id, behandlingId, beslutter)!!

            avbrytRammebehandling(
                tac = tac,
                saksnummer = sak.saksnummer,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                saksbehandler = beslutter,
            )!!

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).status shouldBe Rammebehandlingsstatus.AVBRUTT
        }
    }

    @Test
    fun `saksbehandleren kan ikke avbryte en behandling som er under beslutning hos en beslutter`() {
        withTestApplicationContext { tac ->
            val beslutter = ObjectMother.saksbehandlerOgBeslutter()
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            taBehandling(tac, sak.id, behandlingId, beslutter)!!

            avbrytRammebehandling(
                tac = tac,
                saksnummer = sak.saksnummer,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Behandlingen tilhører en annen beslutter. Overta behandlingen og prøv igjen.",
                      "kode": "behandlingen_tildelt_annen_beslutter"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
        }
    }

    @Test
    fun `avbryter en behandling som allerede er avbrutt gir 409 Conflict`() {
        withTestApplicationContext { tac ->
            val (sak, _, søknadsbehandling, _) = opprettSøknadsbehandlingOgAvbryt(
                tac = tac,
            )!!

            avbrytRammebehandling(
                tac = tac,
                saksnummer = sak.saksnummer,
                sakId = sak.id,
                rammebehandlingId = søknadsbehandling!!.id,
                forventet = ForventetRespons.json(
                    409,
                    """
                    {
                      "melding": "Behandlingen er allerede avsluttet.",
                      "kode": "behandling_kan_ikke_avbrytes_i_tilstanden"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            )
        }
    }
}
