package no.nav.tiltakspenger.saksbehandling.klage.infra.route.start

import arrow.core.Tuple4
import arrow.core.Tuple5
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageInnsendingskilde
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagehjemmelDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.vurder.Vurderingstype
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentEllerOpprettSakForSystembruker
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.vurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

interface OpprettKlagebehandlingBuilder {
    /** Oppretter ny sak og starter klagebehandling til avvisning  */
    suspend fun ApplicationTestBuilder.opprettSakOgKlagebehandlingTilAvvisning(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val saksnummer = hentEllerOpprettSakForSystembruker(tac, fnr)
        val tomSak: Sak = tac.sakContext.sakRepo.hentForSaksnummer(saksnummer)!!
        val personopplysningerForBrukerFraPdl = ObjectMother.personopplysningKjedeligFyr(fnr)
        tac.leggTilPerson(fnr, personopplysningerForBrukerFraPdl, tiltaksdeltakelse())
        val (oppdatertSak, klagebehandling, klagebehandlingJson) = this.opprettKlagebehandlingForSakId(
            tac = tac,
            sakId = tomSak.id,
            saksbehandler = saksbehandler,
            forventet = forventet,
        )!!
        return Triple(oppdatertSak, klagebehandling, klagebehandlingJson)
    }

    suspend fun ApplicationTestBuilder.opprettSakOgKlagebehandlingTilOpprettholdelse(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple4<Sak, Rammevedtak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (oppdatertSak, _, rammevedtak, klagebehandling) = this.iverksettSøknadsbehandlingOgOpprettKlagebehandlingTilVurdering(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandler,
            saksbehandlerSøknadsbehandling = saksbehandler,
            fnr = fnr,
            forventet = forventet,
        )!!

        val (sakMedVurdertKlage, vurdertKlage, vurdertKlageJson) = this.vurderKlagebehandling(
            tac = tac,
            sakId = oppdatertSak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            vurderingstype = Vurderingstype.OPPRETTHOLD,
            hjemler = listOf(KlagehjemmelDto.ARBEIDSMARKEDSLOVEN_17),
            begrunnelse = null,
            årsak = null,
        )!!

        return Tuple4(sakMedVurdertKlage, rammevedtak, vurdertKlage, vurdertKlageJson)
    }

    /** Oppretter ny sak, søknad og iverksetter søknadsbehandlingen; og starter klagebehandling med oppfylte formkrav  */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgOpprettKlagebehandlingTilVurdering(
        tac: TestApplicationContext,
        saksbehandlerSøknadsbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSøknadsbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        fnr: Fnr = ObjectMother.gyldigFnr(),
        journalpostId: JournalpostId = JournalpostId("12345"),
        erKlagerPartISaken: Boolean = true,
        klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        erKlagenSignert: Boolean = true,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple5<Sak, Søknad, Rammevedtak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, søknad, vedtakSøknadsbehandling, _) = iverksettSøknadsbehandling(
            tac = tac,
            saksbehandler = saksbehandlerSøknadsbehandling,
            fnr = fnr,
        )
        val (oppdatertSak, klagebehandling, klagebehandlingJson) = this.opprettKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            saksbehandler = saksbehandlerKlagebehandling,
            journalpostId = journalpostId,
            vedtakDetKlagesPå = vedtakSøknadsbehandling.id,
            erKlagerPartISaken = erKlagerPartISaken,
            klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
            erKlagefristenOverholdt = erKlagefristenOverholdt,
            erUnntakForKlagefrist = erUnntakForKlagefrist,
            erKlagenSignert = erKlagenSignert,
            forventet = forventet,
        ) ?: return null
        return Tuple5(oppdatertSak, søknad, vedtakSøknadsbehandling, klagebehandling, klagebehandlingJson)
    }

    suspend fun ApplicationTestBuilder.iverksettMeldekortvedtakOgOpprettKlagebehandlingTilOpprettholdelse(
        tac: TestApplicationContext,
        saksbehandlerMeldekortbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerMeldekortbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        fnr: Fnr = ObjectMother.gyldigFnr(),
        journalpostId: JournalpostId = JournalpostId("12345"),
        erKlagerPartISaken: Boolean = true,
        klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        erKlagenSignert: Boolean = true,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple4<Sak, Meldekortvedtak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, _, _, meldekortvedtak) = iverksettSøknadsbehandlingOgMeldekortbehandling(
            tac = tac,
            fnr = fnr,
            saksbehandler = saksbehandlerMeldekortbehandling,
        ) ?: return null

        val (oppdatertSak, klagebehandling, _) = this.opprettKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            saksbehandler = saksbehandlerKlagebehandling,
            journalpostId = journalpostId,
            vedtakDetKlagesPå = meldekortvedtak.id,
            erKlagerPartISaken = erKlagerPartISaken,
            klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
            erKlagefristenOverholdt = erKlagefristenOverholdt,
            erUnntakForKlagefrist = erUnntakForKlagefrist,
            erKlagenSignert = erKlagenSignert,
            forventet = forventet,
        ) ?: return null

        val (sakMedVurdertKlage, vurdertKlage, vurdertKlageJson) = this.vurderKlagebehandling(
            tac = tac,
            sakId = oppdatertSak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            vurderingstype = Vurderingstype.OPPRETTHOLD,
            hjemler = listOf(KlagehjemmelDto.ARBEIDSMARKEDSLOVEN_17),
            begrunnelse = null,
            årsak = null,
        )!!

        return Tuple4(sakMedVurdertKlage, meldekortvedtak, vurdertKlage, vurdertKlageJson)
    }

    suspend fun ApplicationTestBuilder.iverksettMeldekortvedtakOgOpprettKlagebehandlingTilAvvisning(
        tac: TestApplicationContext,
        saksbehandlerMeldekortbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerMeldekortbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        fnr: Fnr = ObjectMother.gyldigFnr(),
        journalpostId: JournalpostId = JournalpostId("12345"),
        erKlagerPartISaken: Boolean = true,
        klagesDetPåKonkreteElementerIVedtaket: Boolean = false,
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        erKlagenSignert: Boolean = true,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple4<Sak, Meldekortvedtak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, _, _, meldekortedtak) = iverksettSøknadsbehandlingOgMeldekortbehandling(
            tac = tac,
            fnr = fnr,
            saksbehandler = saksbehandlerMeldekortbehandling,
        ) ?: return null

        val (oppdatertSak, klagebehandling, klagebehandlingJson) = this.opprettKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            saksbehandler = saksbehandlerKlagebehandling,
            journalpostId = journalpostId,
            vedtakDetKlagesPå = meldekortedtak.id,
            erKlagerPartISaken = erKlagerPartISaken,
            klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
            erKlagefristenOverholdt = erKlagefristenOverholdt,
            erUnntakForKlagefrist = erUnntakForKlagefrist,
            erKlagenSignert = erKlagenSignert,
            forventet = forventet,
        ) ?: return null

        return Tuple4(oppdatertSak, meldekortedtak, klagebehandling, klagebehandlingJson)
    }

    /** Forventer at det allerede finnes en sak. */
    suspend fun ApplicationTestBuilder.opprettKlagebehandlingForSakId(
        tac: TestApplicationContext,
        sakId: SakId,
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
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/klage",
            jwt = jwt,
            forventet = forventet,
            //language=JSON
            body =
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
            .apply {
                val bodyAsText = this.body

                if (statusCode != 200) return null
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
