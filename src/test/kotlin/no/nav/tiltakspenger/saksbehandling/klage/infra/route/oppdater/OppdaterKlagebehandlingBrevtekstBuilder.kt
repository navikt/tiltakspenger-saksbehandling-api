package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppdater

import arrow.core.Tuple4
import io.kotest.assertions.json.shouldEqualJson
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortvedtakOgOpprettKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortvedtakOgOpprettKlagebehandlingTilOpprettholdelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilOpprettholdelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.brev.oppdaterTekstTilBrev]
 */
interface OppdaterKlagebehandlingBrevtekstBuilder {
    /**
     * 1. Oppretter ny sak
     * 2. Starter klagebehandling til avvisning
     * 3. Oppdaterer brevtekst
     */
    suspend fun ApplicationTestBuilder.opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        brevtekst: List<TittelOgTekst> = listOf(
            TittelOgTekst(
                tittel = NonBlankString.create("Avvisning av klage"),
                tekst = NonBlankString.create("Din klage er dessverre avvist."),
            ),
        ),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
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
            forventet = forventet,
        )
    }

    suspend fun ApplicationTestBuilder.opprettSakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        brevtekst: List<TittelOgTekst> = listOf(
            TittelOgTekst(
                tittel = NonBlankString.create("Hva klagesaken gjelder"),
                tekst = NonBlankString.create("Vi viser til klage av 2025-01-01 på vedtak av 2025-01-01 der <kort om resultatet i vedtaket>"),
            ),
            TittelOgTekst(
                tittel = NonBlankString.create("Klagers anførsler"),
                tekst = NonBlankString.create("<saksbehandler fyller ut>"),
            ),
            TittelOgTekst(
                tittel = NonBlankString.create("Vurdering av klagen"),
                tekst = NonBlankString.create("<saksbehandler fyller ut>"),
            ),
        ),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple4<Sak, Rammevedtak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, rammevedtak, klagebehandling, _) = this.opprettSakOgKlagebehandlingTilOpprettholdelse(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        ) ?: return null
        val (oppdatertSak, oppdatertKlagebehandling, klagebehandlingJson) = oppdaterKlagebehandlingBrevtekstForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            brevtekst = brevtekst,
            forventet = forventet,
        )!!

        return Tuple4(oppdatertSak, rammevedtak, oppdatertKlagebehandling, klagebehandlingJson)
    }

    suspend fun ApplicationTestBuilder.iverksettMeldekortvedtakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandlerMeldekortbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerMeldekortbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        brevtekst: List<TittelOgTekst> = listOf(
            TittelOgTekst(
                tittel = NonBlankString.create("Hva klagesaken gjelder"),
                tekst = NonBlankString.create("Vi viser til klage av 2025-01-01 på vedtak av 2025-01-01 der <kort om resultatet i vedtaket>"),
            ),
            TittelOgTekst(
                tittel = NonBlankString.create("Klagers anførsler"),
                tekst = NonBlankString.create("<saksbehandler fyller ut>"),
            ),
            TittelOgTekst(
                tittel = NonBlankString.create("Vurdering av klagen"),
                tekst = NonBlankString.create("<saksbehandler fyller ut>"),
            ),
        ),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple4<Sak, Meldekortvedtak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, meldekortvedtak, klagebehandling, _) = this.iverksettMeldekortvedtakOgOpprettKlagebehandlingTilOpprettholdelse(
            tac = tac,
            saksbehandlerMeldekortbehandling = saksbehandlerMeldekortbehandling,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
            fnr = fnr,
        ) ?: return null

        val (oppdatertSak, oppdatertKlagebehandling, klagebehandlingJson) = oppdaterKlagebehandlingBrevtekstForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            brevtekst = brevtekst,
            forventet = forventet,
        )!!

        return Tuple4(oppdatertSak, meldekortvedtak, oppdatertKlagebehandling, klagebehandlingJson)
    }

    suspend fun ApplicationTestBuilder.iverksettMeldekortVedtakOgOppdaterKlagebehandlingTilAvvisningBrevtekst(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        brevtekst: List<TittelOgTekst> = listOf(
            TittelOgTekst(
                tittel = NonBlankString.create("Avvisning av klage"),
                tekst = NonBlankString.create("Din klage er dessverre avvist."),
            ),
        ),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple4<Sak, Meldekortvedtak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, meldekortvedtak, klagebehandling, _) = this.iverksettMeldekortvedtakOgOpprettKlagebehandlingTilAvvisning(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandler,
            fnr = fnr,
        ) ?: return null

        val (oppdatertSak, oppdatertKlagebehandling, klagebehandlingJson) = oppdaterKlagebehandlingBrevtekstForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            brevtekst = brevtekst,
            forventet = forventet,
        )!!

        return Tuple4(oppdatertSak, meldekortvedtak, oppdatertKlagebehandling, klagebehandlingJson)
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
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        val tekstTilVedtaksbrevListe = brevtekst.joinToString(separator = ",") {
            """
            {
                "tittel": "${it.tittel.value}",
                "tekst": "${it.tekst.value}"
            }
            """.trimIndent()
        }
        defaultRequestWithAssertions(
            HttpMethod.PUT,
            "/sak/$sakId/klage/$klagebehandlingId/brevtekst",
            jwt = jwt,
            forventet = forventet,
            //language=JSON
            body = """
                {
                    "tekstTilVedtaksbrev": [$tekstTilVedtaksbrevListe]
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
