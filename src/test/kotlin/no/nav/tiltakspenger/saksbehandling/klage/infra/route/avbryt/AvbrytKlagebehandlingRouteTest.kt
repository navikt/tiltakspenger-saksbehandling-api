package no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbruttKlagebehandlingStatus
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbruttKlagebehandlng
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytKlagebehandlingForSak
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgVurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgIverksettKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettetRammebehandlingMedOpprettholdtKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettetRevurderingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettetSøknadsbehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import org.junit.jupiter.api.Test

/**
 * TODO jah: Disse kjører isolert, pga. statisk fnr (kreves for helved sin fnr validator) og statisk tiltaksdeltakelse (gjørejobb).
 * Vi trenger genereringstyper for dette på samme nivå som no.nav.tiltakspenger.saksbehandling.sak.TestSaksnummerGenerator
 */
class AvbrytKlagebehandlingRouteTest {
    @Test
    fun `oppretter klage og deretter avbryter`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagebehandling, json) = avbruttKlagebehandlng(
                tac = tac,
            )!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksnummer = Saksnummer("202505011001"),
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = "AVVIST",
                vedtakDetKlagesPå = null,
                status = "AVBRUTT",
                avbrutt = """{"avbruttAv": "saksbehandlerKlagebehandling","avbruttTidspunkt": "TIMESTAMP","status": "ANNET","begrunnelse": "begrunnelse for avbryt klagebehandling"}""",
            )
        }
    }

    @Test
    fun `kan avbryte hvis vi ikke har knyttet den til en rammebehandling enda`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, rammevedtak, klagebehandling, _) = iverksettSøknadsbehandlingOgVurderKlagebehandling(
                tac = tac,
            )!!
            val (_, _, json) = avbrytKlagebehandlingForSak(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
            )!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksnummer = Saksnummer("202505011001"),
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = "OMGJØR",
                vedtakDetKlagesPå = "${rammevedtak.id}",
                behandlingDetKlagesPå = "${rammevedtak.behandlingId}",
                status = "AVBRUTT",
                årsak = "PROSESSUELL_FEIL",
                begrunnelse = "Begrunnelse for omgjøring",
                avbrutt = """{"avbruttAv": "saksbehandlerKlagebehandling","avbruttTidspunkt": "TIMESTAMP","status": "ANNET","begrunnelse": "begrunnelse for avbryt klagebehandling"}""",
            )
        }
    }

    @Test
    fun `kan avbryte rammebehandling vi omgjør etter KA`() {
        val saksbehandler = ObjectMother.saksbehandler()
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, _, _) = opprettetRammebehandlingMedOpprettholdtKlage(
                tac = tac,
                saksbehandler = saksbehandler,
            )!!
            val (_, _, rammebehandling, _) = avbrytRammebehandling(
                tac = tac,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                rammebehandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
            )!!

            rammebehandling!!.status shouldBe Rammebehandlingsstatus.AVBRUTT
        }
    }

    @Test
    fun `kan avbryte klagebehandling hvis vi har opprettet og avbrutt tilknyttet rammebehandling (søknadsbehandling)`() {
        val saksbehandler = ObjectMother.saksbehandler()
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedklagebehandling, _) = opprettetSøknadsbehandlingForKlage(
                tac = tac,
                saksbehandlerSøknadsbehandling = saksbehandler,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!
            val klagebehandling = rammebehandlingMedklagebehandling.klagebehandling!!
            avbrytRammebehandling(
                tac = tac,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                rammebehandlingId = rammebehandlingMedklagebehandling.id,
                saksbehandler = saksbehandler,
            )!!
            val (_, _, json) = avbrytKlagebehandlingForSak(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = saksbehandler,
            )!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksnummer = Saksnummer("202505011001"),
                fnr = "12345678911",
                saksbehandler = "Z12345",
                resultat = "OMGJØR",
                vedtakDetKlagesPå = sak.rammevedtaksliste.single().id.toString(),
                behandlingDetKlagesPå = sak.rammevedtaksliste.single().behandlingId.toString(),
                status = "AVBRUTT",
                årsak = "PROSESSUELL_FEIL",
                begrunnelse = "Begrunnelse for omgjøring",
                avbrutt = """{"avbruttAv": "Z12345","avbruttTidspunkt": "TIMESTAMP","status": "ANNET","begrunnelse": "begrunnelse for avbryt klagebehandling"}""",
            )
        }
    }

    @Test
    fun `kan avbryte klagebehandling hvis vi har opprettet og avbrutt tilknyttet rammebehandling (revurdering)`() {
        val saksbehandler = ObjectMother.saksbehandler()
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedklagebehandling, _) = opprettetRevurderingForKlage(
                tac = tac,
                saksbehandlerSøknadsbehandling = saksbehandler,
                saksbehandlerKlagebehandling = saksbehandler,
                type = "REVURDERING_OMGJØRING",
            )!!
            val klagebehandling = rammebehandlingMedklagebehandling.klagebehandling!!
            avbrytRammebehandling(
                tac = tac,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                rammebehandlingId = rammebehandlingMedklagebehandling.id,
                saksbehandler = saksbehandler,
            )!!
            val (_, _, json) = avbrytKlagebehandlingForSak(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = saksbehandler,
            )!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksnummer = Saksnummer("202505011001"),
                fnr = "12345678911",
                saksbehandler = "Z12345",
                resultat = "OMGJØR",
                vedtakDetKlagesPå = sak.rammevedtaksliste.single().id.toString(),
                behandlingDetKlagesPå = klagebehandling.formkrav.behandlingDetKlagesPå?.toString(),
                status = "AVBRUTT",
                årsak = "PROSESSUELL_FEIL",
                begrunnelse = "Begrunnelse for omgjøring",
                avbrutt = """{"avbruttAv": "Z12345","avbruttTidspunkt": "TIMESTAMP","status": "ANNET","begrunnelse": "begrunnelse for avbryt klagebehandling"}""",
            )
        }
    }

    @Test
    fun `kan ikke avbryte hvis åpen tilknyttet rammebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, _) = opprettetSøknadsbehandlingForKlage(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            avbrytKlagebehandlingForSak(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                        "melding": "Klagebehandlingen er knyttet til en annen behandling. Avslutt den andre behandlingen først.",
                        "kode": "knyttet_til_ikke_avbrutt_behandling"
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
            avbrytKlagebehandlingForSak(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.Conflict,
                forventetJsonBody = {
                    """
                     {
                      "melding": "Klagebehandlingen er allerede avsluttet",
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
            val (sak, klagebehandling, _) = avbruttKlagebehandlng(tac = tac)!!
            avbrytKlagebehandlingForSak(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.Conflict,
                forventetJsonBody = {
                    """
                     {
                      "melding": "Klagebehandlingen er allerede avsluttet",
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
            val (sak, rammebehandlingMedKlagebehandling, _) = opprettetSøknadsbehandlingForKlage(
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
            avbrytKlagebehandlingForSak(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.Conflict,
                forventetJsonBody = {
                    """
                     {
                      "melding": "Klagebehandlingen er allerede avsluttet",
                      "kode": "allerede_avsluttet"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }

    @Test
    fun `kan avbryte med status KLAGE_TRUKKET`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagebehandling, _) = opprettSakOgKlagebehandlingTilAvvisning(tac = tac)!!
            val (_, _, json) = avbrytKlagebehandlingForSak(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                avbruttStatus = AvbruttKlagebehandlingStatus.KLAGE_TRUKKET,
                begrunnelse = null,
            )!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksnummer = Saksnummer("202505011001"),
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = "AVVIST",
                vedtakDetKlagesPå = null,
                status = "AVBRUTT",
                avbrutt = """{"avbruttAv": "saksbehandlerKlagebehandling","avbruttTidspunkt": "TIMESTAMP","status": "KLAGE_TRUKKET","begrunnelse": null}""",
            )
        }
    }

    @Test
    fun `kan ikke avbryte med status ANNET uten begrunnelse`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagebehandling, _) = opprettSakOgKlagebehandlingTilAvvisning(tac = tac)!!
            avbrytKlagebehandlingForSak(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                avbruttStatus = AvbruttKlagebehandlingStatus.ANNET,
                begrunnelse = null,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                        "melding": "Begrunnelse må være satt når status er ANNET",
                        "kode": "begrunnelse_må_være_satt_for_status"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke avbryte med begrunnelse satt når status ikke er ANNET`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagebehandling, _) = opprettSakOgKlagebehandlingTilAvvisning(tac = tac)!!
            avbrytKlagebehandlingForSak(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                avbruttStatus = AvbruttKlagebehandlingStatus.KLAGE_TRUKKET,
                begrunnelse = "ugyldig begrunnelse",
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                        "melding": "Begrunnelse må være null når status ikke er ANNET",
                        "kode": "ugyldig_begrunnelse_for_status"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }
}
