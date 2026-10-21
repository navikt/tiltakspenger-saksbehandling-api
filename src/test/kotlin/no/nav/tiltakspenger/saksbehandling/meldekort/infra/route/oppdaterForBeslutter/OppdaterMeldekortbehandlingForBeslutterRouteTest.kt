package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.oppdaterForBeslutter

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.journalføreNotat.MeldekortbehandlingSkalJournalføreNotatEndring
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterMeldekortbehandlingForBeslutter
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.underkjennMeldekortbehandling
import org.json.JSONObject
import org.junit.jupiter.api.Test

/**
 * Beslutter kan endre saksbehandlers valg om begrunnelsen skal journalføres som notat.
 * Saksbehandlers opprinnelige valg tas vare på, slik at det alltid går an å se at beslutter har endret valget.
 */
class OppdaterMeldekortbehandlingForBeslutterRouteTest {

    private val saksbehandler = ObjectMother.saksbehandler()
    private val beslutter = ObjectMother.beslutter("beslutter")
    private val begrunnelse = "Begrunnelse for meldekortbehandlingen."

    @Test
    fun `beslutter endrer valget, og saksbehandlers valg tas vare på gjennom iverksettelse`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, meldekortId) = meldekortbehandlingUnderBeslutning(tac, begrunnelse = begrunnelse, skalJournalføreNotat = true)

            val json = oppdaterMeldekortbehandlingForBeslutter(tac, sakId, meldekortId, false, beslutter)!!
            val behandlingJson = JSONObject(json).getJSONObject("meldekortbehandlinger").getJSONObject(meldekortId.toString())
            behandlingJson.getBoolean("skalJournalføreNotat") shouldBe false
            val behandling = tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!
            behandling.skalJournalføreNotat shouldBe false
            val endring = behandling.beslutterEndringer.skalJournalføreNotat
            endring shouldBe MeldekortbehandlingSkalJournalføreNotatEndring(
                tidspunkt = behandling.sistEndret,
                saksbehandler = true,
                beslutter = false,
            )
            val forventetJson = """
                {
                  "tidspunkt": "${behandling.sistEndret}",
                  "saksbehandler": true,
                  "beslutter": false
                }
            """.trimIndent()
            behandlingJson.getJSONObject("beslutterEndringer").getJSONObject("skalJournalføreNotat").toString().shouldEqualJson(forventetJson)
            hentBeslutterEndringerFraDb(tac, meldekortId).shouldEqualJson("""{"skalJournalføreNotat": $forventetJson}""")

            val (_, vedtak) = iverksettMeldekortbehandling(tac, sakId, meldekortId, beslutter = beslutter)!!
            vedtak.skalJournalføreNotat shouldBe false
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.beslutterEndringer.skalJournalføreNotat shouldBe endring
        }
    }

    @Test
    fun `beslutter som endrer tilbake til saksbehandlers valg, nullstiller endringen`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, meldekortId) = meldekortbehandlingUnderBeslutning(tac, begrunnelse = begrunnelse, skalJournalføreNotat = true)

            oppdaterMeldekortbehandlingForBeslutter(tac, sakId, meldekortId, false, beslutter)
            oppdaterMeldekortbehandlingForBeslutter(tac, sakId, meldekortId, true, beslutter)

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.also {
                it.skalJournalføreNotat shouldBe true
                it.beslutterEndringer.skalJournalføreNotat shouldBe null
            }
        }
    }

    @Test
    fun `beslutters endring beholdes etter underkjenning og nullstilles når saksbehandler sender til beslutning igjen`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, meldekortId) = meldekortbehandlingUnderBeslutning(tac, begrunnelse = begrunnelse, skalJournalføreNotat = true)
            oppdaterMeldekortbehandlingForBeslutter(tac, sakId, meldekortId, false, beslutter)
            val endring = tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.beslutterEndringer.skalJournalføreNotat
            endring!!.beslutter shouldBe false
            underkjennMeldekortbehandling(tac, sakId, meldekortId, beslutter = beslutter)

            oppdaterMeldekortbehandling(
                tac = tac,
                sakId = sakId,
                meldekortId = meldekortId,
                saksbehandler = saksbehandler,
                begrunnelse = begrunnelse,
                skalJournalføreNotat = true,
            )
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.beslutterEndringer.skalJournalføreNotat shouldBe endring

            sendMeldekortbehandlingTilBeslutning(tac, sakId, meldekortId, saksbehandler)
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.also {
                it.skalJournalføreNotat shouldBe true
                it.beslutterEndringer.skalJournalføreNotat shouldBe null
            }
            hentBeslutterEndringerFraDb(tac, meldekortId).shouldEqualJson("""{"skalJournalføreNotat": null}""")
        }
    }

    @Test
    fun `beslutter kan ikke velge å journalføre en begrunnelse som mangler`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, meldekortId) = meldekortbehandlingUnderBeslutning(tac, begrunnelse = null, skalJournalføreNotat = false)

            oppdaterMeldekortbehandlingForBeslutter(
                tac = tac,
                sakId = sakId,
                meldekortId = meldekortId,
                skalJournalføreNotat = true,
                beslutter = beslutter,
                forventet = ForventetRespons(
                    status = 400,
                    contentType = "application/json; charset=UTF-8",
                    body = ForventetBody.Json(serialize(Standardfeil.måHaBegrunnelseForÅJournalføreNotat())),
                ),
            )
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.beslutterEndringer.skalJournalføreNotat shouldBe null
        }
    }

    @Test
    fun `en annen beslutter enn den som er tildelt kan ikke endre valget`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, meldekortId) = meldekortbehandlingUnderBeslutning(tac, begrunnelse = begrunnelse, skalJournalføreNotat = true)

            oppdaterMeldekortbehandlingForBeslutter(
                tac = tac,
                sakId = sakId,
                meldekortId = meldekortId,
                skalJournalføreNotat = false,
                beslutter = ObjectMother.beslutter("annenBeslutter"),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.skalJournalføreNotat shouldBe true
        }
    }

    private suspend fun ApplicationTestBuilder.meldekortbehandlingUnderBeslutning(
        tac: TestApplicationContext,
        begrunnelse: String?,
        skalJournalføreNotat: Boolean,
    ): Pair<SakId, MeldekortId> {
        val (sak, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            begrunnelse = begrunnelse,
            skalJournalføreNotat = skalJournalføreNotat,
        )!!
        sendMeldekortbehandlingTilBeslutning(tac, sak.id, meldekortbehandling.id, saksbehandler)
        taMeldekortbehanding(tac, sak.id, meldekortbehandling.id, saksbehandlerEllerBeslutter = beslutter)
        return sak.id to meldekortbehandling.id
    }

    private fun hentBeslutterEndringerFraDb(tac: TestApplicationContextMedPostgres, meldekortId: MeldekortId): String =
        tac.sessionFactory.withSession { session ->
            session.run(
                queryOf("select beslutter_endringer from meldekortbehandling where id = :id", mapOf("id" to meldekortId.toString()))
                    .map { it.string("beslutter_endringer") }
                    .asSingle,
            )!!
        }
}
