package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.saksbehandling.journalpost.DokumentInfoId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Avvist
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Omgjør
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Opprettholdt
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Resultatvariantene og de valgfrie feltene gir mange flere kombinasjoner enn prodstiene når i én kjøring, og `dokumentInfoIder: null` finnes bare i gamle rader.
 * Mappingen rører ikke postgres — den er ren json.
 *
 * Testen pinner **den faktiske json-en**, ikke bare rundturen, jf. mønsteret i [no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.HjemmelForOpphørDbTest].
 */
class KlagebehandlingsresultatDbJsonTest {

    /** En gammel rad skrevet før `dokumentInfoIder` fantes har null der — den leses som tom liste. */
    @Test
    fun `en gammel rad med null dokumentInfoIder leses som tom liste`() {
        val resultat = opprettholdtJson(
            journalført = false,
            dokumentInfoIder = "null",
            begrunnelseFerdigstilling = "null",
        ).toKlagebehandlingResultat(brevtekst = null)

        resultat.shouldBeInstanceOf<Opprettholdt>().dokumentInfoIder shouldBe emptyList()
    }

    @Test
    fun `journalførte dokumenter og ferdigstillingsbegrunnelse leses tilbake og skrives likt`() {
        val lagret = opprettholdtJson(
            journalført = true,
            dokumentInfoIder = """["521649734"]""",
            begrunnelseFerdigstilling = "\"Klagen er behandlet på nytt\"",
        )

        val opprettholdt = lagret.toKlagebehandlingResultat(
            brevtekst = Brevtekster(listOf(TittelOgTekst(tittel = "Vurdering", tekst = "Vedtaket opprettholdes"))),
        ).shouldBeInstanceOf<Opprettholdt>()

        opprettholdt.dokumentInfoIder shouldBe listOf(DokumentInfoId("521649734"))
        opprettholdt.begrunnelseFerdigstilling?.verdi shouldBe "Klagen er behandlet på nytt"
        opprettholdt.toDbJson() shouldEqualJson lagret
    }

    @Test
    fun `omgjøring leses tilbake og skrives likt`() {
        val lagret = omgjørJson()

        val omgjør = lagret.toKlagebehandlingResultat(brevtekst = null).shouldBeInstanceOf<Omgjør>()

        omgjør.begrunnelse.verdi shouldBe "Vedtaket bygde på feil fakta"
        omgjør.toDbJson() shouldEqualJson lagret
    }

    @Test
    fun `avvist skrives uten omgjørings- og opprettholdt-felter`() {
        Avvist(brevtekst = null).toDbJson() shouldEqualJson
            //language=json
            """
            {
              "type": "AVVIST",
              "omgjørBegrunnelse": null,
              "omgjørÅrsak": null,
              "behandlingId": [],
              "åpenBehandlingId": null,
              "hjemler": null,
              "iverksattOpprettholdelseTidspunkt": null,
              "brevdato": null,
              "oversendtKlageinstansenTidspunkt": null,
              "journalpostIdInnstillingsbrev": null,
              "dokumentInfoIder": [],
              "journalføringstidspunktInnstillingsbrev": null,
              "distribusjonIdInnstillingsbrev": null,
              "distribusjonstidspunktInnstillingsbrev": null,
              "klageinstanshendelser": [],
              "ferdigstiltTidspunkt": null,
              "begrunnelseFerdigstilling": null
            }
            """.trimIndent()
    }

    /** Domenets init krever brevtekst og journalpost-felter i par, så den journalførte varianten setter hele kjeden. */
    //language=json
    private fun opprettholdtJson(
        journalført: Boolean,
        dokumentInfoIder: String,
        begrunnelseFerdigstilling: String,
    ): String = """
    {
      "type": "OPPRETTHOLDT",
      "omgjørBegrunnelse": null,
      "omgjørÅrsak": null,
      "behandlingId": [],
      "åpenBehandlingId": null,
      "hjemler": ["ARBEIDSMARKEDSLOVEN_13"],
      "iverksattOpprettholdelseTidspunkt": ${if (journalført) "\"2025-01-06T12:00:00\"" else "null"},
      "brevdato": null,
      "oversendtKlageinstansenTidspunkt": null,
      "journalpostIdInnstillingsbrev": ${if (journalført) "\"123456789\"" else "null"},
      "dokumentInfoIder": $dokumentInfoIder,
      "journalføringstidspunktInnstillingsbrev": ${if (journalført) "\"2025-01-06T12:30:00\"" else "null"},
      "distribusjonIdInnstillingsbrev": null,
      "distribusjonstidspunktInnstillingsbrev": null,
      "klageinstanshendelser": [],
      "ferdigstiltTidspunkt": null,
      "begrunnelseFerdigstilling": $begrunnelseFerdigstilling
    }
    """.trimIndent()

    //language=json
    private fun omgjørJson(): String = """
    {
      "type": "OMGJØR",
      "omgjørBegrunnelse": "Vedtaket bygde på feil fakta",
      "omgjørÅrsak": "FEIL_ELLER_ENDRET_FAKTA",
      "behandlingId": ["${RammebehandlingId.random()}"],
      "åpenBehandlingId": null,
      "hjemler": null,
      "iverksattOpprettholdelseTidspunkt": null,
      "brevdato": null,
      "oversendtKlageinstansenTidspunkt": null,
      "journalpostIdInnstillingsbrev": null,
      "dokumentInfoIder": [],
      "journalføringstidspunktInnstillingsbrev": null,
      "distribusjonIdInnstillingsbrev": null,
      "distribusjonstidspunktInnstillingsbrev": null,
      "klageinstanshendelser": [],
      "ferdigstiltTidspunkt": "2025-01-07T09:00:00",
      "begrunnelseFerdigstilling": "Ferdigstilt etter omgjøring"
    }
    """.trimIndent()
}
