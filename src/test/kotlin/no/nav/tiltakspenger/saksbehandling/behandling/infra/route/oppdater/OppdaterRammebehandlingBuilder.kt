package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

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
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.medQuotes
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.intellij.lang.annotations.Language
import java.time.LocalDate

interface OppdaterRammebehandlingBuilder {

    suspend fun ApplicationTestBuilder.oppdaterSøknadsbehandlingInnvilgelse(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder),
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "resultat": "INNVILGELSE",
              ${
            innvilgelseJson(
                innvilgelsesperioder,
                barnetillegg,
                begrunnelseVilkårsvurdering,
                fritekstTilVedtaksbrev,
            )
        }
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventetStatus = forventetStatus,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterSøknadsbehandlingIkkeValgt(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "resultat": "IKKE_VALGT",
              "begrunnelseVilkårsvurdering": ${begrunnelseVilkårsvurdering?.medQuotes()},
              "fritekstTilVedtaksbrev": ${fritekstTilVedtaksbrev?.medQuotes()}
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventetStatus = forventetStatus,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterSøknadsbehandlingAvslag(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        avslagsgrunner: Set<Avslagsgrunnlag> = setOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak),
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "resultat": "AVSLAG",
              "avslagsgrunner": [${avslagsgrunner.joinToString(",") { it.toString().medQuotes() }}],
              "begrunnelseVilkårsvurdering": ${begrunnelseVilkårsvurdering?.medQuotes()},
              "fritekstTilVedtaksbrev": ${fritekstTilVedtaksbrev?.medQuotes()}
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventetStatus = forventetStatus,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterRevurderingInnvilgelse(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder),
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "resultat": "REVURDERING_INNVILGELSE",
              ${
            innvilgelseJson(
                innvilgelsesperioder,
                barnetillegg,
                begrunnelseVilkårsvurdering,
                fritekstTilVedtaksbrev,
            )
        }
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventetStatus = forventetStatus,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterOmgjøringInnvilgelse(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        vedtaksperiode: Periode,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "resultat": "OMGJØRING",
              "vedtaksperiode": ${serialize(vedtaksperiode.toDTO())},
              ${
            innvilgelseJson(
                innvilgelsesperioder,
                barnetillegg,
                begrunnelseVilkårsvurdering,
                fritekstTilVedtaksbrev,
            )
        }
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventetStatus = forventetStatus,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterOmgjøringOpphør(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        vedtaksperiode: Periode,
        valgteHjemler: Set<HjemmelForStans> = setOf(HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "resultat": "OMGJØRING_OPPHØR",
              "vedtaksperiode": ${serialize(vedtaksperiode.toDTO())},
              "valgteHjemler": [${valgteHjemler.joinToString(",") { it.tilDTO().toString().medQuotes() }}],
              "begrunnelseVilkårsvurdering": ${begrunnelseVilkårsvurdering?.medQuotes()},
              "fritekstTilVedtaksbrev": ${fritekstTilVedtaksbrev?.medQuotes()}
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventetStatus = forventetStatus,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterOmgjøringIkkeValgt(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "resultat": "OMGJØRING_IKKE_VALGT"
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventetStatus = forventetStatus,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterRevurderingStans(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        stansFraOgMed: LocalDate? = null,
        harValgtStansFraFørsteDagSomGirRett: Boolean = stansFraOgMed == null,
        valgteHjemler: Set<HjemmelForStans> = setOf(HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "begrunnelseVilkårsvurdering": ${begrunnelseVilkårsvurdering?.medQuotes()},
              "fritekstTilVedtaksbrev": ${fritekstTilVedtaksbrev?.medQuotes()},
              "valgteHjemler": [${valgteHjemler.joinToString(",") { it.tilDTO().toString().medQuotes() }}],
              "harValgtStansFraFørsteDagSomGirRett": $harValgtStansFraFørsteDagSomGirRett,
              "stansFraOgMed": ${stansFraOgMed?.toString()?.medQuotes()},
              "resultat": "STANS"
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventetStatus = forventetStatus,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterBehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        body: String,
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Triple<Sak, Rammebehandling, String> {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/behandling/$behandlingId/oppdater")
            },
            jwt = jwt,
        ) {
            setBody(body)
        }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe forventetStatus
            }
            val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val behandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
            return Triple(sak, behandling, bodyAsText)
        }
    }

    private fun innvilgelseJson(
        innvilgelsesperioder: Innvilgelsesperioder,
        barnetillegg: Barnetillegg,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
    ): String {
        return """            
            "begrunnelseVilkårsvurdering": ${begrunnelseVilkårsvurdering?.medQuotes()},
            "fritekstTilVedtaksbrev": ${fritekstTilVedtaksbrev?.medQuotes()},
            "innvilgelsesperioder": [${
            innvilgelsesperioder.tilDTO().joinToString(",") {
                """
                            {
                                "periode": ${serialize(it.periode)},
                                "antallDagerPerMeldeperiode": ${it.antallDagerPerMeldeperiode},
                                "internDeltakelseId": ${it.internDeltakelseId.medQuotes()}
                            }                  
                """.trimIndent()
            }
        }],
            "barnetillegg": ${
            barnetillegg.toBarnetilleggDTO().let { bt ->
                """
                        {
                            "begrunnelse": ${bt.begrunnelse?.medQuotes()},
                            "perioder": [${
                    bt.perioder.joinToString(",") {
                        """
                                    {
                                        "periode": ${serialize(it.periode)},
                                        "antallBarn": ${it.antallBarn} 
                                    }
                        """.trimIndent()
                    }
                }]                                                  
                        }
                """.trimIndent()
            }
        }
        """.trimIndent()
    }
}
