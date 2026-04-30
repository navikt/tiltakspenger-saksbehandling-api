package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.gjenoppta

import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.shouldBeSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import org.junit.jupiter.api.Test

class GjenopptaRammebehandlingRouteTest : GjenopptaRammebehandlingBuilder {
    @Test
    fun `gjenoppta søknadsbehandling`() {
        withTestApplicationContext { tac ->
            val (sak, søknad, søknadsbehandling, json) = opprettSøknadsbehandlingOgGjenoppta(tac = tac)!!

            json.toString().shouldBeSøknadsbehandlingDTO(
                behandlingId = søknadsbehandling!!.id,
                sakId = sak.id,
                klagebehandlingId = null,
                søknadId = søknad.id,
                saksnummer = Saksnummer("202505011001"),
                iverksattTidspunkt = null,
                vedtaksperiode = null,
                saksbehandler = "Z12345",
                resultat = RammebehandlingResultatTypeDTO.IKKE_VALGT,
                beslutter = null,
                //language=json
                ventestatus = listOf(
                    """{"sattPåVentAv": "Z12345", "status": "UNDER_BEHANDLING","tidspunkt": "2025-05-01T01:02:17.456789","begrunnelse": "Begrunnelse for å sette rammebehandling på vent","erSattPåVent": true,"frist": null}""",
                    """{"sattPåVentAv": "Z12345","status": "KLAR_TIL_BEHANDLING","tidspunkt": "2025-05-01T01:02:16.456789","begrunnelse": "","erSattPåVent": false,"frist": null}""",
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
}
