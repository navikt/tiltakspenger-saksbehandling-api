package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route

import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.konsumerTilbakekrevingshendelse
import org.intellij.lang.annotations.Language
import java.time.LocalDate

/**
 * Hjelpefunksjoner for å sette opp en [TilbakekrevingBehandling] og kalle tildelingsrutene i tester.
 *
 * Routes:
 * - [no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route.tildelTilbakekrevingBehandlingRoute]
 * - [no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route.overtaTilbakekrevingBehandlingRoute]
 * - [no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route.leggTilbakeTilbakekrevingBehandlingRoute]
 */
interface TilbakekrevingBehandlingBuilder {

    /**
     * 1. Iverksetter en søknadsbehandling og meldekortbehandling (gir oss utbetalinger).
     * 2. Sender to behandling_endret Kafka-hendelser (OPPRETTET → TIL_BEHANDLING).
     * 3. Kjører hendelsejobben to ganger slik at behandlingen opprettes og oppdateres.
     *
     * @return Sak med tilbakekrevingbehandling i status TIL_BEHANDLING, og selve tilbakekrevingbehandlingen.
     */
    suspend fun ApplicationTestBuilder.opprettTilbakekrevingBehandlingTilBehandling(
        tac: TestApplicationContext,
        vedtaksperiode: Periode = 1.til(10.april(2025)),
    ): Pair<Sak, TilbakekrevingBehandling> {
        val (sak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac, vedtaksperiode = vedtaksperiode)!!

        val utbetaling = sak.utbetalinger.first()
        val eksternBehandlingId = utbetaling.id.uuidPart()
        val tilbakeBehandlingId = "tilbake-behandling-${SakId.random()}"

        @Language("JSON")
        val opprettetHendelseJson = """
            {
                "hendelsestype": "behandling_endret",
                "versjon": 1,
                "eksternFagsakId": "${sak.saksnummer.verdi}",
                "hendelseOpprettet": "${nå(tac.clock)}",
                "eksternBehandlingId": "$eksternBehandlingId",
                "tilbakekreving": {
                    "behandlingId": "$tilbakeBehandlingId",
                    "sakOpprettet": "${nå(tac.clock)}",
                    "varselSendt": null,
                    "behandlingsstatus": "OPPRETTET",
                    "forrigeBehandlingsstatus": null,
                    "totaltFeilutbetaltBeløp": 1000.00,
                    "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/$tilbakeBehandlingId",
                    "fullstendigPeriode": {
                        "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                        "tom": "${LocalDate.now(tac.clock)}"
                    }
                }
            }
        """.trimIndent()

        val opprettetHendelseJsonId = konsumerTilbakekrevingshendelse(
            key = sak.fnr.verdi,
            value = opprettetHendelseJson,
            tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
            clock = tac.clock,
        )!!
        // Jobbens kø-spørring går på tvers av saker, så vi kjører kun vår egen hendelse.
        tac.behandleTilbakekrevingHendelserJobb.håndterHendelse(opprettetHendelseJsonId)

        @Language("JSON")
        val tilBehandlingHendelseJson = """
            {
                "hendelsestype": "behandling_endret",
                "versjon": 1,
                "eksternFagsakId": "${sak.saksnummer.verdi}",
                "hendelseOpprettet": "${nå(tac.clock).plusSeconds(10)}",
                "eksternBehandlingId": "$eksternBehandlingId",
                "tilbakekreving": {
                    "behandlingId": "$tilbakeBehandlingId",
                    "sakOpprettet": "${nå(tac.clock)}",
                    "varselSendt": "${LocalDate.now(tac.clock)}",
                    "behandlingsstatus": "TIL_BEHANDLING",
                    "forrigeBehandlingsstatus": "OPPRETTET",
                    "totaltFeilutbetaltBeløp": 1000.00,
                    "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/$tilbakeBehandlingId",
                    "fullstendigPeriode": {
                        "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                        "tom": "${LocalDate.now(tac.clock)}"
                    }
                }
            }
        """.trimIndent()

        val tilBehandlingHendelseJsonId = konsumerTilbakekrevingshendelse(
            key = sak.fnr.verdi,
            value = tilBehandlingHendelseJson,
            tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
            clock = tac.clock,
        )!!
        // Jobbens kø-spørring går på tvers av saker, så vi kjører kun vår egen hendelse.
        tac.behandleTilbakekrevingHendelserJobb.håndterHendelse(tilBehandlingHendelseJsonId)

        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
        val tilbakekrevingBehandling = oppdatertSak.tilbakekrevinger.single()
        tilbakekrevingBehandling.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING

        return oppdatertSak to tilbakekrevingBehandling
    }

