package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

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
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForAvslagDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDate

/**
 * Gjelder for innvilgelse, avslag og stans.
 */
interface ForhåndsvisVedtaksbrevTestbuilder {

    /** Forventer at det allerede finnes en sak, søknad og behandling. */
    suspend fun ApplicationTestBuilder.forhåndsvisVedtaksbrevForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fritekstTilVedtaksbrev: String = "some_tekst",
        virkningsperiode: Periode? = null,
        stansFraOgMed: LocalDate? = null,
        stansTilOgMed: LocalDate? = null,
        valgteHjemler: List<ValgtHjemmelForStansDTO>? = null,
        barnetillegg: List<BarnetilleggPeriodeDTO>? = null,
        resultat: BehandlingResultatDTO,
        avslagsgrunner: List<ValgtHjemmelForAvslagDTO>? = null,
    ): Triple<Sak, Rammebehandling, String> {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.texasClient.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/behandling/$behandlingId/forhandsvis")
            },
            jwt = jwt,
        ) {
            val jsonBody = """
                  {
                    "fritekst": "$fritekstTilVedtaksbrev",
                    "virkningsperiode": ${if (virkningsperiode != null) """{"fraOgMed":"${virkningsperiode.fraOgMed}","tilOgMed":"${virkningsperiode.tilOgMed}"}""" else null},
                    "stansFraOgMed": ${if (stansFraOgMed != null) """"$stansFraOgMed"""" else null},
                    "stansTilOgMed": ${if (stansTilOgMed != null) """"$stansTilOgMed"""" else null},
                    "valgteHjemler": ${valgteHjemler?.joinToString(prefix = "[", postfix = "]") { """"$it"""" }},
                    "barnetillegg": ${
                barnetillegg?.joinToString(
                    prefix = "[",
                    postfix = "]",
                ) { """{"antallBarn":${it.antallBarn},"periode":{"fraOgMed":"${it.periode.fraOgMed}","tilOgMed":"${it.periode.tilOgMed}"}}""" }
            },
                    "resultat": "$resultat",
                    "avslagsgrunner": ${avslagsgrunner?.joinToString(prefix = "[", postfix = "]") { """"$it"""" }}
                  }
            """.trimIndent()
            setBody(jsonBody)
        }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe HttpStatusCode.OK
                bodyAsText shouldBe "pdf"
            }
            val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val behandling = tac.behandlingContext.behandlingRepo.hent(behandlingId)
            return Triple(sak, behandling, bodyAsText)
        }
    }
}
