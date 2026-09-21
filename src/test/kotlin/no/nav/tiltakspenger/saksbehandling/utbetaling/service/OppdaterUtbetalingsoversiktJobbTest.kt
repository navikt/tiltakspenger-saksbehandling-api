package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.testing.ApplicationTestBuilder
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.KunneIkkeHenteUtbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsfeiltype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsperiodetype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsplan
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktMappingfeil
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.registrertUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt.UtbetalingsoversiktFakeKlient
import no.nav.tiltakspenger.saksbehandling.vedtak.periodeForUtbetalingsoversikt
import org.junit.jupiter.api.Test

/** Hver test bygger sin egen sak gjennom rutene og kaller jobben for den saken, så testene kan gå parallelt i delt skjema. */
class OppdaterUtbetalingsoversiktJobbTest {
    @Test
    fun `vellykket oppslag lagrer utbetalingene, planen og rå request og response`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedOkUtbetaling(tac)
            val utbetalinger = listOf(registrertUtbetaling())
            tac.utbetalingsoversiktFakeKlient.leggTilUtbetalinger(sak.fnr, utbetalinger)

            jobb(tac).oppdaterForSak(sak.id).isRight() shouldBe true

            val oversikt = status(tac, sak).shouldBeInstanceOf<Utbetalingsoversiktstatus.SisteOppslagVellykket>().oversikt
            oversikt.utbetalinger shouldBe utbetalinger
            oversikt.oppslag shouldBe Oppslag(sak.vedtaksliste.periodeForUtbetalingsoversikt()!!, Oppslagsperiodetype.YTELSESPERIODE)
            tac.utbetalingsoversiktFakeKlient.sisteOppslagFor(sak.fnr) shouldBe oversikt.oppslag
            oversikt.plan shouldBe Oppslagsplan.etterVellykketOppslag(oversikt.hentet, harNyligSendtUtbetaling = true)
            hentMetadata(tac, oversikt.id).shouldNotBeNull().let { (metadata, correlationId) ->
                metadata shouldEqualJson """
                    {
                      "request": "${UtbetalingsoversiktFakeKlient.RÅ_REQUEST}",
                      "response": "${UtbetalingsoversiktFakeKlient.RÅ_RESPONSE}",
                      "statusKode": 200,
                      "correlationId": "$correlationId",
                      "requestSendt": null,
                      "responsMottatt": null,
                      "varighetMs": 0,
                      "antallForsøk": 1
                    }
                """.trimIndent()
            }
            antallOppslag(tac, "vellykket") shouldBe 1.0
        }
    }

    @Test
    fun `sak uten nylig sendt utbetaling slås opp igjen om 30 dager`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedOkUtbetaling(tac)
            tac.clock.spolTil(sak.utbetalinger.single().sendtTilUtbetaling!!.toLocalDate().plusDays(31))

            jobb(tac).oppdaterForSak(sak.id)

            val oversikt = status(tac, sak).shouldBeInstanceOf<Utbetalingsoversiktstatus.SisteOppslagVellykket>().oversikt
            oversikt.plan shouldBe Oppslagsplan.etterVellykketOppslag(oversikt.hentet, harNyligSendtUtbetaling = false)
        }
    }

    @Test
    fun `feilet oppslag lagres med feiltype, og ventetiden øker for hver feil på rad`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedOkUtbetaling(tac)
            val feil = KunneIkkeHenteUtbetalingsoversikt.Tjenestefeil(ObjectMother.httpKlientUventetStatus(500))
            tac.utbetalingsoversiktFakeKlient.leggTilFeil(sak.fnr, feil)

            jobb(tac).oppdaterForSak(sak.id).leftOrNull() shouldBe feil
            val første = status(tac, sak).shouldBeInstanceOf<Utbetalingsoversiktstatus.SisteOppslagFeilet>()
            første.sisteVellykkede shouldBe null
            første.oversikt.feiltype shouldBe Oppslagsfeiltype.TJENESTEFEIL
            første.oversikt.plan shouldBe Oppslagsplan.etterFeiletOppslag(første.oversikt.hentet, tidligereFeilPåRad = 0, clock = tac.clock)

            jobb(tac).oppdaterForSak(sak.id)
            val andre = status(tac, sak).shouldBeInstanceOf<Utbetalingsoversiktstatus.SisteOppslagFeilet>()
            andre.oversikt.plan shouldBe Oppslagsplan.etterFeiletOppslag(andre.oversikt.hentet, tidligereFeilPåRad = 1, clock = tac.clock)
            antallOppslag(tac, "tjenestefeil") shouldBe 2.0
        }
    }

    @Test
    fun `feil etter et vellykket oppslag beholder siste vellykkede, og neste vellykkede nullstiller telleren`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedOkUtbetaling(tac)

            jobb(tac).oppdaterForSak(sak.id)
            val vellykket = status(tac, sak).shouldBeInstanceOf<Utbetalingsoversiktstatus.SisteOppslagVellykket>().oversikt

            tac.utbetalingsoversiktFakeKlient.leggTilFeil(
                sak.fnr,
                KunneIkkeHenteUtbetalingsoversikt.UgyldigInnhold(
                    feil = UtbetalingsoversiktMappingfeil.PåkrevdFeltMangler("posteringsdato"),
                    metadata = ObjectMother.httpKlientResponse(body = Unit).metadata,
                ),
            )
            jobb(tac).oppdaterForSak(sak.id)
            val feilet = status(tac, sak).shouldBeInstanceOf<Utbetalingsoversiktstatus.SisteOppslagFeilet>()
            feilet.oversikt.feiltype shouldBe Oppslagsfeiltype.UGYLDIG_INNHOLD
            feilet.sisteVellykkede shouldBe vellykket

            tac.utbetalingsoversiktFakeKlient.leggTilUtbetalinger(sak.fnr, emptyList())
            jobb(tac).oppdaterForSak(sak.id)
            status(tac, sak).shouldBeInstanceOf<Utbetalingsoversiktstatus.SisteOppslagVellykket>().oversikt.plan.antallFeilPåRad shouldBe 0
        }
    }

    @Test
    fun `oppslag med samme tidspunkt ordnes etter id`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedOkUtbetaling(tac)
            val tidspunkt = nå(tac.clock)
            tac.utbetalingsoversiktFakeKlient.responsMottatt = tidspunkt

            jobb(tac).oppdaterForSak(sak.id)
            val vellykket = status(tac, sak).shouldBeInstanceOf<Utbetalingsoversiktstatus.SisteOppslagVellykket>().oversikt

            val metadata = ObjectMother.httpKlientResponse(body = Unit).metadata
            tac.utbetalingsoversiktFakeKlient.leggTilFeil(
                sak.fnr,
                KunneIkkeHenteUtbetalingsoversikt.UgyldigInnhold(
                    feil = UtbetalingsoversiktMappingfeil.PåkrevdFeltMangler("posteringsdato"),
                    metadata = metadata.copy(tidsstempler = metadata.tidsstempler.copy(responsMottatt = tidspunkt)),
                ),
            )
            jobb(tac).oppdaterForSak(sak.id)

            val feilet = status(tac, sak).shouldBeInstanceOf<Utbetalingsoversiktstatus.SisteOppslagFeilet>()
            feilet.oversikt.hentet shouldBe vellykket.hentet
            feilet.sisteVellykkede shouldBe vellykket
        }
    }

    @Test
    fun `sak uten oppslag har status IkkeHentet`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandling(tac, jobber = JobberEtterIverksettelse.ingen)

            status(tac, sak) shouldBe Utbetalingsoversiktstatus.IkkeHentet
        }
    }

    private fun jobb(tac: TestApplicationContext) = tac.utbetalingContext.oppdaterUtbetalingsoversiktJobb

    private fun status(tac: TestApplicationContext, sak: Sak) = tac.utbetalingContext.utbetalingsoversiktRepo.hentStatusForSak(sak.id)

    private fun antallOppslag(tac: TestApplicationContext, resultat: String): Double =
        tac.meterRegistry.counter("tpts_saksbehandlingapi_utbetalingsoversikt_oppslag", "resultat", resultat).count()

    private suspend fun ApplicationTestBuilder.sakMedOkUtbetaling(tac: TestApplicationContext): Sak {
        val sak = iverksettSøknadsbehandling(tac, jobber = JobberEtterIverksettelse.ingen).first
        opprettOgIverksettMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            jobber = JobberEtterIverksettelse(journalførVedtaksbrev = false, distribuerVedtaksbrev = false),
        )
        return tac.sakContext.sakRepo.hentForSakId(sak.id)!!.also {
            it.utbetalinger.single().status shouldBe Utbetalingsstatus.Ok
        }
    }

    /** Metadata leses aldri ut i domenet, så testen leser kolonnen med egen SQL. */
    private fun hentMetadata(tac: TestApplicationContext, id: UtbetalingsoversiktId): Pair<String, String>? =
        (tac.sessionFactory as PostgresSessionFactory).withSession {
            it.run(
                queryOf(
                    "SELECT metadata::text AS metadata, metadata ->> 'correlationId' AS correlation_id FROM utbetalingsoversikt WHERE id = :id",
                    mapOf("id" to id.toString()),
                ).map { row -> row.string("metadata") to row.string("correlation_id") }.asSingle,
            )
        }
}
