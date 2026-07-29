package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.settPåVent

import arrow.core.Tuple5
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBeslutterTarBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.settMeldekortbehandlingPåVentRoute]
 */
interface SettMeldekortbehandlingPåVentBuilder {

    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgSettPåVent(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        begrunnelse: String = "Begrunnelse for å sette meldekortbehandling på vent",
        frist: LocalDate? = 1.januar(2026),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortUnderBehandling, SakDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling, meldekortbehandling, _) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
            tac = tac,
            saksbehandler = saksbehandler,
        ) ?: return null

        val (oppdatertSak, oppdatertMeldekortbehandling, json) = settMeldekortbehandlingPåVent(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortbehandling.id,
            saksbehandlerEllerBeslutter = saksbehandler,
            begrunnelse = begrunnelse,
            frist = frist,
            forventet = forventet,
        ) ?: return null

        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            oppdatertMeldekortbehandling as MeldekortUnderBehandling,
            json,
        )
    }

    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingOgSettPåVent(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        begrunnelse: String = "Begrunnelse for å sette meldekortbehandling på vent",
        frist: LocalDate? = 1.januar(2026),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortbehandlingManuell, SakDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling, meldekortbehandling, _) = iverksettSøknadsbehandlingOgBeslutterTarBehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
        ) ?: return null

        val (oppdatertSak, oppdatertMeldekortbehandling, json) = settMeldekortbehandlingPåVent(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortbehandling.id,
            saksbehandlerEllerBeslutter = beslutter,
            begrunnelse = begrunnelse,
            frist = frist,
            forventet = forventet,
        ) ?: return null

        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            oppdatertMeldekortbehandling as MeldekortbehandlingManuell,
            json,
        )
    }

    /**
     * Forventer at meldekortbehandlingen er i status UNDER_BEHANDLING eller UNDER_BESLUTNING.
     */
    suspend fun ApplicationTestBuilder.settMeldekortbehandlingPåVent(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandlerEllerBeslutter: Saksbehandler = ObjectMother.saksbehandlerOgBeslutter("saksbehandlerOgBeslutter"),
        begrunnelse: String = "Begrunnelse for å sette meldekortbehandling på vent",
        frist: LocalDate? = 1.januar(2026),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Meldekortbehandling, SakDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandlerEllerBeslutter,
        )
        tac.leggTilBruker(jwt, saksbehandlerEllerBeslutter)
        val fristJson = frist?.let { "\"$it\"" } ?: "null"
        defaultRequestWithAssertions(
            HttpMethod.PATCH,
            "/sak/$sakId/meldekort/$meldekortId/vent",
            jwt = jwt,
            forventet = forventet,
            body =
            """
                    {
                      "begrunnelse": "$begrunnelse",
                      "frist": $fristJson
                    }
            """.trimIndent(),
        ).apply {
            val bodyAsText = body
            if (statusCode != 200) return null
            val jsonObject: SakDTOJson = objectMapper.readTree(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                oppdatertSak.hentMeldekortbehandling(meldekortId)!!,
                jsonObject,
            )
        }
    }
}
