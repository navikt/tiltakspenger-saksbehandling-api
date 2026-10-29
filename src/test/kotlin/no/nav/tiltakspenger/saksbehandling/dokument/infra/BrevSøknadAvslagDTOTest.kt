package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.NonEmptySet
import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptySetOrThrow
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.lesTre
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.Navn
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import java.time.LocalDate

class BrevSøknadAvslagDTOTest {

    @Test
    fun `genererer og serialiserer brevdata for pdf`() {
        runTest {
            val fnr = Fnr.random()
            val actual = genererAvslagSøknadsbrev(
                hentBrukersNavn = { _ -> Navn("Fornavn", null, "Etternavn") },
                hentSaksbehandlersNavn = { _ -> "Saksbehandlernavn" },
                tilleggstekst = FritekstTilVedtaksbrev.create("genererer og serialiserer brevdata for pdf test"),
                avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
                fnr = fnr,
                saksbehandlerNavIdent = "SaksbehandlerNavIdent",
                beslutterNavIdent = null,
                saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "2000"),
                forhåndsvisning = true,
                harSøktBarnetillegg = true,
                avslagsperiode = ObjectMother.vedtaksperiode(),
                datoForUtsending = LocalDate.now(fixedClock),
            )

            //language=json
            val expected = """
                {
                  "personalia":{
                    "ident":"${fnr.verdi}",
                    "fornavn":"Fornavn",
                    "etternavn":"Etternavn"
                  },
                  "saksnummer":"202501012000",
                  "saksbehandlerNavn":"Saksbehandlernavn",
                  "beslutterNavn":null,
                  "tilleggstekst":"genererer og serialiserer brevdata for pdf test",
                  "avslagsgrunner":["ALDER"],
                  "harSøktMedBarn":true,
                  "hjemler":[{"rettskilde":"TILTAKSPENGEFORSKRIFTEN","paragraf":"3","ledd":null},{"rettskilde":"TILTAKSPENGEFORSKRIFTEN","paragraf":"3","ledd":null}],
                  "forhandsvisning":true,
                  "avslagFraOgMed":"2023-01-01",
                  "avslagTilOgMed":"2023-03-31",
                  "datoForUtsending": "2025-01-01"
                  
                }
            """.trimIndent()

            actual.shouldEqualJson(expected)
        }
    }

    @Test
    fun `genererer og serialiserer brevdata for pdf fra et vedtak`() {
        val (_, avslagsvedtak) = ObjectMother.nySakMedAvslagsvedtak()
        runTest {
            avslagsvedtak.genererAvslagSøknadsbrev(
                hentBrukersNavn = { _: Fnr -> Navn("Fornavn", null, "Etternavn") },
                hentSaksbehandlersNavn = { _: String -> "Saksbehandlernavn" },
                datoForUtsending = LocalDate.now(fixedClock),
            ).shouldEqualJson(
                //language=json
                """
                {
                  "personalia":{
                    "ident":"${avslagsvedtak.fnr.verdi}",
                    "fornavn":"Fornavn",
                    "etternavn":"Etternavn"
                  },
                  "saksnummer":"${avslagsvedtak.saksnummer}",
                  "saksbehandlerNavn":"Saksbehandlernavn",
                  "beslutterNavn":"Saksbehandlernavn",
                  "tilleggstekst":"nySakMedAvslagsvedtak",
                  "avslagsgrunner":["ALDER"],
                  "harSøktMedBarn":false,
                  "hjemler":[{"rettskilde":"TILTAKSPENGEFORSKRIFTEN","paragraf":"3","ledd":null}],
                  "forhandsvisning":false,
                  "avslagFraOgMed":"2023-01-01",
                  "avslagTilOgMed":"2023-03-31",
                  "datoForUtsending": "2025-01-01"
                  
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `én avslagsgrunn gir koden og hjemlene til grunnen, med og uten barnetillegg`() {
        runTest {
            Avslagsgrunnlag.entries.forEach { grunn ->
                listOf(false, true).forEach { barnetillegg ->
                    withClue("avslagsgrunn=$grunn, barnetillegg=$barnetillegg") {
                        val brev = genererBrevJson(nonEmptySetOf(grunn), barnetillegg)
                        brev.koder("avslagsgrunner") shouldBe listOf(grunn.tilGrunnBrevDto().name)
                        brev.get("hjemler").toString().shouldEqualJson(serialize(forventedeHjemler(listOf(grunn), barnetillegg)))
                        brev.skalIkkeHaHjemmeltekst()
                    }
                }
            }
        }
    }

    @Test
    fun `flere avslagsgrunner gir kodene og de samlede hjemlene i samme rekkefølge, med og uten barnetillegg`() {
        runTest {
            val alleGrunner = Avslagsgrunnlag.entries.toNonEmptySetOrThrow()
            listOf(false, true).forEach { barnetillegg ->
                withClue("barnetillegg=$barnetillegg") {
                    val brev = genererBrevJson(alleGrunner, barnetillegg)
                    brev.koder("avslagsgrunner") shouldBe alleGrunner.map { it.tilGrunnBrevDto().name }
                    brev.get("hjemler").toString().shouldEqualJson(serialize(forventedeHjemler(alleGrunner.toList(), barnetillegg)))
                    brev.skalIkkeHaHjemmeltekst()
                }
            }
        }
    }

    @Test
    fun `brevdata som testdataene i pdfgenrs speiler`() {
        runTest {
            val brev = genererBrevJson(
                nonEmptySetOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak, Avslagsgrunnlag.Livsoppholdytelser),
                harSøktBarnetillegg = true,
            )
            brev.koder("avslagsgrunner") shouldBe listOf("DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK", "LIVSOPPHOLDYTELSE")
            brev.skalIkkeHaHjemmeltekst()
            brev.get("hjemler").toString().shouldEqualJson(
                //language=json
                """
                [
                  {"rettskilde":"TILTAKSPENGEFORSKRIFTEN","paragraf":"2","ledd":null},
                  {"rettskilde":"ARBEIDSMARKEDSLOVEN","paragraf":"13","ledd":null},
                  {"rettskilde":"TILTAKSPENGEFORSKRIFTEN","paragraf":"7","ledd":1},
                  {"rettskilde":"ARBEIDSMARKEDSLOVEN","paragraf":"13","ledd":1},
                  {"rettskilde":"TILTAKSPENGEFORSKRIFTEN","paragraf":"3","ledd":null}
                ]
                """.trimIndent(),
            )
        }
    }

    private suspend fun genererBrevJson(
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
        harSøktBarnetillegg: Boolean,
    ): JsonNode = lesTre(
        genererAvslagSøknadsbrev(
            hentBrukersNavn = { _ -> Navn("Fornavn", null, "Etternavn") },
            hentSaksbehandlersNavn = { _ -> "Saksbehandlernavn" },
            tilleggstekst = null,
            avslagsgrunner = avslagsgrunner,
            fnr = Fnr.random(),
            saksbehandlerNavIdent = "SaksbehandlerNavIdent",
            beslutterNavIdent = null,
            saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "2000"),
            forhåndsvisning = true,
            harSøktBarnetillegg = harSøktBarnetillegg,
            avslagsperiode = ObjectMother.vedtaksperiode(),
            datoForUtsending = LocalDate.now(fixedClock),
        ),
    )

    private fun forventedeHjemler(grunner: List<Avslagsgrunnlag>, harSøktBarnetillegg: Boolean): List<HjemmelBrevDto> =
        (grunner.flatMap { it.hjemler } + if (harSøktBarnetillegg) listOf(Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_3) else emptyList())
            .map { it.tilHjemmelBrevDto() }
}
