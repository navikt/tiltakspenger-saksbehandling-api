package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startBehandlingAvManueltRegistrertSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Oppholdssted
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

/**
 * `fnr` på et barn hentet fra PDL er valgfritt både i ruta og i domenemodellen.
 * Papirsøknadsruta tar imot `"fnr": null`, så tilstanden bygges gjennom prodstien.
 *
 * Rutetesten for papirsøknad dekker allerede varianten med fnr satt; denne dekker fraværet, i begge retninger.
 *
 * `oppholdssted` er også valgfritt: eldre søknader og ubesvarte papirsøknader har det ikke.
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

    @Test
    fun `oppholdssted lagres og leses tilbake, og ubesvart oppholdssted lagres uten oppholdssted`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac)

            @Language("JSON")
            val pdlBarn = """
                [{
                  "fødselsdato": "2015-05-04",
                  "fornavn": "Barn",
                  "mellomnavn": null,
                  "etternavn": "Utenlandsk",
                  "oppholdssted": {"svar": "IKKE_EØS_LAND"},
                  "fnr": null
                }]
            """.trimIndent()

            @Language("JSON")
            val manuelleBarn = """
                [{
                  "fødselsdato": "2018-09-12",
                  "fornavn": "Barn",
                  "mellomnavn": null,
                  "etternavn": "Ubesvart",
                  "oppholdssted": {"svar": "IKKE_BESVART"},
                  "fnr": null
                }]
            """.trimIndent()

            startBehandlingAvManueltRegistrertSøknad(
                tac = tac,
                saksnummer = sak.saksnummer,
                journalpostId = "journalpost-oppholdssted",
                barnetilleggPdlJson = pdlBarn,
                barnetilleggManuelleJson = manuelleBarn,
            )

            val søknad = tac.sakContext.sakRepo.hentForSaksnummer(sak.saksnummer)!!
                .søknader.single { it.journalpostId == "journalpost-oppholdssted" }

            søknad.barnetillegg.filterIsInstance<BarnetilleggFraSøknad.FraPdl>().single().let {
                it.oppholdssted shouldBe Oppholdssted.IKKE_EØS_LAND
                it.oppholderSegIEØS shouldBe Søknad.JaNeiSpm.Nei
            }
            søknad.barnetillegg.filterIsInstance<BarnetilleggFraSøknad.Manuell>().single().let {
                it.oppholdssted shouldBe null
                it.oppholderSegIEØS shouldBe Søknad.JaNeiSpm.IkkeBesvart
            }
        }
    }
}
