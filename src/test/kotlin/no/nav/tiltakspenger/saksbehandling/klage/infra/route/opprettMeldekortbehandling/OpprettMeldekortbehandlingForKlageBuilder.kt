package no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprettMeldekortbehandling

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldeperiodeKjedeDTOJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.vurder.Vurderingstype
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettKlagebehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.vurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.json.JSONObject

interface OpprettMeldekortbehandlingForKlageBuilder {
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandlingForKlage(
        tac: TestApplicationContext,
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
    ): Triple<Sak, Klagebehandling, MeldekortUnderBehandling> {
        val (sak, _, rammevedtak, _) = iverksettSøknadsbehandling(
            tac = tac,
            saksbehandler = saksbehandlerKlagebehandling,
        )
        val (_, klagebehandling, _) = opprettKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            saksbehandler = saksbehandlerKlagebehandling,
            vedtakDetKlagesPå = rammevedtak.id,
        ) ?: error("Kunne ikke opprette klagebehandling")
        val (_, vurdertKlagebehandling, _) = vurderKlagebehandling(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            vurderingstype = Vurderingstype.OMGJØR,
            begrunnelse = Begrunnelse.createOrThrow("Begrunnelse for omgjøring"),
            årsak = KlageOmgjøringsårsak.PROSESSUELL_FEIL,
            hjemler = null,
        ) ?: error("Kunne ikke vurdere klagebehandling")

        val førsteMeldeperiode = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede.first()
        val (oppdatertSak, meldekortbehandling, _) = opprettMeldekortbehandlingForKlage(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = vurdertKlagebehandling.id,
            kjedeId = førsteMeldeperiode.kjedeId,
            saksbehandler = saksbehandlerKlagebehandling,
        ) ?: error("Kunne ikke opprette meldekortbehandling for klage")

        return Triple(
            oppdatertSak,
            requireNotNull(meldekortbehandling.klagebehandling),
            meldekortbehandling,
        )
    }

    suspend fun ApplicationTestBuilder.opprettMeldekortbehandlingForKlage(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        kjedeId: MeldeperiodeKjedeId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    ): Triple<Sak, MeldekortUnderBehandling, MeldeperiodeKjedeDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Post,
            "/sak/$sakId/klage/$klagebehandlingId/opprettBehandling",
            jwt = jwt,
        ) {
            setBody(
                //language=JSON
                """
                {
                    "type": "MELDEKORTBEHANDLING",
                    "søknadId": null,
                    "vedtakIdSomSkalOmgjøres": null,
                    "kjedeId": "${kjedeId.fraOgMed}/${kjedeId.tilOgMed}"
                }
                """.trimIndent(),
            )
        }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                status shouldBe forventetStatus
            }

            if (status != HttpStatusCode.OK) {
                return null
            }

            val jsonObject: MeldeperiodeKjedeDTOJson = JSONObject(bodyAsText)
            val meldekortbehandlingerJson = jsonObject.getJSONArray("meldekortbehandlinger")
            val meldekortbehandlingJson = meldekortbehandlingerJson.getJSONObject(meldekortbehandlingerJson.length() - 1)
            val meldekortbehandlingId = MeldekortId.fromString(meldekortbehandlingJson.getString("id"))

            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val meldekortbehandling = tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandlingId) as MeldekortUnderBehandling

            return Triple(
                oppdatertSak,
                meldekortbehandling,
                jsonObject,
            )
        }
    }
}
