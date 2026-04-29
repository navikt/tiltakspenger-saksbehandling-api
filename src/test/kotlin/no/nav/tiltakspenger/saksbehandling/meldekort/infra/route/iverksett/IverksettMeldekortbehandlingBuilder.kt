package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.iverksett

import arrow.core.Tuple4
import arrow.core.Tuple6
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldeperiodeKjedeDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO.OppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBeslutterTarBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgBesluttertarMeldekortbehanding
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.iverksettMeldekortRoute]
 * Dto: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldeperiodeKjedeDTO]
 */
interface IverksettMeldekortbehandlingBuilder {

    /**
     * 1. Iverksetter en søknadsbehandling
     * 2. Oppretter en meldekortbehandling
     * 3. Oppdaterer behandlingen
     * 4. Sender til beslutning
     * 5. Beslutter tar behandlingen.
     * 6. Beslutter iverksetter.
     * 7. Trigger eventuelle utbetalinger og oppdaterer utbetalingsstatus.
     *
     * Merk at den ikke kjører visse jobber som journalføring, distribuering, oppgaveopprettelse, datadeling o.l.
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        vedtaksperiode: Periode = 1.til(10.april(2025)),
        tiltaksdeltakelse: Tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
            fom = vedtaksperiode.fraOgMed,
            tom = vedtaksperiode.tilOgMed,
        ),
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(
            periode = vedtaksperiode,
            valgtTiltaksdeltakelse = tiltaksdeltakelse,
            antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
        ),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Tuple6<Sak, Søknad, Rammevedtak, Meldekortvedtak, MeldekortbehandlingManuell, MeldeperiodeKjedeDTOJson>? {
        val (sakMedMeldekortbehandlingUnderBeslutning, søknad, rammevedtakSøknadsbehandling, meldekortbehandlingUnderBeslutning) = iverksettSøknadsbehandlingOgBeslutterTarBehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            vedtaksperiode = vedtaksperiode,
            tiltaksdeltakelse = tiltaksdeltakelse,
            innvilgelsesperioder = innvilgelsesperioder,
        ) ?: return null
        val (oppdatertSak, meldekortvedtak, oppdatertMeldekort, json) = iverksettMeldekortbehandling(
            tac = tac,
            sakId = sakMedMeldekortbehandlingUnderBeslutning.id,
            meldekortId = meldekortbehandlingUnderBeslutning.id,
            beslutter = beslutter,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        return Tuple6(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            meldekortvedtak,
            oppdatertMeldekort,
            json,
        )
    }

    /**
     * 1. Oppretter en meldekortbehandling
     * 2. Oppdaterer behandlingen
     * 3. Sender til beslutning
     * 4. Beslutter tar behandlingen.
     * 5. Beslutter iverksetter.
     * 6. Trigger eventuelle utbetalinger og oppdaterer utbetalingsstatus.
     *
     * Merk at den ikke kjører visse jobber som journalføring, distribuering, oppgaveopprettelse, datadeling o.l.
     */
    suspend fun ApplicationTestBuilder.opprettOgIverksettMeldekortbehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        kjedeId: MeldeperiodeKjedeId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        begrunnelse: String? = null,
        tekstTilVedtaksbrev: String? = null,
        meldeperioder: List<OppdatertMeldeperiodeDTO>? = null,
        skalSendeVedtaksbrev: Boolean = true,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Tuple4<Sak, Meldekortvedtak, MeldekortbehandlingManuell, MeldeperiodeKjedeDTOJson>? {
        val (sak, meldekortbehandling) = opprettOgBesluttertarMeldekortbehanding(
            tac = tac,
            sakId = sakId,
            kjedeId = kjedeId,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            begrunnelse = begrunnelse,
            tekstTilVedtaksbrev = tekstTilVedtaksbrev,
            meldeperioder = meldeperioder,
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
        ) ?: return null
        return iverksettMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortbehandling.id,
            beslutter = beslutter,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        )
    }

    /**
     * Forventer at det allerede finnes en sak og meldekortbehandling i status UNDER_BESLUTNING
     */
    suspend fun ApplicationTestBuilder.iverksettMeldekortbehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Tuple4<Sak, Meldekortvedtak, MeldekortbehandlingManuell, MeldeperiodeKjedeDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = beslutter,
        )
        tac.leggTilBruker(jwt, beslutter)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/meldekort/$meldekortId/iverksett")
            },
            jwt = jwt,
        ).apply {
            val bodyAsText = bodyAsText()
            withClue(
                "Response details:\n" +
                    "Status: ${this.status}\n" +
                    "Content-Type: ${this.contentType()}\n" +
                    "Body: $bodyAsText}\n",
            ) {
                if (forventetStatus != null) status shouldBe forventetStatus
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                if (forventetJsonBody != null) bodyAsText.shouldEqualJson(forventetJsonBody)
            }
            if (status != HttpStatusCode.OK) return null
            val jsonObject: MeldeperiodeKjedeDTOJson = JSONObject(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val meldekortbehandling = oppdatertSak.hentMeldekortbehandling(meldekortId) as MeldekortbehandlingManuell
            val meldekortvedtak = oppdatertSak.meldekortvedtaksliste.single { it.behandlingId == meldekortbehandling.id }
            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.GODKJENT

            tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()
            tac.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus()

            return Tuple4(
                oppdatertSak,
                meldekortvedtak,
                meldekortbehandling,
                jsonObject,
            )
        }
    }
}
