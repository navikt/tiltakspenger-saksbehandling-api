package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ferdigstill

import arrow.core.Tuple4
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
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.infra.route.RammebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.GenerererKlageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.klageinstanshendelse.mottaHendelseFraKlageinstansen
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterKlagebehandlingBrevtekstForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettKlagebehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOpprettholdKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettholdKlagebehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.vurderKlagebehandlingOpprettholdelseForSakId
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.json.JSONObject
import java.util.UUID

interface FerdigstillKlagebehandlingBuilder {
    /** 1. Oppretter ny sak
     *  2. Starter klagebehandling til opprettholdelse
     *  3. Oppdaterer brevtekst
     *  4. Opprettholder (emulerer journalføring, distribuering av vedtaksbrev, oversendelse til klageinstansen, og utfall fra klageinstansen)
     *  5. Ferdigstiller klagebehandling
     */
    suspend fun ApplicationTestBuilder.opprettSakOgFerdigstillOppretholdtKlagebehandling(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
        utførJobber: Boolean = true,
        hendelseGenerering: (
            sak: Sak,
            klagebehandling: Klagebehandling,
        ) -> String = { sak, klagebehandling ->
            GenerererKlageinstanshendelse.avsluttetJson(
                eventId = UUID.randomUUID().toString(),
                kildeReferanse = klagebehandling.id.toString(),
                kabalReferanse = UUID.randomUUID().toString(),
                avsluttetTidspunkt = nå(tac.clock).toString(),
                utfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE,
                journalpostReferanser = emptyList(),
            )
        },
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, klagebehandling) = this.opprettSakOgOpprettholdKlagebehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        ) ?: return null

        if (utførJobber) {
            tac.mottaHendelseFraKlageinstansen(hendelseGenerering(sak, klagebehandling))
            tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelser()
        }

        return ferdigstillKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        )
    }

    suspend fun ApplicationTestBuilder.opprettOgFerdigstillOppretholdtKlagebehandlingForSak(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        sak: Sak,
        vedtakDetKlagesPå: VedtakId = sak.vedtaksliste.alle.last().id,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
        utførJobber: Boolean = true,
        hendelseGenerering: (
            sak: Sak,
            klagebehandling: Klagebehandling,
        ) -> String = { sak, klagebehandling ->
            GenerererKlageinstanshendelse.avsluttetJson(
                eventId = UUID.randomUUID().toString(),
                kildeReferanse = klagebehandling.id.toString(),
                kabalReferanse = UUID.randomUUID().toString(),
                avsluttetTidspunkt = nå(tac.clock).toString(),
                utfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE,
                journalpostReferanser = emptyList(),
            )
        },
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (_, opprettetklagebehandling, _) = this.opprettKlagebehandlingForSakId(tac = tac, sakId = sak.id, vedtakDetKlagesPå = vedtakDetKlagesPå)!!
        this.vurderKlagebehandlingOpprettholdelseForSakId(tac = tac, sakId = sak.id, klagebehandlingId = opprettetklagebehandling.id)
        this.oppdaterKlagebehandlingBrevtekstForSakId(tac = tac, sakId = sak.id, klagebehandlingId = opprettetklagebehandling.id)
        this.opprettholdKlagebehandlingForSakId(tac = tac, sakId = sak.id, klagebehandlingId = opprettetklagebehandling.id)

        if (utførJobber) {
            tac.mottaHendelseFraKlageinstansen(hendelseGenerering(sak, opprettetklagebehandling))
            tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelser()
        }

        return ferdigstillKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = opprettetklagebehandling.id,
            saksbehandler = saksbehandler,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        )
    }

    suspend fun ApplicationTestBuilder.ferdigstillKlagebehandlingForSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        begrunnelse: String? = null,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Patch,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/klage/$klagebehandlingId/ferdigstill")
            },
            jwt = jwt,
        ) {
            //language=json
            setBody("""{"begrunnelse": ${begrunnelse?.let { "\"$it\"" }}}""".trimIndent())
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

    /** 1. Oppretter ny sak
     *  2. Starter klagebehandling til opprettholdelse
     *  3. Oppdaterer brevtekst
     *  4. Opprettholder (emulerer journalføring, distribuering av vedtaksbrev, oversendelse til klageinstansen, og utfall fra klageinstansen)
     *  5. Oppretter ny rammebehandling
     *  6. vedtar rammebehandling + klagebehandling
     *
     *  Dersom ny rammebehandling er en søknadsbehandling, brukes eksisterende søknad på saken.
     *
     * @param behandlingstype En av: [SØKNADSBEHANDLING_INNVILGELSE, REVURDERING_INNVILGELSE, REVURDERING_OMGJØRING]
     */
    suspend fun ApplicationTestBuilder.opprettSakOgOmgjørFraKaKlagebehandlingMedNyRammebehandling(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
        utførJobber: Boolean = true,
        hendelseGenerering: (
            sak: Sak,
            klagebehandling: Klagebehandling,
        ) -> String = { sak, klagebehandling ->
            GenerererKlageinstanshendelse.avsluttetJson(
                eventId = UUID.randomUUID().toString(),
                kildeReferanse = klagebehandling.id.toString(),
                kabalReferanse = UUID.randomUUID().toString(),
                avsluttetTidspunkt = nå(tac.clock).toString(),
                utfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE,
                journalpostReferanser = emptyList(),
            )
        },
        behandlingstype: String = "REVURDERING_OMGJØRING",
    ): Tuple4<Sak, Rammebehandling, Klagebehandling, RammebehandlingDTOJson>? {
        val (sak, klagebehandling) = this.opprettSakOgOpprettholdKlagebehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        ) ?: return null

        val søknadId = if (behandlingstype == "SØKNADSBEHANDLING_INNVILGELSE") {
            tac.søknadContext.søknadRepo.hentSøknaderForFnr(fnr)
                .single().id
        } else {
            null
        }

        val vedtakIdSomOmgjøres = if (behandlingstype == "REVURDERING_OMGJØRING") {
            sak.rammevedtaksliste.single().id.toString()
        } else {
            null
        }

        if (utførJobber) {
            tac.mottaHendelseFraKlageinstansen(hendelseGenerering(sak, klagebehandling))
            tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelser()
        }

        return omgjørKlagebehandlingOgOpprettNyRammebehandling(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
            behandlingstype = behandlingstype,
            søknadId = søknadId,
            vedtakIdSomOmgjøres = vedtakIdSomOmgjøres,
        )
    }

    /**
     * @param søknadId - må oppgis hvis behandlingstype er SØKNADSBEHANDLING_INNVILGELSE
     * @param vedtakIdSomOmgjøres - må oppgis hvis behandlingstype er REVURDERING_OMGJØRING
     */
    suspend fun ApplicationTestBuilder.omgjørKlagebehandlingOgOpprettNyRammebehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
        behandlingstype: String = "REVURDERING_OMGJØRING",
        søknadId: SøknadId? = null,
        vedtakIdSomOmgjøres: String? = null,
    ): Tuple4<Sak, Rammebehandling, Klagebehandling, RammebehandlingDTOJson>? {
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
            //language=json
            this.setBody(
                """{
                    "type": "$behandlingstype",
                    "søknadId": ${søknadId?.let { "\"$it\"" }},
                    "vedtakIdSomSkalOmgjøres": ${vedtakIdSomOmgjøres?.let { "\"$it\"" }}
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
            val jsonObject: RammebehandlingDTOJson = JSONObject(bodyAsText)
            val behandlingId = BehandlingId.fromString(jsonObject.get("id").toString())
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!

            return Tuple4(
                oppdatertSak,
                oppdatertSak.hentRammebehandling(behandlingId)!!,
                oppdatertSak.hentKlagebehandling(klagebehandlingId),
                jsonObject,
            )
        }
    }
}
