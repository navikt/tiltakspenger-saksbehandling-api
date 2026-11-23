package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.ubestemt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.infra.route.rammebehandlingJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettUbestemtRevurdering
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.konverterRevurdering
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
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
            behandlingJson.getJSONArray("automatiskOpprettedeGrunner").length() shouldBe 1
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

    /**
     * Jobben for oppdatert tiltaksdeltakelse legger til grunner mens saksbehandler kan ha revurderingen åpen.
     * Saksbehandlers lagring skal derfor ikke overskrive dem, og en revurdering som ikke lenger er ubestemt skal ikke få nye.
     */
    @Test
    fun `grunn lagt til mens revurderingen er åpen beholdes ved lagring og konvertering, og varsles til den er lagret`() {
        withTestApplicationContextAndPostgres { tac ->
            val repo = tac.behandlingContext.rammebehandlingRepo
            val (sak, revurdering) = iverksettSøknadsbehandlingOgOpprettUbestemtRevurdering(tac)
            taRammebehandlinger(tac, listOf(sak.id to revurdering.id))!!
            val tatt = repo.hent(revurdering.id) as Revurdering
            val medNyGrunn = tatt.leggTilAutomatiskOpprettetGrunn(
                AutomatiskOpprettetRevurderingGrunn(
                    endring = TiltaksdeltakerEndring.IkkeAktuellDeltakelse,
                    tiltaksdeltakerId = revurdering.automatiskOpprettetGrunn!!.tiltaksdeltakerId,
                    lagtTil = nå(tac.clock),
                ),
            )

            repo.lagreAutomatiskOpprettedeGrunner(medNyGrunn, null) shouldBe true
            // Saksbehandlers lagring tar utgangspunkt i revurderingen fra før grunnen ble lagt til.
            repo.lagre(tatt)
            (repo.hent(revurdering.id) as Revurdering).automatiskOpprettedeGrunner shouldBe medNyGrunn.automatiskOpprettedeGrunner
            hentSakForSaksnummer(tac, sak.saksnummer)!!.rammebehandling(revurdering.id).getJSONArray("automatiskOpprettedeGrunner").let {
                it.length() shouldBe 2
                it.getJSONObject(0).getBoolean("lagtTilEtterSistEndret") shouldBe false
                it.getJSONObject(1).getBoolean("lagtTilEtterSistEndret") shouldBe true
            }

            val (_, konvertert) = konverterRevurdering(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                revurderingType = "STANS",
            )!!

            konvertert.automatiskOpprettedeGrunner shouldBe medNyGrunn.automatiskOpprettedeGrunner
            hentSakForSaksnummer(tac, sak.saksnummer)!!.rammebehandling(revurdering.id).getJSONArray("automatiskOpprettedeGrunner")
                .getJSONObject(1).getBoolean("lagtTilEtterSistEndret") shouldBe false
            repo.lagreAutomatiskOpprettedeGrunner(medNyGrunn, null) shouldBe false
            (repo.hent(revurdering.id) as Revurdering).automatiskOpprettedeGrunner shouldBe medNyGrunn.automatiskOpprettedeGrunner
        }
    }

    /**
     * Saksbehandler kan ha lastet revurderingen før jobben la til en ny grunn.
     * Konvertering og avbryting avvises da, slik at endringen ikke blir stående som håndtert uten at noen har sett den.
     */
    @Test
    fun `konvertering og avbryting avvises når det er lagt til grunner saksbehandler ikke har fått vist`() {
        withTestApplicationContextAndPostgres { tac ->
            val repo = tac.behandlingContext.rammebehandlingRepo
            val (sak, revurdering) = iverksettSøknadsbehandlingOgOpprettUbestemtRevurdering(tac)
            taRammebehandlinger(tac, listOf(sak.id to revurdering.id))!!
            val medNyGrunn = (repo.hent(revurdering.id) as Revurdering).leggTilAutomatiskOpprettetGrunn(
                AutomatiskOpprettetRevurderingGrunn(
                    endring = TiltaksdeltakerEndring.IkkeAktuellDeltakelse,
                    tiltaksdeltakerId = revurdering.automatiskOpprettetGrunn!!.tiltaksdeltakerId,
                    lagtTil = nå(tac.clock),
                ),
            )
            repo.lagreAutomatiskOpprettedeGrunner(medNyGrunn, null) shouldBe true
            val forventetFeil = ForventetRespons(
                status = 409,
                body = ForventetBody.Json(
                    """
                    {
                      "melding": "Tiltaksdeltakelsen er endret igjen siden du åpnet behandlingen. Last inn behandlingen på nytt og se gjennom de nye endringene.",
                      "kode": "har_nye_automatisk_opprettede_grunner"
                    }
                    """.trimIndent(),
                ),
                contentType = "application/json; charset=UTF-8",
            )

            konverterRevurdering(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                revurderingType = "STANS",
                antallVisteAutomatiskOpprettedeGrunner = 1,
                forventet = forventetFeil,
            ).shouldBeNull()
            avbrytRammebehandling(
                tac = tac,
                saksnummer = sak.saksnummer,
                sakId = sak.id,
                rammebehandlingId = revurdering.id,
                antallVisteAutomatiskOpprettedeGrunner = 1,
                forventet = forventetFeil,
            ).shouldBeNull()
            repo.hent(revurdering.id) shouldBe medNyGrunn

            val (_, _, avbrutt) = avbrytRammebehandling(
                tac = tac,
                saksnummer = sak.saksnummer,
                sakId = sak.id,
                rammebehandlingId = revurdering.id,
                antallVisteAutomatiskOpprettedeGrunner = 2,
            )!!
            avbrutt!!.status shouldBe Rammebehandlingsstatus.AVBRUTT
        }
    }

    @Test
    fun `konvertering og avbryting avvises når antallet viste grunner mangler`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, revurdering) = iverksettSøknadsbehandlingOgOpprettUbestemtRevurdering(tac)
            taRammebehandlinger(tac, listOf(sak.id to revurdering.id))!!
            val saksbehandler = ObjectMother.saksbehandler()
            val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
            tac.leggTilBruker(jwt, saksbehandler)
            val forventet = ForventetRespons(status = 400, contentType = "application/json; charset=UTF-8")

            defaultRequestWithAssertions(
                HttpMethod.POST,
                "/sak/${sak.id}/behandling/${revurdering.id}/konverter",
                jwt = jwt,
                forventet = forventet,
                body = """{"revurderingType": "STANS"}""",
            )
            defaultRequestWithAssertions(
                HttpMethod.POST,
                "/sak/${sak.saksnummer}/avbryt-aktiv-behandling",
                jwt = jwt,
                forventet = forventet,
                body = """{"behandlingId": "${revurdering.id}", "begrunnelse": "begrunnelse"}""",
            )
            val lagret = tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id) as Revurdering
            lagret.resultat shouldBe Revurderingsresultat.Ubestemt
            lagret.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
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
                revurdering.copy(automatiskOpprettedeGrunner = null)
            }.message shouldBe "En ubestemt revurdering må ha en automatisk opprettet grunn"
        }
    }
}
