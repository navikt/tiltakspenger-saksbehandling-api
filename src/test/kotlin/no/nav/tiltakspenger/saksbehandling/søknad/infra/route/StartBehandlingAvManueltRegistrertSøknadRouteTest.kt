package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.fraOgMedDatoSpm
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.periodeSpm
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.spørsmål
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startBehandlingAvManueltRegistrertSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.IkkeInnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype
import org.junit.jupiter.api.Test

/**
 * Rundturen for en manuelt registrert søknad, gjennom prodstien.
 *
 * Ruta er den eneste som setter søknadstype, manuelt satt søknadsperiode og tiltak, og barnetillegg — og den eneste som kan gi en søknad uten tiltak.
 * Derfor er dette grunnsettet for `SøknadDAO`, `BarnetilleggDAO` og `SpmFunctions` mot postgres.
 */
class StartBehandlingAvManueltRegistrertSøknadRouteTest {

    @Test
    fun `papirsøknad uten tiltak lagres med barnetillegg og ja-svar, og leses tilbake`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _) = opprettSakOgSøknad(tac)

            startBehandlingAvManueltRegistrertSøknad(
                tac = tac,
                saksnummer = sak.saksnummer,
                søknadstype = "PAPIR_SKJEMA",
                journalpostId = "journalpost-papir",
                manueltSattSøknadsperiodeJson = """{"fraOgMed": "2025-01-01", "tilOgMed": "2025-03-31"}""",
                manueltSattTiltak = "Tiltaket saksbehandler skrev inn",
                behandlingsarsak = "ANNET",
                antallVedlegg = 2,
                barnetilleggPdlJson = """
                    [{
                      "fødselsdato": "2015-05-04",
                      "fornavn": "Barn",
                      "mellomnavn": null,
                      "etternavn": "Barnesen",
                      "oppholdInnenforEøs": {"svar": "JA"},
                      "fnr": "04051512345"
                    }]
                """.trimIndent(),
                barnetilleggManuelleJson = """
                    [{
                      "fødselsdato": "2018-09-12",
                      "fornavn": "Manuelt",
                      "mellomnavn": "Registrert",
                      "etternavn": "Barnesen",
                      "oppholdInnenforEøs": {"svar": "NEI"},
                      "fnr": null
                    }]
                """.trimIndent(),
                svarJson = spørsmål(
                    harSøktOmBarnetillegg = "JA",
                    kvp = periodeSpm("JA", fraOgMed = "2025-01-01", tilOgMed = "2025-01-31"),
                    alderspensjon = fraOgMedDatoSpm("JA", fraOgMed = "2025-02-01"),
                ),
            )

            val søknad = tac.sakContext.sakRepo.hentForSaksnummer(sak.saksnummer)!!
                .søknader.single { it.journalpostId == "journalpost-papir" }

            søknad.shouldBeInstanceOf<IkkeInnvilgbarSøknad>()
            søknad.søknadstype shouldBe Søknadstype.PAPIR_SKJEMA
            søknad.manueltSattSøknadsperiode shouldBe Periode(1.januar(2025), 31.mars(2025))
            søknad.manueltSattTiltak shouldBe "Tiltaket saksbehandler skrev inn"
            søknad.vedlegg shouldBe 2

            søknad.harSøktOmBarnetillegg shouldBe Søknad.JaNeiSpm.Ja
            søknad.kvp shouldBe Søknad.PeriodeSpm.Ja(fraOgMed = 1.januar(2025), tilOgMed = 31.januar(2025))
            søknad.alderspensjon shouldBe Søknad.FraOgMedDatoSpm.Ja(fra = 1.februar(2025))
            søknad.intro shouldBe Søknad.PeriodeSpm.Nei
            søknad.etterlønn shouldBe Søknad.JaNeiSpm.Nei

            søknad.barnetillegg shouldBe listOf(
                BarnetilleggFraSøknad.FraPdl(
                    oppholderSegIEØS = Søknad.JaNeiSpm.Ja,
                    fornavn = "Barn",
                    mellomnavn = null,
                    etternavn = "Barnesen",
                    fødselsdato = 4.mai(2015),
                    fnr = søknad.barnetillegg.filterIsInstance<BarnetilleggFraSøknad.FraPdl>().single().fnr,
                ),
                BarnetilleggFraSøknad.Manuell(
                    oppholderSegIEØS = Søknad.JaNeiSpm.Nei,
                    fornavn = "Manuelt",
                    mellomnavn = "Registrert",
                    etternavn = "Barnesen",
                    fødselsdato = 12.september(2018),
                ),
            )
        }
    }

    @Test
    fun `ubesvarte spørsmål lagres som ubesvarte`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _) = opprettSakOgSøknad(tac)

            startBehandlingAvManueltRegistrertSøknad(
                tac = tac,
                saksnummer = sak.saksnummer,
                søknadstype = "PAPIR_FRIHAND",
                journalpostId = "journalpost-ubesvart",
                svarJson = spørsmål(
                    etterlønn = "IKKE_BESVART",
                    intro = periodeSpm("IKKE_BESVART"),
                    alderspensjon = fraOgMedDatoSpm("IKKE_BESVART"),
                ),
            )

            val søknad = tac.sakContext.sakRepo.hentForSaksnummer(sak.saksnummer)!!
                .søknader.single { it.journalpostId == "journalpost-ubesvart" }

            søknad.søknadstype shouldBe Søknadstype.PAPIR_FRIHAND
            søknad.etterlønn shouldBe Søknad.JaNeiSpm.IkkeBesvart
            søknad.intro shouldBe Søknad.PeriodeSpm.IkkeBesvart
            søknad.alderspensjon shouldBe Søknad.FraOgMedDatoSpm.IkkeBesvart
            søknad.manueltSattSøknadsperiode shouldBe null
            søknad.barnetillegg shouldBe emptyList()
        }
    }

    /**
     * Alle søknadstypene skal overleve rundturen gjennom `SøknadstypeDb`.
     * DIGITAL settes av `mottaSøknadRoute` og dekkes av de øvrige testene; resten kan kun oppstå her.
     */
    @Test
    fun `alle søknadstypene lagres og leses tilbake`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _) = opprettSakOgSøknad(tac)

            listOf(
                Søknadstype.PAPIR_SKJEMA,
                Søknadstype.PAPIR_FRIHAND,
                Søknadstype.MODIA,
                Søknadstype.ANNET,
            ).forEach { søknadstype ->
                val journalpostId = "journalpost-${søknadstype.name}"

                startBehandlingAvManueltRegistrertSøknad(
                    tac = tac,
                    saksnummer = sak.saksnummer,
                    søknadstype = søknadstype.name,
                    journalpostId = journalpostId,
                )

                tac.sakContext.sakRepo.hentForSaksnummer(sak.saksnummer)!!
                    .søknader.single { it.journalpostId == journalpostId }
                    .søknadstype shouldBe søknadstype
            }
        }
    }
}
