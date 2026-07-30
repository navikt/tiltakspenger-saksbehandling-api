package no.nav.tiltakspenger.saksbehandling.klage.infra.route.vurder

import arrow.core.Tuple5
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagehjemmelDto
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettKlagebehandlingTilVurdering
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

/**
 * Route: [vurderKlagebehandlingRoute]
 */
interface VurderKlagebehandlingBuilder {
    /** 1. Oppretter ny sak
     *  2. Starter klagebehandling med godkjente formkrav
     *  3. Vurderer klagebehandling (default til omgjøring)
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgVurderKlagebehandling(
        tac: TestApplicationContext,
        saksbehandlerSøknadsbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSøknadsbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        journalpostId: JournalpostId = JournalpostId("12345"),
        erKlagerPartISaken: Boolean = true,
        klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        erKlagenSignert: Boolean = true,
        vurderingstype: Vurderingstype = Vurderingstype.OMGJØR,
        begrunnelse: Begrunnelse? = Begrunnelse.createOrThrow("Begrunnelse for omgjøring"),
        årsak: KlageOmgjøringsårsak? = KlageOmgjøringsårsak.PROSESSUELL_FEIL,
        hjemler: List<KlagehjemmelDto>? = null,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple5<Sak, Søknad, Rammevedtak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling, klagebehandling, _) = this.iverksettSøknadsbehandlingOgOpprettKlagebehandlingTilVurdering(
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
        val (oppdatertSak, oppdatertKlagebehandling, json) = vurderKlagebehandling(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            begrunnelse = begrunnelse,
            årsak = årsak,
            hjemler = hjemler,
            vurderingstype = vurderingstype,
            forventet = forventet,
        ) ?: return null
        return Tuple5(oppdatertSak, søknad, rammevedtakSøknadsbehandling, oppdatertKlagebehandling, json)
    }

    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgVurderKlagebehandlingTilOpprettholdelse(
        tac: TestApplicationContext,
        saksbehandlerSøknadsbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSøknadsbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        hjemler: List<KlagehjemmelDto>? = listOf(KlagehjemmelDto.ARBEIDSMARKEDSLOVEN_17),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple5<Sak, Søknad, Rammevedtak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling, klagebehandling, _) = this.iverksettSøknadsbehandlingOgOpprettKlagebehandlingTilVurdering(
            tac = tac,
            saksbehandlerSøknadsbehandling = saksbehandlerSøknadsbehandling,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
        ) ?: return null
        val (oppdatertSak, oppdatertKlagebehandling, json) = vurderKlagebehandling(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            begrunnelse = null,
            årsak = null,
            hjemler = hjemler,
            vurderingstype = Vurderingstype.OPPRETTHOLD,
            forventet = forventet,
        ) ?: return null
        return Tuple5(oppdatertSak, søknad, rammevedtakSøknadsbehandling, oppdatertKlagebehandling, json)
    }

    suspend fun ApplicationTestBuilder.vurderKlagebehandlingOpprettholdelseForSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        hjemler: List<KlagehjemmelDto> = listOf(KlagehjemmelDto.ARBEIDSMARKEDSLOVEN_17),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        return vurderKlagebehandling(
            tac = tac,
            sakId = sakId,
            klagebehandlingId = klagebehandlingId,
            saksbehandler = saksbehandler,
            vurderingstype = Vurderingstype.OPPRETTHOLD,
            begrunnelse = null,
            årsak = null,
            hjemler = hjemler,
        )
    }

    /** Forventer at det allerede finnes en sak. */
    suspend fun ApplicationTestBuilder.vurderKlagebehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        vurderingstype: Vurderingstype,
        begrunnelse: Begrunnelse?,
        årsak: KlageOmgjøringsårsak?,
        hjemler: List<KlagehjemmelDto>?,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.PATCH,
            "/sak/$sakId/klage/$klagebehandlingId/vurder",
            jwt = jwt,
            forventet = forventet,
            //language=JSON
            body =
            """
                {
                    "vurderingstype": "$vurderingstype",
                    "begrunnelse": ${begrunnelse?.let { "\"${it.verdi}\"" }},
                    "årsak": ${årsak?.let { "\"${it.name}\"" }},
                    "hjemler": ${hjemler?.joinToString(",") { "\"${it}\"" }?.let { "[$it]" }}
                }
            """.trimIndent(),
        ).apply {
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
