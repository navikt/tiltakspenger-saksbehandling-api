package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.gjenoppta

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.shouldBeSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgAvbryt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderAutomatiskBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GjenopptaRammebehandlingRouteTest : GjenopptaRammebehandlingBuilder {
    @Test
    fun `gjenoppta søknadsbehandling`() {
        withTestApplicationContext { tac ->
            val (sak, søknad, søknadsbehandling, json) = opprettSøknadsbehandlingOgGjenoppta(tac = tac)!!

            json.toString().shouldBeSøknadsbehandlingDTO(
                gyldigeKommandoer = listOf("LeggTilbakeSaksbehandler", "SettPåVent", "Avbryt"),
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
                //language=json
                ventestatus = listOf(
                    """{"sattPåVentAv": "Z12345","status": "KLAR_TIL_BEHANDLING","tidspunkt": "2025-05-01T01:02:16.456789","begrunnelse": "","erSattPåVent": false,"frist": null}""",
                    """{"sattPåVentAv": "Z12345", "status": "UNDER_BEHANDLING","tidspunkt": "2025-05-01T01:02:17.456789","begrunnelse": "Begrunnelse for å sette rammebehandling på vent","erSattPåVent": true,"frist": null}""",
                ),
                status = "UNDER_BEHANDLING",
                eksternDeltagelseId = "ekstern_tiltaksdeltakelse_id_1",
                internDeltakelseId = "${søknad.tiltak!!.tiltaksdeltakerId}",
                søknadTiltakId = "ekstern_tiltaksdeltakelse_id_1",
                innvilgelsesperiode = false,
                barnetillegg = false,
            )
        }
    }

    @Test
    fun `kan gjenoppta søknadsbehandling under beslutning som er satt på vent`() {
        withTestApplicationContext { tac ->
            val beslutter = ObjectMother.beslutter()
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            taBehandling(tac, sak.id, behandlingId, beslutter)!!
            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                saksbehandler = beslutter,
            )!!

            gjenopptaRammebehandling(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                saksbehandler = beslutter,
            )!!

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                it.ventestatus.erSattPåVent shouldBe false
                it.saksbehandler shouldBe "Z12345"
                it.beslutter shouldBe beslutter.navIdent
            }
        }
    }

    /**
     * En automatisk behandling settes på vent av den automatiske saksbehandlingen, ikke via en route.
     * Tilstanden bygges derfor gjennom [no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.DelautomatiskBehandlingService], som er prodstien for den.
     */
    @Test
    fun `saksbehandler kan gjenoppta en automatisk behandling som er satt på vent`() {
        withTestApplicationContext { tac ->
            val iDag = LocalDate.now(tac.clock)
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
                tac = tac,
                tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
                    fom = iDag.plusDays(3),
                    tom = iDag.plusMonths(3),
                    status = TiltakDeltakerstatus.VenterPåOppstart,
                ),
            )
            tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(
                behandling,
                CorrelationId.generate(),
            )
            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                it.ventestatus.erSattPåVent shouldBe true
            }

            gjenopptaRammebehandling(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandling.id,
            )!!

            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).also {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.ventestatus.erSattPåVent shouldBe false
                it.saksbehandler shouldBe "Z12345"
                it.beslutter shouldBe null
            }
        }
    }

    @Test
    fun `kan ikke gjenoppta en behandling som ikke er satt på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            gjenopptaRammebehandling(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandling.id,
                forventet = ikkePåVent,
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke gjenoppta en vedtatt behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, rammevedtak, _) = iverksettSøknadsbehandling(tac)

            gjenopptaRammebehandling(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = rammevedtak.behandlingId,
                forventet = ikkePåVent,
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke gjenoppta en avbrutt behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, søknadsbehandling, _) = opprettSøknadsbehandlingOgAvbryt(tac)!!

            gjenopptaRammebehandling(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = søknadsbehandling!!.id,
                forventet = ikkePåVent,
            ) shouldBe null
        }
    }

    @Test
    fun `en bruker uten beslutterrolle kan ikke gjenoppta en behandling som venter på beslutning`() {
        withTestApplicationContext { tac ->
            val beslutter = ObjectMother.beslutter()
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
            taBehandling(tac, sak.id, behandlingId, beslutter)!!
            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                saksbehandler = beslutter,
            )!!

            gjenopptaRammebehandling(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = behandlingId,
                saksbehandler = ObjectMother.saksbehandler123(),
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være beslutter for å gjenoppta denne behandlingen.",
                      "kode": "maa_vaere_beslutter"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).ventestatus.erSattPåVent shouldBe true
        }
    }

    private val ikkePåVent = ForventetRespons.json(
        400,
        """
        {
          "melding": "Behandlingen er ikke satt på vent, og kan derfor ikke gjenopptas.",
          "kode": "behandlingen_er_ikke_paa_vent"
        }
        """.trimIndent(),
        "application/json; charset=UTF-8",
    )
}
