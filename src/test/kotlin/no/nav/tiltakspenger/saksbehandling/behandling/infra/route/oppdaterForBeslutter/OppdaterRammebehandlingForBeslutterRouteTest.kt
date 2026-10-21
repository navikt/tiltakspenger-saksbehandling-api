package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdaterForBeslutter

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SkalJournalføreNotat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.journalføreNotat.BarnetilleggSkalJournalføreNotatEndring
import no.nav.tiltakspenger.saksbehandling.behandling.domene.journalføreNotat.RammebehandlingSkalJournalføreNotatEndring
import no.nav.tiltakspenger.saksbehandling.behandling.domene.journalføreNotat.VilkårsvurderingSkalJournalføreNotatEndring
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.createOrThrow
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRammebehandlingForBeslutter
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.underkjennForBehandlingId
import org.json.JSONObject
import org.junit.jupiter.api.Test

/**
 * Beslutter kan endre saksbehandlers valg om hvilke begrunnelser som skal journalføres som notat.
 * Saksbehandlers opprinnelige valg tas vare på, slik at det alltid går an å se at beslutter har endret valget.
 */
class OppdaterRammebehandlingForBeslutterRouteTest {

    private val saksbehandler = ObjectMother.saksbehandler()
    private val beslutter = ObjectMother.beslutter()
    private val begrunnelse = "Begrunnelse for vilkårsvurderingen."
    private val saksbehandlersValg = SkalJournalføreNotat(vilkårsvurdering = true, barnetillegg = false)

