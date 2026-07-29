package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.InnvilgelsesperiodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilHjemmelForOpphørDTO
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
        behandlingId: RammebehandlingId,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        skalSendeVedtaksbrev: Boolean = true,
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "resultat": "INNVILGELSE",
              ${
            innvilgelseJson(
                innvilgelsesperioder.tilDTOMedDeltakelseFra(tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)),
                barnetillegg,
                begrunnelseVilkårsvurdering,
                fritekstTilVedtaksbrev,
                skalSendeVedtaksbrev,
            )
        }
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventet = forventet,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterSøknadsbehandlingIkkeValgt(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
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
            forventet = forventet,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterSøknadsbehandlingAvslag(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        avslagsgrunner: Set<Avslagsgrunnlag> = setOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        skalSendeVedtaksbrev: Boolean = true,
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "resultat": "AVSLAG",
              "avslagsgrunner": [${avslagsgrunner.joinToString(",") { it.toString().medQuotes() }}],
              "begrunnelseVilkårsvurdering": ${begrunnelseVilkårsvurdering?.medQuotes()},
              "fritekstTilVedtaksbrev": ${fritekstTilVedtaksbrev?.medQuotes()},
              "skalSendeVedtaksbrev" : $skalSendeVedtaksbrev
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventet = forventet,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterRevurderingInnvilgelse(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        skalSendeVedtaksbrev: Boolean = true,
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "resultat": "REVURDERING_INNVILGELSE",
              ${
            innvilgelseJson(
                innvilgelsesperioder.tilDTOMedDeltakelseFra(tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)),
                barnetillegg,
                begrunnelseVilkårsvurdering,
                fritekstTilVedtaksbrev,
                skalSendeVedtaksbrev,
            )
        }
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventet = forventet,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterOmgjøringInnvilgelse(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        vedtaksperiode: Periode,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        skalSendeVedtaksbrev: Boolean = true,
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "resultat": "OMGJØRING",
              "vedtaksperiode": ${serialize(vedtaksperiode.toDTO())},
              ${
            innvilgelseJson(
                innvilgelsesperioder.tilDTOMedDeltakelseFra(tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)),
                barnetillegg,
                begrunnelseVilkårsvurdering,
                fritekstTilVedtaksbrev,
                skalSendeVedtaksbrev,
            )
        }
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventet = forventet,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterOmgjøringOpphør(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        vedtaksperiode: Periode,
        valgteHjemler: Set<HjemmelForOpphør> = setOf(HjemmelForOpphør.DeltarIkkePåArbeidsmarkedstiltak),
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        skalSendeVedtaksbrev: Boolean = true,
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "resultat": "OMGJØRING_OPPHØR",
              "vedtaksperiode": ${serialize(vedtaksperiode.toDTO())},
              "valgteHjemler": [${valgteHjemler.joinToString(",") { it.tilHjemmelForOpphørDTO().toString().medQuotes() }}],
              "begrunnelseVilkårsvurdering": ${begrunnelseVilkårsvurdering?.medQuotes()},
              "fritekstTilVedtaksbrev": ${fritekstTilVedtaksbrev?.medQuotes()},
                "skalSendeVedtaksbrev" : $skalSendeVedtaksbrev
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventet = forventet,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterOmgjøringIkkeValgt(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
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
            forventet = forventet,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterRevurderingStans(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        stansFraOgMed: LocalDate? = null,
        harValgtStansFraFørsteDagSomGirRett: Boolean = stansFraOgMed == null,
        valgteHjemler: Set<HjemmelForStans> = setOf(HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        skalSendeVedtaksbrev: Boolean = true,
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "begrunnelseVilkårsvurdering": ${begrunnelseVilkårsvurdering?.medQuotes()},
              "fritekstTilVedtaksbrev": ${fritekstTilVedtaksbrev?.medQuotes()},
              "valgteHjemler": [${valgteHjemler.joinToString(",") { it.tilDTO().toString().medQuotes() }}],
              "harValgtStansFraFørsteDagSomGirRett": $harValgtStansFraFørsteDagSomGirRett,
              "stansFraOgMed": ${stansFraOgMed?.toString()?.medQuotes()},
              "resultat": "STANS",
                "skalSendeVedtaksbrev" : $skalSendeVedtaksbrev
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventet = forventet,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterBehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        body: String,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Triple<Sak, Rammebehandling, String> {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/behandling/$behandlingId/oppdater",
            jwt = jwt,
            forventet = forventet,
            body = body,
        ).apply {
            val bodyAsText = this.body
            val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val behandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
            return Triple(sak, behandling, bodyAsText)
        }
    }

    /**
     * Bytter ut ukjente deltakelse-id-er i innvilgelsesperiodene med behandlingens faktiske.
     * Kallstedene uttrykker hvilke perioder som innvilges; deltakelsen eies av flyten og har unik id per test.
     * Jobber på DTO-nivå slik at serverens validering av periodene fortsatt nås av tester som tester den.
     */
    private fun Innvilgelsesperioder.tilDTOMedDeltakelseFra(behandling: Rammebehandling): List<InnvilgelsesperiodeDTO> {
        val kjenteDeltakelser = behandling.saksopplysninger.tiltaksdeltakelser.value
        return tilDTO().map { dto ->
            if (kjenteDeltakelser.any { it.internDeltakelseId.toString() == dto.internDeltakelseId }) {
                dto
            } else {
                val dekkende = kjenteDeltakelser.firstOrNull { deltakelse ->
                    deltakelse.deltakelseFraOgMed != null &&
                        deltakelse.deltakelseTilOgMed != null &&
                        Periode(deltakelse.deltakelseFraOgMed, deltakelse.deltakelseTilOgMed).inneholderHele(dto.periode.toDomain())
                }
                val erstatning = dekkende ?: kjenteDeltakelser.firstOrNull()
                // Uten kjente deltakelser lar vi dto-en stå, slik at serveren svarer med sin egen feil.
                if (erstatning == null) dto else dto.copy(internDeltakelseId = erstatning.internDeltakelseId.toString())
            }
        }
    }

    private fun innvilgelseJson(
        innvilgelsesperioder: List<InnvilgelsesperiodeDTO>,
        barnetillegg: Barnetillegg,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        skalSendeVedtaksbrev: Boolean = true,
    ): String {
        return """            
            "begrunnelseVilkårsvurdering": ${begrunnelseVilkårsvurdering?.medQuotes()},
            "fritekstTilVedtaksbrev": ${fritekstTilVedtaksbrev?.medQuotes()},
            "innvilgelsesperioder": [${
            innvilgelsesperioder.joinToString(",") {
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
        },
        "skalSendeVedtaksbrev": $skalSendeVedtaksbrev
        """.trimIndent()
    }
}
