package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.motta

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.erHelg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.mottaMeldekortRoute]
 */
interface MottaMeldekortBuilder {

    /**
     * En meldeperiode midt i en innvilgelsesperiode gir rett alle 14 dagene, også lørdag og søndag.
     *
     * @param kanSendeInnHelgForMeldekort Speiler saksfeltet med samme navn.
     * Er det `false`, lar vi helgedagene stå ubesvart slik bruker faktisk sender dem inn.
     * Ellers avvises meldekortet av automatisk behandling med `KAN_IKKE_MELDE_HELG`.
     */
    fun Meldeperiode.tilUtfyltFraBruker(
        kanSendeInnHelgForMeldekort: Boolean = true,
    ): Map<LocalDate, BrukerutfyltMeldekortDTO.Status> {
        return this.girRett.entries.associate { (dato, harRett) ->
            val registrerer = harRett && (kanSendeInnHelgForMeldekort || !dato.erHelg())
            dato to (if (registrerer) BrukerutfyltMeldekortDTO.Status.DELTATT_UTEN_LØNN_I_TILTAKET else BrukerutfyltMeldekortDTO.Status.IKKE_BESVART)
        }
    }

    /**
     * Bygger en sak med et meldekort som er behandlet automatisk, hele veien gjennom prodstiene.
     * [iverksettSøknadsbehandling] registrerer tiltaksdeltakelsen, ruta tar imot brukers meldekort, og jobben behandler det slik den gjør på cron-intervall i prod.
     *
     * Kalleren må sørge for at klokka står på en hverdag innenfor økonomisystemets åpningstider.
     * Ellers hopper jobben over alle meldekort, og standardklokka (1. mai) er både helligdag og utenfor åpningstidene.
     *
     * Jobben henter brukers meldekort på tvers av alle saker.
     * Kjøres denne mot postgres, må testen derfor være isolert (`runIsolated = true` + `@IsolatedDatabaseTest`).
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgBehandleMeldekortAutomatisk(
        tac: TestApplicationContext,
    ): Pair<Sak, BrukersMeldekort> {
        val (sak) = iverksettSøknadsbehandling(tac = tac)
        val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()

        // Ruta setter behandlesAutomatisk = true, slik at jobben plukker meldekortet opp.
        val (_, brukersMeldekort) = mottaMeldekortRequest(
            tac = tac,
            meldeperiodeId = meldeperiode.id,
            sakId = sak.id,
            dager = meldeperiode.tilUtfyltFraBruker(kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort),
        )

        tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

        return sak to brukersMeldekort!!
    }

    suspend fun ApplicationTestBuilder.mottaMeldekortRequest(
        tac: TestApplicationContext,
        meldeperiodeId: MeldeperiodeId,
        sakId: SakId,
        id: MeldekortId = MeldekortId.random(),
        dager: Map<LocalDate, BrukerutfyltMeldekortDTO.Status>,
        mottatt: LocalDateTime = nå(tac.clock),
        journalpostId: String = "1234",
        // Ruta svarer tom 200 med text/plain (respondOk) ved suksess.
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "text/plain; charset=UTF-8"),
        medJsonBody: ((jsonBody: String) -> Unit)? = null,
    ): Triple<Sak, BrukersMeldekort?, String> {
        val jwt = tac.jwtGenerator.createJwtForSystembruker(
            roles = listOf("lagre_meldekort"),
        )
        tac.leggTilBruker(jwt, ObjectMother.systembrukerLagreMeldekort())

        val dagerJson = dager.entries.joinToString(separator = ",\n") { (dato, status) ->
            """    "$dato": "$status""""
        }

        @Language("JSON")
        val body = """
            {
              "id": "$id",
              "meldeperiodeId": "$meldeperiodeId",
              "sakId": "$sakId",
              "mottatt": "$mottatt",
              "journalpostId": "$journalpostId",
              "dager": { $dagerJson }
            }
        """.trimIndent()

        val response = defaultRequestWithAssertions(
            HttpMethod.POST,
            "/meldekort/motta",
            jwt = jwt,
            forventet = forventet,
            body = body,
        )
        val bodyAsText = response.body
        if (medJsonBody != null) {
            medJsonBody(bodyAsText)
        }

        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        val brukersMeldekort = tac.meldekortContext.brukersMeldekortRepo.hentForMeldekortId(id)

        return Triple(
            oppdatertSak,
            brukersMeldekort,
            bodyAsText,
        )
    }
}
