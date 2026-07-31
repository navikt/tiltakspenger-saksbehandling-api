package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.opprett

import arrow.core.Tuple5
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldekortbehandlingDTOV2Json
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.opprettMeldekortbehandlingRoute]
 */
interface OpprettMeldekortbehandlingBuilder {

    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
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
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        medJsonBody: ((jsonBody: String) -> Unit)? = null,
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortUnderBehandling, MeldekortbehandlingDTOV2Json>? {
        val (sak, søknad, rammevedtak, _) = iverksettSøknadsbehandling(
            tac = tac,
            fnr = fnr,
            saksbehandler = saksbehandler,
            tiltaksdeltakelse = tiltaksdeltakelse,
            innvilgelsesperioder = innvilgelsesperioder,
        )
        val førsteMeldeperiode = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede.first()
        val (sakMedMeldekortbehandling, meldekortbehandling, opprettMeldekortbehandlingResponse) = opprettMeldekortbehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            kjedeId = førsteMeldeperiode.kjedeId,
            saksbehandler = saksbehandler,
            forventet = forventet,
            medJsonBody = medJsonBody,
        ) ?: return null
        return Tuple5(
            sakMedMeldekortbehandling,
            søknad,
            rammevedtak,
            meldekortbehandling,
            opprettMeldekortbehandlingResponse,
        )
    }

    /** Forventer at det det finnes en sak med en meldeperiode som gir rett til tiltakspenger */
    suspend fun ApplicationTestBuilder.opprettMeldekortbehandlingForSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        kjedeId: MeldeperiodeKjedeId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        medJsonBody: ((jsonBody: String) -> Unit)? = null,
    ): Triple<Sak, MeldekortUnderBehandling, MeldekortbehandlingDTOV2Json>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        val kjedeId = "${kjedeId.fraOgMed}%2F${kjedeId.tilOgMed}"
        val response = defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/meldeperiode/$kjedeId/opprettBehandling",
            jwt = jwt,
            forventet = forventet,
            body =
            """
                {
                "v2": true
                }
            """.trimIndent(),
        )
        val bodyAsText = response.body
        if (medJsonBody != null) {
            medJsonBody(bodyAsText)
        }

        if (response.statusCode != 200) {
            return null
        }

        val jsonObject: MeldekortbehandlingDTOV2Json = objectMapper.readTree(bodyAsText)
        val meldekortbehandlingId = MeldekortId.fromString(jsonObject.get("id").asString())

        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        val meldekortbehandling =
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandlingId) as MeldekortUnderBehandling

        return Triple(
            oppdatertSak,
            meldekortbehandling,
            jsonObject,
        )
    }

    /**
     * Ny variant som støtter flere meldeperiodekjeder i samme behandling.
     * Kjedene sendes i request-body i stedet for i path.
     */
    suspend fun ApplicationTestBuilder.opprettMeldekortbehandlingForSakIdV2(
        tac: TestApplicationContext,
        sakId: SakId,
        kjedeIder: List<MeldeperiodeKjedeId>,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        medJsonBody: ((jsonBody: String) -> Unit)? = null,
    ): Triple<Sak, MeldekortUnderBehandling, MeldekortbehandlingDTOV2Json>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        val response = defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/meldekort/opprettBehandling",
            jwt = jwt,
            forventet = forventet,
            body = """
                {
                  "kjedeIder": ${kjedeIder.joinToString(prefix = "[", postfix = "]") { "\"${it.verdi}\"" }}
                }
            """.trimIndent(),
        )
        val bodyAsText = response.body
        if (medJsonBody != null) {
            medJsonBody(bodyAsText)
        }

        if (response.statusCode != 200) {
            return null
        }

        val jsonObject: MeldekortbehandlingDTOV2Json = objectMapper.readTree(bodyAsText)
        val meldekortbehandlingId = MeldekortId.fromString(jsonObject.get("id").asString())

        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        val meldekortbehandling =
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandlingId) as MeldekortUnderBehandling

        return Triple(
            oppdatertSak,
            meldekortbehandling,
            jsonObject,
        )
    }
}
