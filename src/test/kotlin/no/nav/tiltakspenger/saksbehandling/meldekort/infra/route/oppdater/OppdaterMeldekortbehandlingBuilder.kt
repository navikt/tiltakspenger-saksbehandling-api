package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.oppdater

import arrow.core.Tuple5
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
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
import no.nav.tiltakspenger.saksbehandling.felles.erHelg
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldekortBehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldeperiodeKjedeDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus.IKKE_TILTAKSDAG
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.tilMeldekortDager
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject
import java.time.LocalDate

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.oppdaterMeldekortBehandlingRoute]
 * Dto: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldeperiodeKjedeDTO]
 */
interface OppdaterMeldekortbehandlingBuilder {

    /**
     * 1. Oppretter sak, søknad og iverksetter en innvilget søknadsbehandling.
     * 2. Oppretter meldekortbehandling for sakens første meldeperiode. (UNDER_BEHANDLING)
     * 3. Oppdaterer meldekortbehandlingen. (UNDER_BEHANDLING)
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        begrunnelse: String? = null,
        tekstTilVedtaksbrev: String? = null,
        dager: List<Pair<LocalDate, MeldekortDagStatus>>? = null,
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
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortUnderBehandling, MeldeperiodeKjedeDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling, meldekortUnderBehandling, _) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            vedtaksperiode = vedtaksperiode,
            tiltaksdeltakelse = tiltaksdeltakelse,
            innvilgelsesperioder = innvilgelsesperioder,
        ) ?: return null

        val (oppdatertSak, oppdatertMeldekortbehandling, json) = oppdaterMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortUnderBehandling.id,
            saksbehandler = saksbehandler,
            begrunnelse = begrunnelse,
            tekstTilVedtaksbrev = tekstTilVedtaksbrev,
            dager = dager,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            oppdatertMeldekortbehandling,
            json,
        )
    }

    /**
     * Forventer at meldekortbehandlingen er i status UNDER_BEHANDLING.
     * @param dager Dersom null, fylles dager basert på sakens meldeperioden.
     */
    suspend fun ApplicationTestBuilder.opprettOgOppdaterMeldekortbehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        kjedeId: MeldeperiodeKjedeId = tac.sakContext.sakRepo.hentForSakId(sakId)!!.meldeperiodeKjeder.sisteMeldeperiodePerKjede.first().kjedeId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        begrunnelse: String? = null,
        tekstTilVedtaksbrev: String? = null,
        dager: List<Pair<LocalDate, MeldekortDagStatus>>? = null,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, MeldekortUnderBehandling, MeldeperiodeKjedeDTOJson>? {
        val (_, opprettetMeldekortbehandling, _) = opprettMeldekortbehandlingForSakId(
            tac = tac,
            sakId = sakId,
            kjedeId = kjedeId,
            saksbehandler = saksbehandler,
        ) ?: return null
        return oppdaterMeldekortbehandling(
            tac = tac,
            sakId = sakId,
            meldekortId = opprettetMeldekortbehandling.id,
            saksbehandler = saksbehandler,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        )
    }

    /**
     * Forventer at meldekortbehandlingen er i status UNDER_BEHANDLING.
     * @param dager Dersom null, fylles dager basert på sakens meldeperioden.
     */
    suspend fun ApplicationTestBuilder.oppdaterMeldekortbehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        begrunnelse: String? = null,
        tekstTilVedtaksbrev: String? = null,
        dager: List<Pair<LocalDate, MeldekortDagStatus>>? = null,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, MeldekortUnderBehandling, MeldeperiodeKjedeDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/meldekort/$meldekortId/oppdater")
            },
            jwt = jwt,
        ) {
            val dagerIBody = buildDagerBody(tac = tac, sakId = sakId, meldekortId = meldekortId, dager = dager)
            this.setBody(
                """
                    {
                    "begrunnelse":${if (begrunnelse != null) "\"$begrunnelse\"" else null},
                    "tekstTilVedtaksbrev":${if (tekstTilVedtaksbrev != null) "\"$tekstTilVedtaksbrev\"" else null},
                    "dager":$dagerIBody
                    }
                """.trimIndent(),
            )
        }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe forventetStatus
                if (forventetJsonBody != null) bodyAsText.shouldEqualJson(forventetJsonBody)
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
            }
            if (status != HttpStatusCode.OK) return null
            val jsonObject: MeldekortBehandlingDTOJson = JSONObject(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                oppdatertSak.hentMeldekortBehandling(meldekortId) as MeldekortUnderBehandling,
                jsonObject,
            )
        }
    }

    fun buildDagerBody(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        dager: List<Pair<LocalDate, MeldekortDagStatus>>?,
    ): String? {
        return (dager ?: defaultDagerBasertPåMeldeperiode(tac, sakId, meldekortId)).let {
            it.joinToString(
                prefix = "[",
                postfix = "]",
                separator = ",",
            ) { (dato, status) ->
                """
                    {
                        "dato":"$dato",
                        "status":"$status"
                    }
                """.trimIndent()
            }
        }
    }

    /**
     * TODO jah: Bør kunne styre om man skal kunne melde helg.
     */
    fun defaultDagerBasertPåMeldeperiode(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
    ): List<Pair<LocalDate, MeldekortDagStatus>> {
        val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        val meldekortBehandling = sak.hentMeldekortBehandling(meldekortId) as MeldekortUnderBehandling
        val meldeperiode = meldekortBehandling.meldeperiode
        val antallDager = meldeperiode.maksAntallDagerForMeldeperiode
        val dager = meldeperiode.tilMeldekortDager()

        // Resten av dagene er IKKE_RETT_TIL_TILTAKSPENGER og kan aldri overstyres.
        val tilgjengeligeDager = dager.filter { it.status == MeldekortDagStatus.IKKE_BESVART }

        val (uke1, uke2) = tilgjengeligeDager.partition { it.dato < meldeperiode.periode.fraOgMed.plusDays(7) }

        val (uke1Ukedager, uke1Helgedager) = uke1.partition { !it.dato.erHelg() }
        val (uke2Ukedager, uke2Helgedager) = uke2.partition { !it.dato.erHelg() }

        val dagerPerUke = antallDager / 2
        val restDager = antallDager % 2

        val uke1Deltakelse = (uke1Ukedager + uke1Helgedager).take(dagerPerUke + restDager)
        val uke2Deltakelse = (uke2Ukedager + uke2Helgedager).take(dagerPerUke)

        val dagerMedDeltakelse = (uke1Deltakelse + uke2Deltakelse).map { it.dato }.toSet()

        return dager.map { dag ->
            when {
                dag.status == IKKE_RETT_TIL_TILTAKSPENGER -> dag.dato to IKKE_RETT_TIL_TILTAKSPENGER

                dag.dato in dagerMedDeltakelse -> dag.dato to DELTATT_UTEN_LØNN_I_TILTAKET

                else -> dag.dato to IKKE_TILTAKSDAG
            }
        }
    }
}
