package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.ubestemt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.infra.route.rammebehandlingJson
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettUbestemtRevurdering
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.konverterRevurdering
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import org.json.JSONObject
import org.junit.jupiter.api.Test

class UbestemtRevurderingRouteTest {

    private fun JSONObject.rammebehandling(id: Any): JSONObject {
        val behandlinger = getJSONArray("rammebehandlinger")
        return (0 until behandlinger.length()).map { behandlinger.getJSONObject(it) }
            .single { it.getString("id") == id.toString() }
    }

    @Test
    fun `systemet oppretter en ubestemt revurdering som er klar til behandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, revurdering) = iverksettSøknadsbehandlingOgOpprettUbestemtRevurdering(tac)

            val lagret = tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id) as Revurdering
            lagret shouldBe revurdering
            lagret.resultat shouldBe Revurderingsresultat.Ubestemt
            lagret.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
            lagret.saksbehandler.shouldBeNull()
            lagret.automatiskOpprettetGrunn!!.endring shouldBe TiltaksdeltakerEndring.AvbruttDeltakelse

            val behandlingJson = hentSakForSaksnummer(tac, sak.saksnummer)!!.rammebehandling(revurdering.id)
            behandlingJson.getString("resultat") shouldBe "UBESTEMT"
            behandlingJson.getJSONObject("automatiskOpprettetGrunn").getJSONArray("endringer").length() shouldBe 1
        }
    }

    @Test
    fun `ubestemt revurdering tilbyr konvertering og avbryting når den er tatt, og kan avbrytes`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, revurdering) = iverksettSøknadsbehandlingOgOpprettUbestemtRevurdering(tac)
            taRammebehandlinger(tac, listOf(sak.id to revurdering.id))!!

            val kommandoer = hentSakForSaksnummer(tac, sak.saksnummer)!!.rammebehandling(revurdering.id)
                .getJSONArray("gyldigeKommandoer")
                .let { json -> (0 until json.length()).map { json.getString(it) } }
            kommandoer shouldContain "KonverterRevurdering"
            kommandoer shouldContain "Avbryt"

            val (_, _, avbrutt) = avbrytRammebehandling(
                tac = tac,
                saksnummer = sak.saksnummer,
                sakId = sak.id,
                rammebehandlingId = revurdering.id,
            )!!
            avbrutt!!.status shouldBe Rammebehandlingsstatus.AVBRUTT
        }
    }

    @Test
    fun `ubestemt revurdering konverteres til stans, og beholder grunnen`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, revurdering) = iverksettSøknadsbehandlingOgOpprettUbestemtRevurdering(tac)
            taRammebehandlinger(tac, listOf(sak.id to revurdering.id))!!

            val (_, konvertert, sakJson) = konverterRevurdering(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                revurderingType = "STANS",
            )!!

            konvertert.resultat shouldBe Revurderingsresultat.Stans.empty
            konvertert.automatiskOpprettetGrunn shouldBe revurdering.automatiskOpprettetGrunn
            sakJson.rammebehandlingJson(revurdering.id)["resultat"].asString() shouldBe "STANS"
        }
    }

    @Test
    fun `ubestemt revurdering kan verken oppdateres eller sendes til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, revurdering) = iverksettSøknadsbehandlingOgOpprettUbestemtRevurdering(tac)
            taRammebehandlinger(tac, listOf(sak.id to revurdering.id))!!

            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, _, response) -> response harKode "feil_resultattype" }

            sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )

            val lagret = tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id)
            lagret.resultat shouldBe Revurderingsresultat.Ubestemt
            lagret.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
        }
    }

    @Test
    fun `ubestemt revurdering må ha en automatisk opprettet grunn`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, revurdering) = iverksettSøknadsbehandlingOgOpprettUbestemtRevurdering(tac)

            shouldThrow<IllegalArgumentException> {
                revurdering.copy(automatiskOpprettetGrunn = null)
            }.message shouldBe "En ubestemt revurdering må ha en automatisk opprettet grunn"
        }
    }
}
