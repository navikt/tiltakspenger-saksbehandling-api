package no.nav.tiltakspenger.saksbehandling.klage.infra.route.gjenoppta

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.shouldBeSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.gjenopptaRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgGjenopptaKlagebehandlingMedRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class GjenopptaKlagebehandlingMedRammebehandlingRouteTest {

    @Test
    fun `kan gjenoppta klagebehandling med rammebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgGjenopptaKlagebehandlingMedRammebehandling(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = "OMGJØR",
                vedtakDetKlagesPå = sak.rammevedtaksliste.first().id.toString(),
                behandlingDetKlagesPå = sak.rammevedtaksliste.first().behandlingId.toString(),
                status = "UNDER_BEHANDLING",
                kanIverksetteVedtak = null,
                årsak = "PROSESSUELL_FEIL",
                begrunnelse = "Begrunnelse for omgjøring",
                rammebehandlingId = listOf(rammebehandlingMedKlagebehandling.id.toString()),
                åpenRammebehandlingId = rammebehandlingMedKlagebehandling.id.toString(),
                //language=json
                ventestatus = listOf(
                    """{"sattPåVentAv": "saksbehandlerKlagebehandling","status": "UNDER_BEHANDLING","tidspunkt": "TIMESTAMP","begrunnelse": "begrunnelse for å sette klage på vent","erSattPåVent": true,"frist": "2025-01-14"}""",
                    """{"sattPåVentAv": "saksbehandlerKlagebehandling","status": "KLAR_TIL_BEHANDLING","tidspunkt": "TIMESTAMP","begrunnelse": "","erSattPåVent": false,"frist": null}""",
                ),
            )
            klagebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "begrunnelse for å sette klage på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = 14.januar(2025),
                        ),
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusSeconds(1),
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )

            rammebehandlingMedKlagebehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            rammebehandlingMedKlagebehandling.saksbehandler shouldBe "saksbehandlerKlagebehandling"

            rammebehandlingMedKlagebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "begrunnelse for å sette klage på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = LocalDate.parse("2025-01-14"),
                        ),
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusSeconds(1),
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `kan gjenoppta rammebehandling tilknyttet klage`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, rammebehandlingMedKlagebehandling, _) = iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!
            val (_, _, oppdatertRammebehandlingMedKlagebehandling, json) = gjenopptaRammebehandling(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
            )!!

            val klagebehandling = oppdatertRammebehandlingMedKlagebehandling.klagebehandling!!

            json.toString().shouldBeSøknadsbehandlingDTO(
                behandlingId = oppdatertRammebehandlingMedKlagebehandling.id,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                søknadId = (rammebehandlingMedKlagebehandling as Søknadsbehandling).søknad.id,
                saksnummer = sak.saksnummer,
                iverksattTidspunkt = null,
                vedtaksperiode = null,
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = RammebehandlingResultatTypeDTO.IKKE_VALGT,
                beslutter = null,
                //language=json
                ventestatus = listOf(
                    """{"sattPåVentAv": "saksbehandlerKlagebehandling", "status": "UNDER_BEHANDLING", "tidspunkt": "TIMESTAMP","begrunnelse": "begrunnelse for å sette klage på vent","erSattPåVent": true,"frist": "2025-01-14"}""",
                    """{"sattPåVentAv": "saksbehandlerKlagebehandling","status": "KLAR_TIL_BEHANDLING","tidspunkt": "TIMESTAMP","begrunnelse": "","erSattPåVent": false,"frist": null}""",
                ),
                status = "UNDER_BEHANDLING",
                eksternDeltagelseId = "61328250-7d5d-4961-b70e-5cb727a34371",
                internDeltakelseId = "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0",
                søknadTiltakId = "61328250-7d5d-4961-b70e-5cb727a34371",
                innvilgelsesperiode = false,
                barnetillegg = false,
            )

            klagebehandling.status shouldBe Klagebehandlingsstatus.UNDER_BEHANDLING
            klagebehandling.saksbehandler shouldBe "saksbehandlerKlagebehandling"
            klagebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "begrunnelse for å sette klage på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = 14.januar(2025),
                        ),
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusSeconds(1),
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )
        }
    }
}
