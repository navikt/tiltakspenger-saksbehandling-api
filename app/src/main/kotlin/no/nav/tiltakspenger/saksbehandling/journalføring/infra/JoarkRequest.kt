@file:Suppress("unused")

package no.nav.tiltakspenger.saksbehandling.clients.joark

/**
 * @param eksternReferanseId Brukes som dedup-nøkkel.
 *
 * https://confluence.adeo.no/display/BOA/opprettJournalpost
 */
internal data class JoarkRequest(
    /**
     * Tittel som beskriver forsendelsen samlet, feks "Søknad om foreldrepenger".
     */
    val tittel: String,
    val journalpostType: JournalPostType,
    /**
     * Temaet som forsendelsen tilhører, for eksempel "FOR" (foreldrepenger).
     * Se Tema for en oversikt over gyldige verdier.
     * Tema er påkrevd dersom Sak oppgis.
     */
    val tema: String = "IND",
    /**
     * Kanalen som ble brukt ved innsending eller distribusjon. F.eks. NAV_NO, ALTINN eller EESSI.
     * Se Mottakskanal og Utsendingskanal for gyldige verdier for henholdsvis inngående og utgående dokumenter. Kanal skal ikke settes for notater.
     */
    val kanal: String?,
    /**
     * NAV-enheten som har journalført forsendelsen.
     * Dersom forsoekFerdigstill=true skal enhet alltid settes. Dersom  det ikke er noen Nav-enhet involvert (f.eks. ved automatisk journalføring), skal enhet være '9999'.
     * Dersom foersoekFerdigstill=false bør journalførendeEnhet kun settes dersom oppgavene skal rutes på en annen måte enn Norg-reglene tilsier. Hvis enhet er blank, havner oppgavene på enheten som ligger i Norg-regelsettet.
     */
    val journalfoerendeEnhet: String = "9999",
    val avsenderMottaker: AvsenderMottaker?,
    val bruker: Bruker,
    val sak: Sak?,
    val dokumenter: List<JournalpostDokument>,
    val eksternReferanseId: String,
) {
    /**
     * INNGAAENDE brukes for dokumentasjon som NAV har mottatt fra en ekstern part. Dette kan være søknader, ettersendelser av dokumentasjon til sak eller meldinger fra arbeidsgivere.
     * UTGAAENDE brukes for dokumentasjon som NAV har produsert og sendt ut til en ekstern part. Dette kan for eksempel være informasjons- eller vedtaksbrev til privatpersoner eller organisasjoner.
     * NOTAT brukes for dokumentasjon som NAV har produsert selv og uten mål om å distribuere dette ut av NAV. Eksempler på dette er forvaltningsnotater og referater fra telefonsamtaler med brukere.
     */
    enum class JournalPostType {
        INNGAAENDE,
        UTGAAENDE,
        NOTAT,
    }

    /**
     * Ved journalposttype INNGÅENDE skal avsender av dokumentene oppgis.
     * Ved journalposttype UTGÅENDE skal mottaker av dokumentene oppgis.
     * avsenderMottaker skal ikke settes for journalposttype NOTAT.
     */
    data class AvsenderMottaker(
        val id: String,
        val idType: String = "FNR",
    )

    sealed class Sak {
        data class Fagsak(
            /**
             * Iden til fagsaken i fagsystemet (altså ikke applikasjonen SAK).
             * Skal kun settes dersom sakstype = FAGSAK
             */
            val fagsakId: String,
            val fagsaksystem: String = "TILTAKSPENGER",
            /**
             * FAGSAK vil si at dokumentene tilhører en sak i et fagsystem. Dersom FAGSAK velges, må fagsakid og fagsaksystem oppgis.
             * GENERELL_SAK skal kun brukes for dokumenter som ikke tilhører en konkret fagsak i et fagsystem. Generell sak kan ses på som brukerens "mappe" på et gitt tema.
             */
            val sakstype: String = "FAGSAK",
        ) : Sak()
    }

    data class Bruker(
        val id: String,
        val idType: String = "FNR",
    )

    data class JournalpostDokument(
        /**
         * Dokumentets tittel, f.eks. "Søknad om foreldrepenger ved fødsel" eller "Legeerklæring".
         * Dokumentets tittel blir synlig i brukers journal på nav.no, samt i NAVs fagsystemer.
         */
        val tittel: String,
        /**
         * Kode som sier noe om dokumentets innhold og oppbygning. Brevkode bør settes for alle journalposttyper, og brukes blant annet for statistikk.
         * For inngående dokumenter kan brevkoden for eksempel være en NAV-skjemaID f.eks. "NAV 14-05.09" eller en SED-id.
         * For utgående dokumenter og notater er det systemet som produserer dokumentet som bestemmer hva brevkoden skal være. Om fagsystemet har "malkoder" kan man gjerne bruke disse som brevkode.
         */
        val brevkode: String,
        val dokumentvarianter: List<DokumentVariant>,
    ) {
        /**
         * Alle variantene av et enkeltdokument som skal arkiveres.
         */
        sealed class DokumentVariant {
            /**
             * Filtypen til filen som følger, feks PDF/A, JSON eller XML.
             * Se Filtype for en oversikt over gyldige verdier
             */
            abstract val filtype: String

            /** base64Binary - Selve dokumentet */
            abstract val fysiskDokument: String

            /**
             * Typen variant som arkiveres. Se Variantformat for en oversikt over gyldige verdier.
             * ARKIV-varianten vil være den som vises frem til bruker i Gosys og på nav.no. Alle dokumenter som arkiveres må ha én variant med variantformat ARKIV. Variantformat ARKIV skal ha filtype PDF eller PDFA (helst)
             * ORIGINAL skal brukes for dokumentvariant i maskinlesbart format (for eksempel XML og JSON) som brukes for automatisk saksbehandling
             */
            abstract val variantformat: String

            /**
             * Navnet filen skal ha i arkivet. Brukes for sporingsformål ved arkivering av skannede dokumenter.
             */
            abstract val filnavn: String

            data class ArkivPDF(
                override val fysiskDokument: String,
                val tittel: String,
            ) : DokumentVariant() {
                override val filtype: String = "PDFA"
                override val variantformat: String = "ARKIV"
                override val filnavn: String = "$tittel.pdf"
            }

            data class VedleggPDF(
                override val fysiskDokument: String,
                override val filnavn: String,
            ) : DokumentVariant() {
                override val filtype: String = "PDFA"
                override val variantformat: String = "ARKIV"
            }

            data class OriginalJson(
                override val fysiskDokument: String,
                val tittel: String,
            ) : DokumentVariant() {
                override val filtype: String = "JSON"
                override val variantformat: String = "ORIGINAL"
                override val filnavn: String = "$tittel.json"
            }
        }
    }
}
