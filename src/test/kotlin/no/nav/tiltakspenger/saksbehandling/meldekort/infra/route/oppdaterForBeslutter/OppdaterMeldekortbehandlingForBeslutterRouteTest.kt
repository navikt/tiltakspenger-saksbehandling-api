package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.oppdaterForBeslutter

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.distribuereVedtaksbrev.MeldekortbehandlingSkalDistribuereVedtaksbrevEndring
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.journalføreNotat.MeldekortbehandlingSkalJournalføreNotatEndring
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterMeldekortbehandlingForBeslutter
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgOppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.underkjennMeldekortbehandling
import org.json.JSONObject
import org.junit.jupiter.api.Test

/**
 * Beslutter kan endre saksbehandlers valg om begrunnelsen skal journalføres som notat, og om vedtaksbrevet skal sendes til bruker.
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
            hentBeslutterEndringerFraDb(tac, meldekortId).shouldEqualJson("""{"skalJournalføreNotat": $forventetJson, "skalDistribuereVedtaksbrev": null}""")

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
            hentBeslutterEndringerFraDb(tac, meldekortId).shouldEqualJson("""{"skalJournalføreNotat": null, "skalDistribuereVedtaksbrev": null}""")
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

    @Test
    fun `beslutter velger å sende vedtaksbrevet til bruker, og saksbehandlers valg tas vare på gjennom iverksettelse`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, meldekortId) = korrigeringUnderBeslutning(tac, skalDistribuereVedtaksbrev = false)

            val json = oppdaterMeldekortbehandlingForBeslutter(
                tac = tac,
                sakId = sakId,
                meldekortId = meldekortId,
                skalJournalføreNotat = false,
                skalDistribuereVedtaksbrev = true,
                beslutter = beslutter,
            )!!
            val behandlingJson = JSONObject(json).getJSONObject("meldekortbehandlinger").getJSONObject(meldekortId.toString())
            behandlingJson.getBoolean("skalDistribuereVedtaksbrev") shouldBe true
            val behandling = tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!
            behandling.skalDistribuereVedtaksbrev shouldBe true
            val endring = behandling.beslutterEndringer.skalDistribuereVedtaksbrev
            endring shouldBe MeldekortbehandlingSkalDistribuereVedtaksbrevEndring(
                tidspunkt = behandling.sistEndret,
                saksbehandler = false,
                beslutter = true,
            )
            val forventetJson = """
                {
                  "tidspunkt": "${behandling.sistEndret}",
                  "saksbehandler": false,
                  "beslutter": true
                }
            """.trimIndent()
            behandlingJson.getJSONObject("beslutterEndringer").getJSONObject("skalDistribuereVedtaksbrev").toString().shouldEqualJson(forventetJson)
            hentBeslutterEndringerFraDb(tac, meldekortId).shouldEqualJson("""{"skalJournalføreNotat": null, "skalDistribuereVedtaksbrev": $forventetJson}""")

            val (_, vedtak) = iverksettMeldekortbehandling(tac, sakId, meldekortId, beslutter = beslutter)!!
            vedtak.skalDistribuereVedtaksbrev shouldBe true
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.beslutterEndringer.skalDistribuereVedtaksbrev shouldBe endring
        }
    }

    @Test
    fun `beslutter velger bort å sende vedtaksbrevet til bruker, og endrer tilbake til saksbehandlers valg`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, meldekortId) = korrigeringUnderBeslutning(tac, skalDistribuereVedtaksbrev = true)

            oppdaterMeldekortbehandlingForBeslutter(tac, sakId, meldekortId, false, skalDistribuereVedtaksbrev = false, beslutter = beslutter)
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.also {
                it.skalDistribuereVedtaksbrev shouldBe false
                it.beslutterEndringer.skalDistribuereVedtaksbrev!!.saksbehandler shouldBe true
                it.beslutterEndringer.skalDistribuereVedtaksbrev!!.beslutter shouldBe false
            }

            oppdaterMeldekortbehandlingForBeslutter(tac, sakId, meldekortId, false, skalDistribuereVedtaksbrev = true, beslutter = beslutter)
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.also {
                it.skalDistribuereVedtaksbrev shouldBe true
                it.beslutterEndringer.skalDistribuereVedtaksbrev shouldBe null
            }
        }
    }

    @Test
    fun `et valg beslutter ikke endrer, beholder både verdien og en tidligere endring`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, meldekortId) = korrigeringUnderBeslutning(tac, skalDistribuereVedtaksbrev = true)
            oppdaterMeldekortbehandlingForBeslutter(tac, sakId, meldekortId, false, skalDistribuereVedtaksbrev = false, beslutter = beslutter)
            val distribusjonsendring = tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.beslutterEndringer.skalDistribuereVedtaksbrev!!

            oppdaterMeldekortbehandlingForBeslutter(tac, sakId, meldekortId, true, skalDistribuereVedtaksbrev = false, beslutter = beslutter)
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.also {
                it.skalJournalføreNotat shouldBe true
                it.beslutterEndringer.skalJournalføreNotat.shouldNotBeNull()
                it.beslutterEndringer.skalDistribuereVedtaksbrev shouldBe distribusjonsendring
            }

            // Frontend som ikke sender distribusjonsvalget, skal ikke endre det.
            oppdaterMeldekortbehandlingForBeslutter(tac, sakId, meldekortId, true, skalDistribuereVedtaksbrev = null, beslutter = beslutter)
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.also {
                it.skalDistribuereVedtaksbrev shouldBe false
                it.beslutterEndringer.skalDistribuereVedtaksbrev shouldBe distribusjonsendring
            }
        }
    }

    @Test
    fun `beslutter velger å journalføre og sende vedtaksbrevet når saksbehandler valgte bort begge, også uten korrigering`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, meldekortId) = meldekortbehandlingUnderBeslutning(
                tac,
                begrunnelse = begrunnelse,
                skalJournalføreNotat = false,
                skalJournalføreVedtaksbrev = false,
            )

            oppdaterMeldekortbehandlingForBeslutter(
                tac = tac,
                sakId = sakId,
                meldekortId = meldekortId,
                skalJournalføreNotat = false,
                skalJournalføreVedtaksbrev = true,
                skalDistribuereVedtaksbrev = true,
                beslutter = beslutter,
            )
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.also {
                it.harKorrigering shouldBe false
                it.skalJournalføreVedtaksbrev shouldBe true
                it.skalDistribuereVedtaksbrev shouldBe true
                it.beslutterEndringer.skalDistribuereVedtaksbrev!!.saksbehandler shouldBe false
                it.beslutterEndringer.skalDistribuereVedtaksbrev!!.beslutter shouldBe true
            }

            oppdaterMeldekortbehandlingForBeslutter(
                tac = tac,
                sakId = sakId,
                meldekortId = meldekortId,
                skalJournalføreNotat = false,
                skalJournalføreVedtaksbrev = false,
                skalDistribuereVedtaksbrev = false,
                beslutter = beslutter,
            )
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.also {
                it.skalJournalføreVedtaksbrev shouldBe false
                it.skalDistribuereVedtaksbrev shouldBe false
                it.beslutterEndringer.skalDistribuereVedtaksbrev shouldBe null
            }
        }
    }

    @Test
    fun `beslutter kan ikke sende vedtaksbrevet til bruker når det ikke journalføres`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, meldekortId) = korrigeringUnderBeslutning(tac, skalDistribuereVedtaksbrev = false, skalJournalføreVedtaksbrev = false)

            oppdaterMeldekortbehandlingForBeslutter(
                tac = tac,
                sakId = sakId,
                meldekortId = meldekortId,
                skalJournalføreNotat = false,
                skalDistribuereVedtaksbrev = true,
                beslutter = beslutter,
                forventet = ForventetRespons(
                    status = 400,
                    contentType = "application/json; charset=UTF-8",
                    body = ForventetBody.Json(
                        serialize(
                            ErrorJson(
                                "Vedtaksbrevet kan ikke sendes til bruker uten at det journalføres.",
                                "kan_ikke_distribuere_vedtaksbrev_uten_journalføring",
                            ),
                        ),
                    ),
                ),
            )
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId)!!.skalDistribuereVedtaksbrev shouldBe false
        }
    }

    /** Bygger en korrigering av en allerede behandlet meldeperiode, og sender den til beslutning. */
    private suspend fun ApplicationTestBuilder.korrigeringUnderBeslutning(
        tac: TestApplicationContext,
        skalDistribuereVedtaksbrev: Boolean,
        skalJournalføreVedtaksbrev: Boolean = true,
    ): Pair<SakId, MeldekortId> {
        val (sak) = iverksettSøknadsbehandling(tac = tac)
        val kjedeId = sak.meldeperiodeKjeder.first().kjedeId
        opprettOgIverksettMeldekortbehandling(tac = tac, sakId = sak.id, kjedeId = kjedeId)!!
        val (_, korrigering) = opprettOgOppdaterMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            kjedeId = kjedeId,
            saksbehandler = saksbehandler,
            skalJournalføreVedtaksbrev = skalJournalføreVedtaksbrev,
            skalDistribuereVedtaksbrev = skalDistribuereVedtaksbrev,
            begrunnelse = begrunnelse,
        )!!
        sendMeldekortbehandlingTilBeslutning(tac, sak.id, korrigering.id, saksbehandler)
        taMeldekortbehanding(tac, sak.id, korrigering.id, saksbehandlerEllerBeslutter = beslutter)
        return sak.id to korrigering.id
    }

    private suspend fun ApplicationTestBuilder.meldekortbehandlingUnderBeslutning(
        tac: TestApplicationContext,
        begrunnelse: String?,
        skalJournalføreNotat: Boolean,
        skalJournalføreVedtaksbrev: Boolean = true,
    ): Pair<SakId, MeldekortId> {
        val (sak, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            begrunnelse = begrunnelse,
            skalJournalføreNotat = skalJournalføreNotat,
            skalJournalføreVedtaksbrev = skalJournalføreVedtaksbrev,
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
