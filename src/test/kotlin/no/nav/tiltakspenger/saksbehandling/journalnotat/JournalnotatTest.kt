package no.nav.tiltakspenger.saksbehandling.journalnotat

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.dokument.infra.tilJournalnotatDokumentJson
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.createOrThrow
import no.nav.tiltakspenger.saksbehandling.journalnotat.infra.http.tilJournalpostRequest
import no.nav.tiltakspenger.saksbehandling.journalnotat.infra.tilBegrunnelseTittel
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.Test

class JournalnotatTest {

    private val begrunnelse = Begrunnelse.createOrThrow("Linje én.\nLinje to.")

    private val fnr = Fnr.random()

    // Ulik vedtaksdatoen i ObjectMother, slik at testene viser at notatet bruker datoen det ble generert.
    private val notatsdato = 3.januar(2025)

    private val journalnotat = Journalnotat(
        vedtakId = VedtakId.fromString("vedtak_01KAJ7K0Y7SJ1Y5BN5MN8Z9FDV"),
        sakId = SakId.fromString("sak_01KAJ7K0Y7SJ1Y5BN5MN8Z9FDW"),
        saksnummer = Saksnummer("202501011001"),
        fnr = fnr,
        vedtakstype = Journalnotat.Vedtakstype.SØKNAD_AVSLAG,
        saksbehandler = "Z123456",
        beslutter = "B123456",
        notatsdato = 2.januar(2025),
        begrunnelse = begrunnelse,
    )

