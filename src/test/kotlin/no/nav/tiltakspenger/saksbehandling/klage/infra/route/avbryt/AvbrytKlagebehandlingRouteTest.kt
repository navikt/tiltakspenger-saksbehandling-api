package no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgVurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgAvbrytKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgIverksettKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import org.junit.jupiter.api.Test

/**
 * TODO jah: Disse kjører isolert, pga. statisk fnr (kreves for helved sin fnr validator) og statisk tiltaksdeltakelse (gjørejobb). Vi trenger genereringstyper for dette på samme nivå som no.nav.tiltakspenger.saksbehandling.sak.TestSaksnummerGenerator
 */
class AvbrytKlagebehandlingRouteTest {
    @Test
    fun `oppretter klage og deretter avbryter`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagebehandling, json) = opprettSakOgAvbrytKlagebehandling(
                tac = tac,
            )!!
            json.toString().shouldEqualJson(
                """
                   {
                     "id": "${klagebehandling.id}",
                     "sakId": "${sak.id}",
                     "saksnummer": "${sak.saksnummer}",
                     "fnr": "12345678911",
                     "opprettet": "2025-05-01T01:02:07.456789",
                     "sistEndret": "2025-05-01T01:02:08.456789",
                     "iverksattTidspunkt": null,
                     "saksbehandler": "saksbehandlerKlagebehandling",
                     "journalpostId": "12345",
                     "journalpostOpprettet": "2025-05-01T01:02:06.456789",
                     "status": "AVBRUTT",
                     "resultat": "AVVIST",
                     "vedtakDetKlagesPå": null,
                     "erKlagerPartISaken": true,
                     "klagesDetPåKonkreteElementerIVedtaket": true,
                     "erKlagefristenOverholdt": true,
                     "erUnntakForKlagefrist": null,
                     "erKlagenSignert": true,
                     "innsendingsdato": "2026-02-16",
                     "innsendingskilde": "DIGITAL",
                     "brevtekst": [],
                     "avbrutt": {
                          "avbruttAv": "saksbehandlerKlagebehandling",
                          "avbruttTidspunkt": "2025-05-01T01:02:09.456789",
                          "begrunnelse": "begrunnelse for avbryt klagebehandling"
                     },
                     "kanIverksetteVedtak": false,
                     "kanIverksetteOpprettholdelse": false,
                     "årsak": null,
                     "begrunnelse": null,
                     "rammebehandlingId": null,
                     "ventestatus": null,
                     "hjemler": null
                   }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan avbryte hvis vi ikke har knyttet den til en klagebehandling enda`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, rammevedtak, klagebehandling, _) = iverksettSøknadsbehandlingOgVurderKlagebehandling(
                tac = tac,
            )!!
            val (_, oppdatertKlagebehandling, json) = avbrytKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
            )!!
            json.toString().shouldEqualJson(
                """
                   {
                     "id": "${oppdatertKlagebehandling.id}",
                     "sakId": "${sak.id}",
                     "saksnummer": "${sak.saksnummer}",
                     "fnr": "12345678911",
                     "opprettet": "2025-05-01T01:02:33.456789",
                     "sistEndret": "2025-05-01T01:02:35.456789",
                     "iverksattTidspunkt": null,
                     "saksbehandler": "saksbehandlerKlagebehandling",
                     "journalpostId": "12345",
                     "journalpostOpprettet": "2025-05-01T01:02:32.456789",
                     "status": "AVBRUTT",
                     "resultat": "OMGJØR",
                     "vedtakDetKlagesPå": "${rammevedtak.id}",
                     "erKlagerPartISaken": true,
                     "klagesDetPåKonkreteElementerIVedtaket": true,
                     "erKlagefristenOverholdt": true,
                     "erUnntakForKlagefrist": null,
                     "erKlagenSignert": true,
                     "innsendingsdato": "2026-02-16",
                     "innsendingskilde": "DIGITAL",
                     "brevtekst": [],
                     "avbrutt": {
                          "avbruttAv": "saksbehandlerKlagebehandling",
                          "avbruttTidspunkt": "2025-05-01T01:02:36.456789",
                          "begrunnelse": "begrunnelse for avbryt klagebehandling"
                     },
                     "kanIverksetteVedtak": false,
                     "kanIverksetteOpprettholdelse": false,
                     "årsak": "PROSESSUELL_FEIL",
                     "begrunnelse": "Begrunnelse for omgjøring",
                     "rammebehandlingId": null,
                     "ventestatus": null,
                     "hjemler": null
                   }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan avbryte klagebehandling hvis vi har opprettet og avbrutt tilknyttet rammebehandling`() {
        val saksbehandler = ObjectMother.saksbehandler()
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedklagebehandling, _) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
                saksbehandlerSøknadsbehandling = saksbehandler,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!
            val klagebehandling = rammebehandlingMedklagebehandling.klagebehandling!!
            val (_, _, _, _) = avbrytRammebehandling(
                tac = tac,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                rammebehandlingId = rammebehandlingMedklagebehandling.id,
                saksbehandler = saksbehandler,
            )!!
            val (_, oppdatertKlagebehandling, json) = avbrytKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = saksbehandler,
            )!!
            json.toString().shouldEqualJson(
                """
                   {
                     "id": "${oppdatertKlagebehandling.id}",
                     "sakId": "${sak.id}",
                     "saksnummer": "${sak.saksnummer}",
                     "fnr": "12345678911",
                     "opprettet": "2025-05-01T01:02:33.456789",
                     "sistEndret": "2025-05-01T01:02:58.456789",
                     "iverksattTidspunkt": null,
                     "saksbehandler": "Z12345",
                     "journalpostId": "12345",
                     "journalpostOpprettet": "2025-05-01T01:02:32.456789",
                     "status": "AVBRUTT",
                     "resultat": "OMGJØR",
                     "vedtakDetKlagesPå": "${sak.rammevedtaksliste.single().id}",
                     "erKlagerPartISaken": true,
                     "klagesDetPåKonkreteElementerIVedtaket": true,
                     "erKlagefristenOverholdt": true,
                     "erUnntakForKlagefrist": null,
                     "erKlagenSignert": true,
                     "innsendingsdato": "2026-02-16",
                     "innsendingskilde": "DIGITAL",
                     "brevtekst": [],
                     "avbrutt": {
                          "avbruttAv": "Z12345",
                          "avbruttTidspunkt": "2025-05-01T01:02:59.456789",
                          "begrunnelse": "begrunnelse for avbryt klagebehandling"
                     },
                     "kanIverksetteVedtak": false,
                     "kanIverksetteOpprettholdelse": false,
                     "årsak": "PROSESSUELL_FEIL",
                     "begrunnelse": "Begrunnelse for omgjøring",
                     "rammebehandlingId": null,
                     "ventestatus": null,
                     "hjemler": null
                   }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan ikke avbryte hvis åpen tilknyttet rammebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, _) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            val rammebehandlingId = sak.rammebehandlinger[1].id
            avbrytKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                        "melding": "Klagebehandlingen kan ikke avbrytes fordi den er knyttet til en rammebehandling som ikke er avbrutt: $rammebehandlingId",
                        "kode": "knyttet_til_ikke_avbrutt_rammebehandling"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke avbryte vedtatt avvist klage`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagevedtak, _) = opprettSakOgIverksettKlagebehandling(tac = tac)!!
            val klagebehandling = klagevedtak.behandling
            avbrytKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                      "melding": "Klagebehandlingen er allerede avsluttet med status: VEDTATT",
                      "kode": "allerede_avsluttet"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke avbryte avbrutt klage`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagebehandling, _) = opprettSakOgAvbrytKlagebehandling(tac = tac)!!
            avbrytKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                      "melding": "Klagebehandlingen er allerede avsluttet med status: AVBRUTT",
                      "kode": "allerede_avsluttet"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke avbryte vedtatt omgjort klage`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, _) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            val saksbehandler = ObjectMother.saksbehandler(klagebehandling.saksbehandler!!)
            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
            )
            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
            )
            val beslutter = ObjectMother.beslutter()
            taBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = beslutter,
            )
            iverksettForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                beslutter = beslutter,
            )
            avbrytKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                      "melding": "Klagebehandlingen er allerede avsluttet med status: VEDTATT",
                      "kode": "allerede_avsluttet"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }
}
