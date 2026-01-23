package no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprettRammebehandling

import arrow.core.Tuple4
import arrow.core.Tuple5
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
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgVurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.OpprettRammebehandlingFraKlage]
 */
interface OpprettRammebehandlingForKlageBuilder {
    /** 1. Oppretter ny sak
     *  2. Starter klagebehandling med godkjente formkrav
     *  3. Oppdaterer formkrav
     *
     * @param type En av: [SØKNADSBEHANDLING_INNVILGELSE, REVURDERING_INNVILGELSE, REVURDERING_OMGJØRING]
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
        tac: TestApplicationContext,
        saksbehandlerSøknadsbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSøknadsbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        journalpostId: JournalpostId = JournalpostId("12345"),
        erKlagerPartISaken: Boolean = true,
        klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        erKlagenSignert: Boolean = true,
        begrunnelse: Begrunnelse = Begrunnelse.createOrThrow("Begrunnelse for omgjøring"),
        årsak: KlageOmgjøringsårsak = KlageOmgjøringsårsak.PROSESSUELL_FEIL,
        type: String = "SØKNADSBEHANDLING_INNVILGELSE",
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Tuple5<Sak, Søknad, Søknadsbehandling, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling, klagebehandling, _) = this.iverksettSøknadsbehandlingOgVurderKlagebehandling(
            tac = tac,
            saksbehandlerSøknadsbehandling = saksbehandlerSøknadsbehandling,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
            journalpostId = journalpostId,
            erKlagerPartISaken = erKlagerPartISaken,
            klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
            erKlagefristenOverholdt = erKlagefristenOverholdt,
            erUnntakForKlagefrist = erUnntakForKlagefrist,
            erKlagenSignert = erKlagenSignert,
        ) ?: return null
        val søknadId = søknad.id
        val vedtakIdSomOmgjøres = rammevedtakSøknadsbehandling.id.toString()
        val (oppdatertSak, søknadsbehandling, oppdatertKlagebehandling, json) = opprettRammebehandlingForKlage(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            søknadId = søknadId,
            vedtakIdSomOmgjøres = vedtakIdSomOmgjøres,
            type = type,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        return Tuple5(oppdatertSak, søknad, søknadsbehandling as Søknadsbehandling, oppdatertKlagebehandling, json)
    }

    /**
     * Forventer at det allerede finnes en sak, formkravene er OK og man har vurdert til omgjøring.
     * @param type En av: [SØKNADSBEHANDLING_INNVILGELSE, REVURDERING_INNVILGELSE, REVURDERING_OMGJØRING]
     * @param søknadId Påkrevt ved [type] SØKNADSBEHANDLING_INNVILGELSE
     * @param vedtakIdSomOmgjøres Påkrevt ved [type] REVURDERING_OMGJØRING
     */
    suspend fun ApplicationTestBuilder.opprettRammebehandlingForKlage(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        søknadId: SøknadId? = null,
        vedtakIdSomOmgjøres: String?,
        type: String,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Tuple4<Sak, Rammebehandling, Klagebehandling, KlagebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/klage/$klagebehandlingId/opprettRammebehandling")
            },
            jwt = jwt,
        ) {
            setBody(
                //language=JSON
                """
                {
                    "søknadId": "${søknadId?.toString()}",
                    "vedtakIdSomOmgjøres": "$vedtakIdSomOmgjøres",
                    "type": "$type",
                    "klagebehandlingId": "$klagebehandlingId"
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
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Tuple4(
                oppdatertSak,
                oppdatertSak.rammebehandlinger.last(),
                oppdatertSak.hentKlagebehandling(klagebehandlingId),
                jsonObject,
            )
        }
    }
}
