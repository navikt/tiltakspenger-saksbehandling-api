package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppdater

import io.kotest.assertions.json.CompareJsonOptions
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.brev.oppdaterTekstTilBrev]
 */
interface OppdaterKlagebehandlingBrevtekstBuilder {
    /**
     * 1. Oppretter ny sak
     * 2. Starter klagebehandling til avvisning
     * 3. Oppdaterer brevtekst
     */
    suspend fun ApplicationTestBuilder.opprettSakOgOppdaterKlagebehandlingBrevtekst(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        brevtekst: List<TittelOgTekst> = listOf(
            TittelOgTekst(
                tittel = NonBlankString.create("Avvisning av klage"),
                tekst = NonBlankString.create("Din klage er dessverre avvist."),
            ),
        ),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, klagebehandling, _) = this.opprettSakOgKlagebehandlingTilAvvisning(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,

        ) ?: return null
        return oppdaterKlagebehandlingBrevtekstForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            brevtekst = brevtekst,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        )
    }

    /** Forventer at det allerede finnes en sak. */
    suspend fun ApplicationTestBuilder.oppdaterKlagebehandlingBrevtekstForSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        brevtekst: List<TittelOgTekst> = listOf(
            TittelOgTekst(
                tittel = NonBlankString.create("Avvisning av klage"),
                tekst = NonBlankString.create("Din klage er dessverre avvist."),
            ),
        ),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Put,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/klage/$klagebehandlingId/brevtekst")
            },
            jwt = jwt,
        ) {
            val tekstTilVedtaksbrevListe = brevtekst.joinToString(separator = ",") {
                """
                {
                    "tittel": "${it.tittel.value}",
                    "tekst": "${it.tekst.value}"
                }
                """.trimIndent()
            }
            setBody(
                //language=JSON
                """
                {
                    "tekstTilVedtaksbrev": [$tekstTilVedtaksbrevListe]
                }
                """.trimIndent(),
            )
        }
            .apply {
                val bodyAsText = this.bodyAsText()
                withClue(
                    "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
                ) {
                    if (forventetStatus != null) status shouldBe forventetStatus
                }

                if (forventetJsonBody != null) {
                    bodyAsText.shouldEqualJson(forventetJsonBody)
                }
                if (status != HttpStatusCode.OK) return null
                val jsonObject: KlagebehandlingDTOJson = objectMapper.readTree(bodyAsText)
                val klagebehandlingId = KlagebehandlingId.fromString(jsonObject.get("id").asText())
                val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
                return Triple(
                    oppdatertSak,
                    oppdatertSak.hentKlagebehandling(klagebehandlingId),
                    jsonObject,
                )
            }
    }
}