    @Test
    fun `journalposten er et NOTAT uten kanal, mottaker og overstyrt innsyn, og skiller seg fra vedtaksbrevet på eksternReferanseId`() {
        val pdfOgJson = PdfOgJson(PdfA("pdf".toByteArray()), """{"notat":"json"}""")

        journalnotat.tilJournalpostRequest(pdfOgJson).shouldEqualJson(
            """
            {
              "tittel": "Notat om vedtak om tiltakspenger",
              "journalpostType": "NOTAT",
              "kanal": null,
              "avsenderMottaker": null,
              "bruker": {
                "id": "${fnr.verdi}",
                "idType": "FNR"
              },
              "sak": {
                "fagsakId": "202501011001",
                "fagsaksystem": "TILTAKSPENGER",
                "sakstype": "FAGSAK"
              },
              "tema": "IND",
              "journalfoerendeEnhet": "9999",
              "dokumenter": [
                {
                  "tittel": "Notat om vedtak om tiltakspenger",
                  "brevkode": "NOTAT-TILTAKSPENGER",
                  "dokumentvarianter": [
                    {
                      "filtype": "PDFA",
                      "fysiskDokument": "cGRm",
                      "variantformat": "ARKIV",
                      "filnavn": "Notat om vedtak om tiltakspenger.pdf",
                      "tittel": "Notat om vedtak om tiltakspenger"
                    },
                    {
                      "filtype": "JSON",
                      "fysiskDokument": "eyJub3RhdCI6Impzb24ifQ==",
                      "variantformat": "ORIGINAL",
                      "filnavn": "Notat om vedtak om tiltakspenger.json",
                      "tittel": "Notat om vedtak om tiltakspenger"
                    }
                  ]
                }
              ],
              "eksternReferanseId": "vedtak_01KAJ7K0Y7SJ1Y5BN5MN8Z9FDV-notat",
              "overstyrInnsynsregler": null
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `pdfgenrs-payloaden har navn, metadata og begrunnelsen uendret`() = runTest {
        val json = journalnotat.tilJournalnotatDokumentJson(
            hentBrukersNavn = { Navn("Ola", "Mellom", "Nordmann") },
            hentSaksbehandlersNavn = { navIdent -> "Navn for $navIdent" },
        )

        json.shouldEqualJson(
            """
            {
              "personalia": {
                "ident": "${fnr.verdi}",
                "fornavn": "Ola",
                "etternavn": "Mellom Nordmann"
              },
              "saksnummer": "202501011001",
              "tittel": "Notat om vedtak om tiltakspenger",
              "notatsdato": "2. januar 2025",
              "saksbehandlerNavn": "Navn for Z123456",
              "beslutterNavn": "Navn for B123456",
              "begrunnelseTittel": "Begrunnelse for vilkårsvurderingen",
              "begrunnelse": "Linje én.\nLinje to."
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `hver vedtakstype har sin begrunnelsestittel`() {
        Journalnotat.Vedtakstype.entries.associateWith { it.tilBegrunnelseTittel() } shouldBe mapOf(
            Journalnotat.Vedtakstype.SØKNAD_INNVILGELSE to "Begrunnelse for vilkårsvurderingen",
            Journalnotat.Vedtakstype.SØKNAD_AVSLAG to "Begrunnelse for vilkårsvurderingen",
            Journalnotat.Vedtakstype.REVURDERING_INNVILGELSE to "Begrunnelse for vilkårsvurderingen",
            Journalnotat.Vedtakstype.STANS to "Begrunnelse for vilkårsvurderingen",
            Journalnotat.Vedtakstype.OMGJØRING_INNVILGELSE to "Begrunnelse for vilkårsvurderingen",
            Journalnotat.Vedtakstype.OMGJØRING_OPPHØR to "Begrunnelse for vilkårsvurderingen",
            Journalnotat.Vedtakstype.MELDEKORT to "Begrunnelse",
        )
    }

    @Test
    fun `rammevedtak gir notat med vedtakstype utledet fra resultatet`() {
        ObjectMother.nyRammevedtakInnvilgelse().medNotat().tilJournalnotat(notatsdato).vedtakstype shouldBe
            Journalnotat.Vedtakstype.SØKNAD_INNVILGELSE
        ObjectMother.nyRammevedtakAvslag().medNotat().tilJournalnotat(notatsdato).vedtakstype shouldBe
            Journalnotat.Vedtakstype.SØKNAD_AVSLAG
        ObjectMother.nyRammevedtakStans().medNotat().tilJournalnotat(notatsdato).vedtakstype shouldBe
            Journalnotat.Vedtakstype.STANS
        ObjectMother.nyRammevedtakOmgjøring().medNotat().tilJournalnotat(notatsdato).vedtakstype shouldBe
            Journalnotat.Vedtakstype.OMGJØRING_INNVILGELSE
    }

    @Test
    fun `rammevedtak gir notat med metadata fra vedtaket og begrunnelsen fra behandlingen`() {
        val vedtak = ObjectMother.nyRammevedtakInnvilgelse().medNotat()

        vedtak.tilJournalnotat(notatsdato) shouldBe Journalnotat(
            vedtakId = vedtak.id,
            sakId = vedtak.sakId,
            saksnummer = vedtak.saksnummer,
            fnr = vedtak.fnr,
            vedtakstype = Journalnotat.Vedtakstype.SØKNAD_INNVILGELSE,
            saksbehandler = vedtak.saksbehandler,
            beslutter = vedtak.beslutter,
            notatsdato = notatsdato,
            begrunnelse = begrunnelse,
        )
    }

    @Test
    fun `rammevedtak uten valgt journalføring kan ikke bli notat`() {
        shouldThrow<IllegalArgumentException> { ObjectMother.nyRammevedtakInnvilgelse().tilJournalnotat(notatsdato) }
    }

    @Test
    fun `meldekortvedtak gir notat med begrunnelsen fra meldekortbehandlingen`() {
        val vedtak = ObjectMother.meldekortvedtak(
            meldekortbehandling = ObjectMother.meldekortBehandletManuelt(
                begrunnelse = begrunnelse,
                skalJournalføreNotat = true,
            ),
        )

        vedtak.tilJournalnotat(notatsdato) shouldBe Journalnotat(
            vedtakId = vedtak.id,
            sakId = vedtak.sakId,
            saksnummer = vedtak.saksnummer,
            fnr = vedtak.fnr,
            vedtakstype = Journalnotat.Vedtakstype.MELDEKORT,
            saksbehandler = vedtak.saksbehandler,
            beslutter = vedtak.beslutter,
            notatsdato = notatsdato,
            begrunnelse = begrunnelse,
        )
    }

    @Test
    fun `meldekortvedtak uten valgt journalføring kan ikke bli notat`() {
        shouldThrow<IllegalArgumentException> {
            ObjectMother.meldekortvedtak(
                meldekortbehandling = ObjectMother.meldekortBehandletManuelt(skalJournalføreNotat = false),
            ).tilJournalnotat(notatsdato)
        }
    }

    private fun Rammevedtak.medNotat(): Rammevedtak = copy(
        rammebehandling = when (val behandling = rammebehandling) {
            is Søknadsbehandling -> behandling.copy(skalJournalføreNotat = true, begrunnelseVilkårsvurdering = begrunnelse)
            is Revurdering -> behandling.copy(skalJournalføreNotat = true, begrunnelseVilkårsvurdering = begrunnelse)
        },
    )
}
