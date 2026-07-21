package no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprettBehandling

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
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.infra.route.AttesterbarBehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldekortbehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.infra.route.RammebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.GenerererKlageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.klageinstanshendelse.mottaHendelseFraKlageinstansen
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.vurder.Vurderingstype
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.ferdigstiltOpprettholdtKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgVurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettKlagebehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOpprettholdKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.vurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.json.JSONObject
import java.util.UUID

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprettBehandling.opprettBehandlingForKlageRoute]
 */
interface OpprettBehandlingForKlageBuilder {
    /** 1. Oppretter ny sak
     *  2. iverksetter en søknadsbehandling
     *  3. oppretter klagebehandling
     *  4. vurderer klagebehandling til omgjøring
     *  5. oppretter ny søknadsbehandling
     */
    suspend fun ApplicationTestBuilder.opprettetSøknadsbehandlingForKlage(
        tac: TestApplicationContext,
        saksbehandlerSøknadsbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSøknadsbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        journalpostId: JournalpostId = JournalpostId("12345"),
        begrunnelse: Begrunnelse = Begrunnelse.createOrThrow("Begrunnelse for omgjøring"),
        årsak: KlageOmgjøringsårsak = KlageOmgjøringsårsak.PROSESSUELL_FEIL,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, Søknadsbehandling, RammebehandlingDTOJson>? {
        val (sak, søknad, _, klagebehandling, _) = this.iverksettSøknadsbehandlingOgVurderKlagebehandling(
            tac = tac,
            saksbehandlerSøknadsbehandling = saksbehandlerSøknadsbehandling,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
            journalpostId = journalpostId,
            erKlagerPartISaken = true,
            klagesDetPåKonkreteElementerIVedtaket = true,
            erKlagefristenOverholdt = true,
            erUnntakForKlagefrist = null,
            erKlagenSignert = true,
        ) ?: return null
        val søknadId = søknad.id
        val (sakEtterOpprettelseAvKlage, rammebehandling, rammebehandlingJson) = opprettBehandlingForKlage(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            søknadId = søknadId,
            vedtakIdSomOmgjøres = null,
            type = "SØKNADSBEHANDLING_INNVILGELSE",
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        )!!

        return Triple(sakEtterOpprettelseAvKlage, rammebehandling as Søknadsbehandling, rammebehandlingJson)
    }

    /** 1. Oppretter ny sak
     *  2. iverksetter en søknadsbehandling
     *  3. oppretter klagebehandling
     *  4. vurderer klagebehandling til omgjøring
     *  5. oppretter ny revurdering
     *
     *  @param type må være en av [REVURDERING_INNVILGELSE, REVURDERING_OMGJØRING]
     */
    suspend fun ApplicationTestBuilder.opprettetRevurderingForKlage(
        tac: TestApplicationContext,
        saksbehandlerSøknadsbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSøknadsbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        type: String = "REVURDERING_OMGJØRING",
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, Revurdering, RammebehandlingDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling, klagebehandling, _) = this.iverksettSøknadsbehandlingOgVurderKlagebehandling(
            tac = tac,
            saksbehandlerSøknadsbehandling = saksbehandlerSøknadsbehandling,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
        ) ?: return null
        val søknadId = søknad.id
        val vedtakIdSomOmgjøres = rammevedtakSøknadsbehandling.id.toString()
        val (sakEtterOpprettelseAvKlage, revurdering, revurderingJson) = opprettBehandlingForKlage(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            søknadId = søknadId,
            vedtakIdSomOmgjøres = if (type == "REVURDERING_OMGJØRING") vedtakIdSomOmgjøres else null,
            type = type,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        )!!

        return Triple(sakEtterOpprettelseAvKlage, revurdering as Revurdering, revurderingJson)
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
    suspend fun ApplicationTestBuilder.opprettetRammebehandlingMedOpprettholdtKlage(
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

        val (sakEtterOpprettelseAvBehandling, rammebehandling, json) = opprettBehandlingForKlage(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            søknadId = søknadId,
            vedtakIdSomOmgjøres = vedtakIdSomOmgjøres,
            type = behandlingstype,
        )!!

        return Tuple4(
            sakEtterOpprettelseAvBehandling,
            rammebehandling as Rammebehandling,
            rammebehandling.klagebehandling!!,
            json,
        )
    }

    /**
     * oppretterholder klagebehandling som deretter fører til en omgjøring
     * @param type En av: [SØKNADSBEHANDLING_INNVILGELSE, REVURDERING_INNVILGELSE, REVURDERING_OMGJØRING]
     */
    suspend fun ApplicationTestBuilder.rammebehandlingMedFerdigstiltOpprettholdtKlage(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        type: String = "SØKNADSBEHANDLING_INNVILGELSE",
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Tuple5<Sak, Rammebehandling, Klagebehandling, RammebehandlingDTOJson, KlagebehandlingDTOJson>? {
        val (sak, ferdigstiltKlagebehandling, klagebehandlingJson) = this.ferdigstiltOpprettholdtKlagebehandling(
            tac = tac,
            saksbehandler = saksbehandler,
        )!!

        val (sakEtterRammebehandling, rammebehandling, rammebehandlingJson) = opprettBehandlingForKlage(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = ferdigstiltKlagebehandling.id,
            saksbehandler = saksbehandler,
            søknadId = if (type == "SØKNADSBEHANDLING_INNVILGELSE") sak.søknader.single().id else null,
            vedtakIdSomOmgjøres = if (type == "REVURDERING_OMGJØRING") ferdigstiltKlagebehandling.formkrav.vedtakDetKlagesPå!!.toString() else null,
            type = type,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        )!!

        return Tuple5(
            sakEtterRammebehandling,
            rammebehandling as Rammebehandling,
            ferdigstiltKlagebehandling,
            rammebehandlingJson,
            klagebehandlingJson,
        )
    }

    /**
     * 1. Iverksetter søknadsbehandling og en første meldekortbehandling → meldekortvedtak (brukes som formkrav i klagebehandlingen)
     * 2. Starter klagebehandling med vedtakDetKlagesPå = meldekortvedtak
     * 3. Vurderer til omgjøring
     * 4. Oppretter meldekortbehandling for klage (KORRIGERING på samme kjede)
     */
    suspend fun ApplicationTestBuilder.opprettMeldekortbehandlingForKlage(
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
        val (oppdatertSak, meldekortbehandling, _) = opprettBehandlingForKlage(
            tac = tac,
            sakId = sakEtterMeldekortbehandling.id,
            klagebehandlingId = vurdertKlagebehandling.id,
            kjedeId = førsteMeldeperiode.kjedeId,
            saksbehandler = saksbehandlerKlagebehandling,
            vedtakIdSomOmgjøres = null,
            type = "MELDEKORTBEHANDLING",
        ) ?: error("Kunne ikke opprette meldekortbehandling for klage")

        return Triple(
            oppdatertSak,
            requireNotNull(meldekortbehandling.klagebehandling),
            meldekortbehandling as MeldekortUnderBehandling,
        )
    }

    suspend fun ApplicationTestBuilder.opprettMeldekortbehandlingForKlageForSak(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        kjedeId: MeldeperiodeKjedeId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    ): Triple<Sak, MeldekortUnderBehandling, MeldekortbehandlingDTOJson>? {
        val (sak, meldekortbehandling, meldekortbehandlingJson) = opprettBehandlingForKlage(
            tac = tac,
            sakId = sakId,
            klagebehandlingId = klagebehandlingId,
            saksbehandler = saksbehandler,
            søknadId = null,
            vedtakIdSomOmgjøres = null,
            kjedeId = kjedeId,
            type = "MELDEKORTBEHANDLING",
        )!!

        return Triple(sak, meldekortbehandling as MeldekortUnderBehandling, meldekortbehandlingJson)
    }

    /**
     * Forventer at det allerede finnes en sak, formkravene er OK og man har vurdert til omgjøring.
     * @param type En av: [SØKNADSBEHANDLING_INNVILGELSE, REVURDERING_INNVILGELSE, REVURDERING_OMGJØRING, MELDEKORTBEHANDLING]
     * @param søknadId Påkrevt ved [type] SØKNADSBEHANDLING_INNVILGELSE
     * @param kjedeId Påkrevd ved [type] MELDEKORTBEHANDLING
     * @param vedtakIdSomOmgjøres Påkrevt ved [type] REVURDERING_OMGJØRING & MELDEKORTBEHANDLING
     *
     * @return Merk at [AttesterbarBehandling] inneholder [Klagebehandling]
     */
    suspend fun ApplicationTestBuilder.opprettBehandlingForKlage(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        søknadId: SøknadId? = null,
        vedtakIdSomOmgjøres: String?,
        kjedeId: MeldeperiodeKjedeId? = null,
        type: String,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, AttesterbarBehandling, AttesterbarBehandlingDTOJson>? {
        if (type == "REVURDERING_OMGJØRING") require(vedtakIdSomOmgjøres != null) { "vedtakIdSomSkalOmgjøres må oppgis ved type REVURDERING_OMGJØRING" }
        if (type == "MELDEKORTBEHANDLING") require(kjedeId != null) { "kjedeId på oppgis ved type MELDEKORTBEHANDLING" }

        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/klage/$klagebehandlingId/opprettBehandling")
            },
            jwt = jwt,
            forventet = forventetStatus?.let { ForventetRespons(status = it) },
        ) {
            setBody(
                //language=JSON
                """
                {
                    "søknadId": "${søknadId?.toString()}",
                    "vedtakIdSomSkalOmgjøres": "$vedtakIdSomOmgjøres",
                    "type": "$type",
                    "kjedeId": ${kjedeId?.let { """"${kjedeId.fraOgMed}/${kjedeId.tilOgMed}"""" }}
                }
                """.trimIndent(),
            )
        }.apply {
            val bodyAsText = this.bodyAsText()

            if (forventetJsonBody != null) {
                bodyAsText.shouldEqualJson(forventetJsonBody)
            }
            if (status != HttpStatusCode.OK) return null
            val jsonObject: RammebehandlingDTOJson = JSONObject(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                when (type) {
                    "SØKNADSBEHANDLING_INNVILGELSE", "REVURDERING_INNVILGELSE", "REVURDERING_OMGJØRING" -> oppdatertSak.rammebehandlinger.last()
                    "MELDEKORTBEHANDLING" -> oppdatertSak.meldekortbehandlinger.last()
                    else -> error("ukjent return-domene type for $type")
                },
                jsonObject,
            )
        }
    }
}
