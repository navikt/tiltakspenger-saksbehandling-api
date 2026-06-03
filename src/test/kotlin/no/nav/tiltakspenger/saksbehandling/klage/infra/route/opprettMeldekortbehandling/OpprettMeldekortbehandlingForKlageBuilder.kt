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
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldekortbehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.vurder.Vurderingstype
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettKlagebehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.vurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.json.JSONObject

interface OpprettMeldekortbehandlingForKlageBuilder {
    /**
     * 1. Iverksetter søknadsbehandling og en første meldekortbehandling → meldekortvedtak (brukes som formkrav i klagebehandlingen)
     * 2. Starter klagebehandling med vedtakDetKlagesPå = meldekortvedtak
     * 3. Vurderer til omgjøring
     * 4. Oppretter meldekortbehandling for klage (KORRIGERING på samme kjede)
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandlingForKlage(
        tac: TestApplicationContext,
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        saksbehandlerMeldekortbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        vedtaksperiode: Periode = 1.desember(2024).til(14.desember(2024)),
    ): Triple<Sak, Klagebehandling, MeldekortUnderBehandling> {
        val (sakEtterMeldekortbehandling, _, _, meldekortvedtak) = iverksettSøknadsbehandlingOgMeldekortbehandling(
            tac = tac,
            saksbehandler = saksbehandlerMeldekortbehandling,
            beslutter = beslutter,
            vedtaksperiode = vedtaksperiode,
        ) ?: error("Kunne ikke iverksette søknadsbehandling og meldekortbehandling")

        val (_, klagebehandling, _) = opprettKlagebehandlingForSakId(
            tac = tac,
            sakId = sakEtterMeldekortbehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            vedtakDetKlagesPå = meldekortvedtak.id,
        ) ?: error("Kunne ikke opprette klagebehandling")

        val (sakEtterVurdering, vurdertKlagebehandling, _) = vurderKlagebehandling(
            tac = tac,
            sakId = sakEtterMeldekortbehandling.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            vurderingstype = Vurderingstype.OMGJØR,
            begrunnelse = Begrunnelse.createOrThrow("Begrunnelse for omgjøring"),
            årsak = KlageOmgjøringsårsak.PROSESSUELL_FEIL,
            hjemler = null,
        ) ?: error("Kunne ikke vurdere klagebehandling")

        val førsteMeldeperiode = sakEtterVurdering.meldeperiodeKjeder.sisteMeldeperiodePerKjede.first()
        val (oppdatertSak, meldekortbehandling, _) = opprettMeldekortbehandlingForKlage(
            tac = tac,
            sakId = sakEtterMeldekortbehandling.id,
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
    ): Triple<Sak, MeldekortUnderBehandling, MeldekortbehandlingDTOJson>? {
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

            val jsonObject: MeldekortbehandlingDTOJson = JSONObject(bodyAsText)
            val meldekortbehandlingId = MeldekortId.fromString(jsonObject.getString("id"))

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
