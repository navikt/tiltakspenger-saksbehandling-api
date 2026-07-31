package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother

/**
 * Registrerer en søknad manuelt, slik en saksbehandler gjør med en papirsøknad.
 *
 * Dette er den eneste prodstien som setter feltene den digitale søknaden ikke har:
 * søknadstype, manuelt satt søknadsperiode og tiltak, behandlingsårsak — og en søknad helt uten tiltak, som blir en `IkkeInnvilgbarSøknad`.
 */
interface StartBehandlingAvManueltRegistrertSøknadRouteBuilder {

    /**
     * @param svarJson Hele `svar`-objektet, slik at testen kan variere spørsmålstypene og barnetillegget.
     * @param tiltakJson `null` gir en søknad uten tiltak, altså en `IkkeInnvilgbarSøknad`.
     */
    suspend fun ApplicationTestBuilder.startBehandlingAvManueltRegistrertSøknad(
        tac: TestApplicationContext,
        saksnummer: Saksnummer,
        søknadstype: String = "PAPIR_SKJEMA",
        journalpostId: String = "journalpost-manuell",
        manueltSattSøknadsperiodeJson: String? = null,
        manueltSattTiltak: String? = null,
        behandlingsarsak: String? = null,
        antallVedlegg: Int = 0,
        tiltakJson: String? = null,
        barnetilleggPdlJson: String = "[]",
        barnetilleggManuelleJson: String = "[]",
        svarJson: String = alleSpørsmålNei(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons = ForventetRespons(status = 200),
    ): String? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)

        // Tjenesten validerer journalposten mot SAF før den oppretter søknaden, så den må finnes på sakens fnr.
        val fnr = tac.sakContext.sakRepo.hentForSaksnummer(saksnummer)!!.fnr
        tac.leggTilJournalpost(JournalpostId(journalpostId), fnr)

        val respons = defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/${saksnummer.verdi}/soknad",
            jwt = jwt,
            forventet = forventet,
            body = """
                {
                  "journalpostId": "$journalpostId",
                  "manueltSattSøknadsperiode": $manueltSattSøknadsperiodeJson,
                  "manueltSattTiltak": ${manueltSattTiltak?.let { "\"$it\"" }},
                  "antallVedlegg": $antallVedlegg,
                  "søknadstype": "$søknadstype",
                  "behandlingsarsak": ${behandlingsarsak?.let { "\"$it\"" }},
                  "svar": {
                    "tiltak": $tiltakJson,
                    "barnetilleggPdl": $barnetilleggPdlJson,
                    "barnetilleggManuelle": $barnetilleggManuelleJson,
                    $svarJson
                  }
                }
            """.trimIndent(),
        )

        return if (forventet.status == 200) respons.body else null
    }

    /** Grunnformen: alle spørsmål besvart med nei. */
    fun alleSpørsmålNei(): String = spørsmål()

    /**
     * Bygger `svar`-feltene for spørsmålene.
     * Hver verdi er `JA`, `NEI` eller `IKKE_BESVART`; periodespørsmålene tar i tillegg fra- og til-dato.
     */
    fun spørsmål(
        harSøktPåTiltak: String = "NEI",
        harSøktOmBarnetillegg: String = "NEI",
        kvp: String = periodeSpm("NEI"),
        intro: String = periodeSpm("NEI"),
        institusjon: String = periodeSpm("NEI"),
        etterlønn: String = "NEI",
        gjenlevendepensjon: String = periodeSpm("NEI"),
        alderspensjon: String = fraOgMedDatoSpm("NEI"),
        sykepenger: String = periodeSpm("NEI"),
        supplerendeStønadAlder: String = periodeSpm("NEI"),
        supplerendeStønadFlyktning: String = periodeSpm("NEI"),
        jobbsjansen: String = periodeSpm("NEI"),
        trygdOgPensjon: String = periodeSpm("NEI"),
    ): String = """
        "harSøktPåTiltak": {"svar": "$harSøktPåTiltak"},
        "harSøktOmBarnetillegg": {"svar": "$harSøktOmBarnetillegg"},
        "kvp": $kvp,
        "intro": $intro,
        "institusjon": $institusjon,
        "etterlønn": {"svar": "$etterlønn"},
        "gjenlevendepensjon": $gjenlevendepensjon,
        "alderspensjon": $alderspensjon,
        "sykepenger": $sykepenger,
        "supplerendeStønadAlder": $supplerendeStønadAlder,
        "supplerendeStønadFlyktning": $supplerendeStønadFlyktning,
        "jobbsjansen": $jobbsjansen,
        "trygdOgPensjon": $trygdOgPensjon
    """.trimIndent()

    fun periodeSpm(svar: String, fraOgMed: String? = null, tilOgMed: String? = null): String =
        """{"svar": "$svar", "fraOgMed": ${fraOgMed?.let { "\"$it\"" }}, "tilOgMed": ${tilOgMed?.let { "\"$it\"" }}}"""

    fun fraOgMedDatoSpm(svar: String, fraOgMed: String? = null): String =
        """{"svar": "$svar", "fraOgMed": ${fraOgMed?.let { "\"$it\"" }}}"""
}
