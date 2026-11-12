package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.tiltaksdeltakelsenSomForlenges
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.OppdaterBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.HjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.InnvilgelsesperioderDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForAvslagDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.LocalDate

/**
 * Gjelder for innvilgelse, avslag og stans.
 */
interface ForhåndsvisRammevedtaksbrevTestbuilder {

    /** Forventer at det allerede finnes en sak, søknad og behandling. */
    suspend fun ApplicationTestBuilder.forhåndsvisVedtaksbrevForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fritekstTilVedtaksbrev: String = "some_tekst",
        vedtaksperiode: Periode? = null,
        stansFraOgMed: LocalDate? = null,
        valgteHjemler: List<HjemmelForStansDTO>? = null,
        barnetillegg: OppdaterBarnetilleggDTO? = null,
        // Overstyrer barnetillegget med rå json, f.eks. med antall barn per periode for barnetillegg lagret som V1.
        barnetilleggJson: String? = barnetillegg?.let { serialize(it) },
        resultat: RammebehandlingResultatTypeDTO,
        avslagsgrunner: List<ValgtHjemmelForAvslagDTO>? = null,
        innvilgelsesperioder: InnvilgelsesperioderDTO? = null,
        tilOgMed: LocalDate? = null,
        internDeltakelseId: TiltaksdeltakerId? = if (resultat == RammebehandlingResultatTypeDTO.FORLENGELSE) {
            tac.sakContext.sakRepo.hentForSakId(sakId)!!.tiltaksdeltakelsenSomForlenges(behandlingId)
        } else {
            null
        },
    ): Triple<Sak, Rammebehandling, String> {
        if (resultat in listOf(
                RammebehandlingResultatTypeDTO.INNVILGELSE,
                RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE,
                RammebehandlingResultatTypeDTO.OMGJØRING,
            ) && innvilgelsesperioder == null
        ) {
            throw IllegalArgumentException("Innvilgelsesperioder er påkrevd")
        }
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        val jsonBody = """
              {
                "fritekst": "$fritekstTilVedtaksbrev",
                "vedtaksperiode": ${if (vedtaksperiode != null) """{"fraOgMed":"${vedtaksperiode.fraOgMed}","tilOgMed":"${vedtaksperiode.tilOgMed}"}""" else null},
                "stansFraOgMed": ${if (stansFraOgMed != null) """"$stansFraOgMed"""" else null},
                "harValgtStansFraFørsteDagSomGirRett": ${stansFraOgMed == null},
                "valgteHjemler": ${valgteHjemler?.joinToString(prefix = "[", postfix = "]") { """"$it"""" }},
                "barnetillegg": $barnetilleggJson,
                "resultat": "$resultat",
                "tilOgMed": ${tilOgMed?.let { "\"$it\"" }},
                "internDeltakelseId": ${internDeltakelseId?.let { "\"$it\"" }},
                "avslagsgrunner": ${avslagsgrunner?.joinToString(prefix = "[", postfix = "]") { """"$it"""" }},
                "innvilgelsesperioder": ${
            innvilgelsesperioder?.let {
                innvilgelsesperioder.joinToString(prefix = "[", postfix = "]") { periode ->
                    """
                    {
                        "periode": {
                            "fraOgMed": "${periode.periode.fraOgMed}",
                            "tilOgMed": "${periode.periode.tilOgMed}"
                        },
                        "antallDagerPerMeldeperiode": ${periode.antallDagerPerMeldeperiode},
                        "internDeltakelseId": "${periode.internDeltakelseId}"
                    }
                """
                }
            }
        }
        }
        """.trimIndent()
        val response = defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/behandling/$behandlingId/forhandsvis",
            jwt = jwt,
            forventet = ForventetRespons(
                status = 200,
                body = ForventetBody.Eksakt("pdf"),
            ),
            body = jsonBody,
        )
        val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        val behandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
        return Triple(sak, behandling, response.body)
    }
}
