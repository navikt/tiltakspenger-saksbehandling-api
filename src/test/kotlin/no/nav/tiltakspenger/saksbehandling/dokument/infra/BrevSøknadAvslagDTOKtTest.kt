package no.nav.tiltakspenger.saksbehandling.dokument.infra

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrevSøknadAvslagDTOKtTest {

    @Test
    fun `genererer og serialiserer brevdata for pdf`() {
        runTest {
            val fnr = Fnr.random()
            val actual = genererAvslagSøknadsbrev(
                hentBrukersNavn = { _ -> Navn("Fornavn", null, "Etternavn") },
                hentSaksbehandlersNavn = { _ -> "Saksbehandlernavn" },
                tilleggstekst = FritekstTilVedtaksbrev("genererer og serialiserer brevdata for pdf test"),
                avslagsgrunner = setOf(Avslagsgrunnlag.Alder),
                fnr = fnr,
                saksbehandlerNavIdent = "SaksbehandlerNavIdent",
                beslutterNavIdent = null,
                saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "2000"),
                forhåndsvisning = true,
                harSøktBarnetillegg = true,
                avslagsperiode = ObjectMother.virkningsperiode(),
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
                  "avslagsgrunnerSize":1,
                  "avslagsgrunner":["ALDER"],
                  "harSøktMedBarn":true,
                  "hjemlerTekst":null,
                  "forhåndsvisning":true,
                  "avslagFraOgMed":"1. januar 2023",
                  "avslagTilOgMed":"31. mars 2023",
                  "datoForUtsending": "1. januar 2025"
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
                  "avslagsgrunnerSize":1,
                  "avslagsgrunner":["ALDER"],
                  "harSøktMedBarn":false,
                  "hjemlerTekst":null,
                  "forhåndsvisning":false,
                  "avslagFraOgMed":"1. januar 2023",
                  "avslagTilOgMed":"31. mars 2023",
                  "datoForUtsending": "1. januar 2025"
                }
                """.trimIndent(),
            )
        }
    }

    @Nested
    inner class CreateBrevForskrifter {
        @Test
        fun `lager forskrifter med bare tiltakspengeforskrifter`() {
            val actual = setOf(Avslagsgrunnlag.Kvalifiseringsprogrammet).createBrevForskrifter(true)
            val expected = """
                Dette kommer frem av tiltakspengeforskriften §§ 3, 7 tredje ledd.
            """.trimIndent()

            actual shouldBe expected
        }

        @Test
        fun `lager forskrifter med tiltakspengerforskrifter og arbeidsmarkedsloven`() {
            val actual = setOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak).createBrevForskrifter(true)
            val expected = """
                Dette kommer frem av arbeidsmarkedsloven § 13, og tiltakspengeforskriften §§ 2, 3.
            """.trimIndent()

            actual shouldBe expected
        }
    }

    @Test
    fun `mapper Avslagsgrunnlag til AvslagsgrunnerBrevDto`() {
        Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
        Avslagsgrunnlag.Alder.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.ALDER
        Avslagsgrunnlag.Livsoppholdytelser.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.LIVSOPPHOLDYTELSE
        Avslagsgrunnlag.Kvalifiseringsprogrammet.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.KVALIFISERINGSPROGRAMMET
        Avslagsgrunnlag.Introduksjonsprogrammet.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.INTRODUKSJONSPROGRAMMET
        Avslagsgrunnlag.LønnFraTiltaksarrangør.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.LØNN_FRA_TILTAKSARRANGØR
        Avslagsgrunnlag.LønnFraAndre.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.LØNN_FRA_ANDRE
        Avslagsgrunnlag.Institusjonsopphold.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.INSTITUSJONSOPPHOLD
        Avslagsgrunnlag.FremmetForSent.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.FREMMET_FOR_SENT
    }

    @Test
    fun `mapper liste av Avslagsgrunnlag til AvslagsgrunnerBrevDto`() {
        setOf(
            Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak,
            Avslagsgrunnlag.Alder,
        ).toAvslagsgrunnerBrevDto() shouldBe
            listOf(
                AvslagsgrunnerBrevDto.DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
                AvslagsgrunnerBrevDto.ALDER,
            )
    }
}