    @Test
    fun `beslutter endrer valget, og saksbehandlers valg tas vare på gjennom iverksettelse`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, behandlingId) = behandlingUnderBeslutning(tac)

            val json = oppdaterRammebehandlingForBeslutter(tac, sakId, behandlingId, SkalJournalføreNotat.INGEN, beslutter)!!
            JSONObject(json).getJSONObject("skalJournalføreNotat").toString().shouldEqualJson(
                """{"vilkårsvurdering": false, "barnetillegg": false}""",
            )
            val behandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
            behandling.skalJournalføreNotat shouldBe SkalJournalføreNotat.INGEN
            val endring = behandling.beslutterEndringer.skalJournalføreNotat
            endring shouldBe RammebehandlingSkalJournalføreNotatEndring(
                tidspunkt = behandling.sistEndret,
                vilkårsvurdering = VilkårsvurderingSkalJournalføreNotatEndring(saksbehandler = true, beslutter = false),
                barnetillegg = null,
            )
            JSONObject(json).getJSONObject("beslutterEndringer").getJSONObject("skalJournalføreNotat").toString().shouldEqualJson(
                """
                {
                  "tidspunkt": "${behandling.sistEndret}",
                  "vilkårsvurdering": {"saksbehandler": true, "beslutter": false},
                  "barnetillegg": null
                }
                """.trimIndent(),
            )
            hentBeslutterEndringerFraDb(tac, behandlingId).shouldEqualJson(
                """
                {
                  "skalJournalføreNotat": {
                    "tidspunkt": "${behandling.sistEndret}",
                    "vilkårsvurdering": {"saksbehandler": true, "beslutter": false},
                    "barnetillegg": null
                  }
                }
                """.trimIndent(),
            )

            val (_, vedtak) = iverksettForBehandlingId(
                tac = tac,
                sakId = sakId,
                behandlingId = behandlingId,
                beslutter = beslutter,
                jobber = JobberEtterIverksettelse(),
            )!!
            vedtak.skalJournalføreNotat shouldBe SkalJournalføreNotat.INGEN
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).beslutterEndringer.skalJournalføreNotat shouldBe endring
        }
    }

    @Test
    fun `beslutter kan endre valget for barnetillegg, og endringen for hver begrunnelse holdes adskilt`() {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandlersValgMedBarnetillegg = SkalJournalføreNotat(vilkårsvurdering = true, barnetillegg = true)
            val (sakId, behandlingId) = behandlingUnderBeslutning(
                tac = tac,
                skalJournalføreNotat = saksbehandlersValgMedBarnetillegg,
                barnetilleggBegrunnelse = "Begrunnelse for barnetillegget.",
            )

            oppdaterRammebehandlingForBeslutter(tac, sakId, behandlingId, SkalJournalføreNotat.INGEN, beslutter)
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.beslutterEndringer.skalJournalføreNotat shouldBe RammebehandlingSkalJournalføreNotatEndring(
                    tidspunkt = it.sistEndret,
                    vilkårsvurdering = VilkårsvurderingSkalJournalføreNotatEndring(saksbehandler = true, beslutter = false),
                    barnetillegg = BarnetilleggSkalJournalføreNotatEndring(saksbehandler = true, beslutter = false),
                )
            }

            oppdaterRammebehandlingForBeslutter(tac, sakId, behandlingId, SkalJournalføreNotat(vilkårsvurdering = true, barnetillegg = false), beslutter)
            val behandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
            behandling.beslutterEndringer.skalJournalføreNotat shouldBe RammebehandlingSkalJournalføreNotatEndring(
                tidspunkt = behandling.sistEndret,
                vilkårsvurdering = null,
                barnetillegg = BarnetilleggSkalJournalføreNotatEndring(saksbehandler = true, beslutter = false),
            )
            hentBeslutterEndringerFraDb(tac, behandlingId).shouldEqualJson(
                """
                {
                  "skalJournalføreNotat": {
                    "tidspunkt": "${behandling.sistEndret}",
                    "vilkårsvurdering": null,
                    "barnetillegg": {"saksbehandler": true, "beslutter": false}
                  }
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `beslutter som endrer tilbake til saksbehandlers valg, nullstiller endringen`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, behandlingId) = behandlingUnderBeslutning(tac)

            oppdaterRammebehandlingForBeslutter(tac, sakId, behandlingId, SkalJournalføreNotat.INGEN, beslutter)
            val json = oppdaterRammebehandlingForBeslutter(tac, sakId, behandlingId, saksbehandlersValg, beslutter)!!

            JSONObject(json).getJSONObject("beslutterEndringer").isNull("skalJournalføreNotat") shouldBe true
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.skalJournalføreNotat shouldBe saksbehandlersValg
                it.beslutterEndringer.skalJournalføreNotat shouldBe null
            }
        }
    }

    @Test
    fun `beslutters endring beholdes etter underkjenning og nullstilles når saksbehandler sender til beslutning igjen`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, behandlingId) = behandlingUnderBeslutning(tac)
            oppdaterRammebehandlingForBeslutter(tac, sakId, behandlingId, SkalJournalføreNotat.INGEN, beslutter)
            val endring = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).beslutterEndringer.skalJournalføreNotat
            endring!!.vilkårsvurdering shouldBe VilkårsvurderingSkalJournalføreNotatEndring(saksbehandler = true, beslutter = false)
            underkjennForBehandlingId(tac, sakId, behandlingId, beslutter = beslutter)

            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sakId,
                behandlingId = behandlingId,
                begrunnelseVilkårsvurdering = begrunnelse,
                skalJournalføreNotat = saksbehandlersValg,
                saksbehandler = saksbehandler,
            )
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).beslutterEndringer.skalJournalføreNotat shouldBe endring

            sendSøknadsbehandlingTilBeslutningForBehandlingId(tac, sakId, behandlingId, saksbehandler)
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).also {
                it.skalJournalføreNotat shouldBe saksbehandlersValg
                it.beslutterEndringer.skalJournalføreNotat shouldBe null
            }
            hentBeslutterEndringerFraDb(tac, behandlingId).shouldEqualJson("""{"skalJournalføreNotat": null}""")
        }
    }

    @Test
    fun `beslutter kan endre valget for en revurdering`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, rammevedtak, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(
                tac = tac,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
            )
            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                begrunnelseVilkårsvurdering = begrunnelse,
                stansFraOgMed = (rammevedtak.rammebehandling as Søknadsbehandling).vedtaksperiode!!.fraOgMed,
                harValgtStansFraFørsteDagSomGirRett = false,
                valgteHjemler = setOf(HjemmelForStans.Alder),
                saksbehandler = saksbehandler,
                skalJournalføreNotat = saksbehandlersValg,
            )
            sendRevurderingTilBeslutningForBehandlingId(tac, sak.id, revurdering.id, saksbehandler)
            taRammebehandlinger(tac, behandlinger = listOf(sak.id to revurdering.id), saksbehandler = beslutter)

            oppdaterRammebehandlingForBeslutter(tac, sak.id, revurdering.id, SkalJournalføreNotat.INGEN, beslutter)

            tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id).also {
                it.skalJournalføreNotat shouldBe SkalJournalføreNotat.INGEN
                it.beslutterEndringer.skalJournalføreNotat shouldBe RammebehandlingSkalJournalføreNotatEndring(
                    tidspunkt = it.sistEndret,
                    vilkårsvurdering = VilkårsvurderingSkalJournalføreNotatEndring(saksbehandler = true, beslutter = false),
                    barnetillegg = null,
                )
            }
        }
    }

    @Test
    fun `beslutter kan ikke velge å journalføre en begrunnelse som mangler`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, behandlingId) = behandlingUnderBeslutning(tac)

            oppdaterRammebehandlingForBeslutter(
                tac = tac,
                sakId = sakId,
                behandlingId = behandlingId,
                skalJournalføreNotat = SkalJournalføreNotat(vilkårsvurdering = true, barnetillegg = true),
                beslutter = beslutter,
                forventet = ForventetRespons(
                    status = 400,
                    contentType = "application/json; charset=UTF-8",
                    body = ForventetBody.Json(serialize(Standardfeil.måHaBegrunnelseForBarnetilleggForÅJournalføreNotat())),
                ),
            )
            tac.behandlingContext.rammebehandlingRepo.hent(behandlingId).beslutterEndringer.skalJournalføreNotat shouldBe null
        }
    }

    @Test
    fun `en annen beslutter enn den som er tildelt kan ikke endre valget`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, behandlingId) = behandlingUnderBeslutning(tac)

            oppdaterRammebehandlingForBeslutter(
                tac = tac,
                sakId = sakId,
                behandlingId = behandlingId,
                skalJournalføreNotat = SkalJournalføreNotat.INGEN,
                beslutter = ObjectMother.beslutter(navIdent = "B999999"),
                forventet = ForventetRespons(
                    status = 400,
                    contentType = "application/json; charset=UTF-8",
                    body = ForventetBody.Json(
                        """{"melding":"Du kan ikke oppdatere behandlingen, da du ikke er beslutter for den.","kode":"må_være_beslutter_for_behandlingen"}""",
                    ),
                ),
            )
        }
    }

    @Test
    fun `valget kan ikke endres før behandlingen er under beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac = tac,
                saksbehandler = saksbehandler,
                begrunnelseVilkårsvurdering = Begrunnelse.createOrThrow(begrunnelse),
                skalJournalføreNotat = saksbehandlersValg,
            )

            oppdaterRammebehandlingForBeslutter(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                skalJournalføreNotat = SkalJournalføreNotat.INGEN,
                beslutter = beslutter,
                forventet = ForventetRespons(
                    status = 409,
                    contentType = "application/json; charset=UTF-8",
                    body = ForventetBody.Json(
                        """{"melding":"Behandlingen er ikke under beslutning. Status: UNDER_BEHANDLING.","kode":"behandlingen_er_ikke_under_beslutning"}""",
                    ),
                ),
            )
        }
    }

    private suspend fun ApplicationTestBuilder.behandlingUnderBeslutning(
        tac: TestApplicationContext,
        skalJournalføreNotat: SkalJournalføreNotat = saksbehandlersValg,
        barnetilleggBegrunnelse: String? = null,
    ): Pair<SakId, RammebehandlingId> {
        val innvilgelsesperioder = ObjectMother.innvilgelsesperioder()
        val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
            tac = tac,
            saksbehandler = saksbehandler,
            begrunnelseVilkårsvurdering = Begrunnelse.createOrThrow(begrunnelse),
            skalJournalføreNotat = skalJournalføreNotat,
            innvilgelsesperioder = innvilgelsesperioder,
            barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder)
                .copy(begrunnelse = barnetilleggBegrunnelse?.let { Begrunnelse.createOrThrow(it) }),
        )
        sendSøknadsbehandlingTilBeslutningForBehandlingId(tac, sak.id, behandling.id, saksbehandler)
        taRammebehandlinger(tac, behandlinger = listOf(sak.id to behandling.id), saksbehandler = beslutter)
        (tac.behandlingContext.rammebehandlingRepo.hent(behandling.id) as Søknadsbehandling).beslutter shouldBe beslutter.navIdent
        return sak.id to behandling.id
    }

    private fun hentBeslutterEndringerFraDb(tac: TestApplicationContextMedPostgres, behandlingId: RammebehandlingId): String =
        tac.sessionFactory.withSession { session ->
            session.run(
                queryOf("select beslutter_endringer from behandling where id = :id", mapOf("id" to behandlingId.toString()))
                    .map { it.string("beslutter_endringer") }
                    .asSingle,
            )!!
        }
}
