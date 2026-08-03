package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startBehandlingAvManueltRegistrertSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

/**
 * `fnr` på et barn hentet fra PDL er valgfritt både i ruta og i domenemodellen.
 * Papirsøknadsruta tar imot `"fnr": null`, så tilstanden bygges gjennom prodstien.
 *
 * Rutetesten for papirsøknad dekker allerede varianten med fnr satt; denne dekker fraværet, i begge retninger.
 */
class BarnetilleggDAOTest {

    @Test
    fun `pdl-barn uten fnr lagres og leses tilbake uten fnr`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac)

            @Language("JSON")
            val barnUtenFnr = """
                [{
                  "fødselsdato": "2015-05-04",
                  "fornavn": "Barn",
                  "mellomnavn": null,
                  "etternavn": "Utenfnr",
                  "oppholdInnenforEøs": {"svar": "JA"},
                  "fnr": null
                }]
            """.trimIndent()

            startBehandlingAvManueltRegistrertSøknad(
                tac = tac,
                saksnummer = sak.saksnummer,
                journalpostId = "journalpost-barn-uten-fnr",
                barnetilleggPdlJson = barnUtenFnr,
            )

            val søknad = tac.sakContext.sakRepo.hentForSaksnummer(sak.saksnummer)!!
                .søknader.single { it.journalpostId == "journalpost-barn-uten-fnr" }

            val barn = søknad.barnetillegg.filterIsInstance<BarnetilleggFraSøknad.FraPdl>().single()
            barn.etternavn shouldBe "Utenfnr"
            barn.fnr shouldBe null
        }
    }
}