    /**
     * Som [opprettTilbakekrevingBehandlingTilBehandling], men sender i tillegg en hendelse som flytter behandlingen til TIL_GODKJENNING.
     * Det er statusen som gjør at tildelingsrutene tar beslutter-veien i stedet for saksbehandler-veien.
     */
    suspend fun ApplicationTestBuilder.opprettTilbakekrevingBehandlingTilGodkjenning(
        tac: TestApplicationContext,
        vedtaksperiode: Periode = 1.til(10.april(2025)),
    ): Pair<Sak, TilbakekrevingBehandling> {
        val (sak, behandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac, vedtaksperiode = vedtaksperiode)

        @Language("JSON")
        val tilGodkjenningHendelseJson = """
            {
                "hendelsestype": "behandling_endret",
                "versjon": 1,
                "eksternFagsakId": "${sak.saksnummer.verdi}",
                "hendelseOpprettet": "${nå(tac.clock).plusSeconds(20)}",
                "eksternBehandlingId": "${sak.utbetalinger.first().id.uuidPart()}",
                "tilbakekreving": {
                    "behandlingId": "${behandling.tilbakeBehandlingId}",
                    "sakOpprettet": "${nå(tac.clock)}",
                    "varselSendt": "${LocalDate.now(tac.clock)}",
                    "behandlingsstatus": "TIL_GODKJENNING",
                    "forrigeBehandlingsstatus": "TIL_BEHANDLING",
                    "totaltFeilutbetaltBeløp": 1000.00,
                    "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/${behandling.tilbakeBehandlingId}",
                    "fullstendigPeriode": {
                        "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                        "tom": "${LocalDate.now(tac.clock)}"
                    }
                }
            }
        """.trimIndent()

        val tilGodkjenningHendelseJsonId = konsumerTilbakekrevingshendelse(
            key = sak.fnr.verdi,
            value = tilGodkjenningHendelseJson,
            tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
            clock = tac.clock,
        )!!
        // Jobbens kø-spørring går på tvers av saker, så vi kjører kun vår egen hendelse.
        tac.behandleTilbakekrevingHendelserJobb.håndterHendelse(tilGodkjenningHendelseJsonId)

        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
        val tilbakekrevingBehandling = oppdatertSak.tilbakekrevinger.single()
        tilbakekrevingBehandling.status shouldBe TilbakekrevingBehandlingsstatus.TIL_GODKJENNING

        return oppdatertSak to tilbakekrevingBehandling
    }

    /**
     * Forventer at det allerede finnes en sak og en tilbakekrevingbehandling.
     * Kaller POST /sak/{sakId}/tilbakekreving/{tilbakekrevingId}/tildel
     */
    suspend fun ApplicationTestBuilder.tildelTilbakekrevingBehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomTar"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, TilbakekrevingBehandling, SakDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        val uri = "/sak/$sakId/tilbakekreving/$tilbakekrevingId/tildel"
        val response = defaultRequestWithAssertions(
            HttpMethod.POST,
            uri,
            jwt = jwt,
            forventet = forventet,
        )
        if (response.statusCode != 200) return null
        val sakDTOJson: SakDTOJson = objectMapper.readTree(response.body)
        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        val oppdatertBehandling = oppdatertSak.tilbakekrevinger.single { it.id == tilbakekrevingId }
        return Triple(oppdatertSak, oppdatertBehandling, sakDTOJson)
    }

    /**
     * Forventer at det allerede finnes en sak og en tilbakekrevingbehandling som er tatt (UNDER_BEHANDLING / UNDER_GODKJENNING).
     * Kaller PATCH /sak/{sakId}/tilbakekreving/{tilbakekrevingId}/overta
     */
    suspend fun ApplicationTestBuilder.overtaTilbakekrevingBehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOvertar"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, TilbakekrevingBehandling, SakDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        val uri = "/sak/$sakId/tilbakekreving/$tilbakekrevingId/overta"
        val response = defaultRequestWithAssertions(
            HttpMethod.POST,
            uri,
            jwt = jwt,
            forventet = forventet,
        )
        if (response.statusCode != 200) return null
        val sakDTOJson: SakDTOJson = objectMapper.readTree(response.body)
        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        val oppdatertBehandling = oppdatertSak.tilbakekrevinger.single { it.id == tilbakekrevingId }
        return Triple(oppdatertSak, oppdatertBehandling, sakDTOJson)
    }

    /**
     * Forventer at det allerede finnes en sak og en tilbakekrevingbehandling som er tatt (UNDER_BEHANDLING / UNDER_GODKJENNING).
     * Kaller POST /sak/{sakId}/tilbakekreving/{tilbakekrevingId}/legg-tilbake
     */
    suspend fun ApplicationTestBuilder.leggTilbakeTilbakekrevingBehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomLeggerTilbake"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, TilbakekrevingBehandling, SakDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        val uri = "/sak/$sakId/tilbakekreving/$tilbakekrevingId/legg-tilbake"
        val response = defaultRequestWithAssertions(
            HttpMethod.POST,
            uri,
            jwt = jwt,
            forventet = forventet,
        )
        if (response.statusCode != 200) return null
        val sakDTOJson: SakDTOJson = objectMapper.readTree(response.body)
        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        val oppdatertBehandling = oppdatertSak.tilbakekrevinger.single { it.id == tilbakekrevingId }
        return Triple(oppdatertSak, oppdatertBehandling, sakDTOJson)
    }
}
