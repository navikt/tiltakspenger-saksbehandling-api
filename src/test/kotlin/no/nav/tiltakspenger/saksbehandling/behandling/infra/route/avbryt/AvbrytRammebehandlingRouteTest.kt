package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.avbryt

import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.shouldBeSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgAvbryt
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

            json.get("behandlinger").single().toString().shouldBeSøknadsbehandlingDTO(
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
}
