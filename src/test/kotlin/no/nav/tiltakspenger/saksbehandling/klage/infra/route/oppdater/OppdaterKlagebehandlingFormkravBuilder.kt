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
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageInnsendingskilde
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDate

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.formkrav.oppdaterKlagebehandlingFormkravRoute]
 */
interface OppdaterKlagebehandlingFormkravBuilder {
    /** 1. Oppretter ny sak
     *  2. Starter klagebehandling til avvisning
     *  3. Oppdaterer formkrav
     */
    suspend fun ApplicationTestBuilder.opprettSakOgOppdaterKlagebehandlingFormkrav(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        journalpostId: JournalpostId = JournalpostId("12345"),
        vedtakDetKlagesPå: VedtakId? = null,
        erKlagerPartISaken: Boolean = true,
        klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        erKlagenSignert: Boolean = true,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, klagebehandling, _) =
            this.opprettSakOgKlagebehandlingTilAvvisning(
                tac = tac,
                saksbehandler = saksbehandler,
                fnr = fnr,
            ) ?: return null
        return oppdaterKlagebehandlingFormkravForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            journalpostId = journalpostId,
            vedtakDetKlagesPå = vedtakDetKlagesPå,
            erKlagerPartISaken = erKlagerPartISaken,
            klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
            erKlagefristenOverholdt = erKlagefristenOverholdt,
            erUnntakForKlagefrist = erUnntakForKlagefrist,
            erKlagenSignert = erKlagenSignert,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        )
    }

    /** Forventer at det allerede finnes en sak. */
    suspend fun ApplicationTestBuilder.oppdaterKlagebehandlingFormkravForSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        journalpostId: JournalpostId = JournalpostId("12345"),
        vedtakDetKlagesPå: VedtakId? = null,
        erKlagerPartISaken: Boolean = true,
        klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        erKlagenSignert: Boolean = true,
        innsendingsdato: LocalDate = 16.februar(2026),
        innsendingskilde: KlageInnsendingskilde = KlageInnsendingskilde.DIGITAL,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Put,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/klage/$klagebehandlingId/formkrav")
            },
            jwt = jwt,
        ) {
            setBody(
                //language=JSON
                """
                {
                    "journalpostId": "$journalpostId",
                    "vedtakDetKlagesPå": ${vedtakDetKlagesPå?.let { "\"$it\"" }},
                    "erKlagerPartISaken": $erKlagerPartISaken,
                    "klagesDetPåKonkreteElementerIVedtaket": $klagesDetPåKonkreteElementerIVedtaket,
                    "erKlagefristenOverholdt": $erKlagefristenOverholdt,
                    "erUnntakForKlagefrist": ${erUnntakForKlagefrist?.let { "\"$it\"" }},
                    "erKlagenSignert": $erKlagenSignert,
                    "innsendingsdato": "$innsendingsdato",
                    "innsendingskilde": "${innsendingskilde.name}"
                }
                """.trimIndent(),
            )
        }.apply {
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
            val klagebehandlingId = KlagebehandlingId.fromString(jsonObject.get("id").asString())
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                oppdatertSak.hentKlagebehandling(klagebehandlingId),
                jsonObject,
            )
        }
    }
}
