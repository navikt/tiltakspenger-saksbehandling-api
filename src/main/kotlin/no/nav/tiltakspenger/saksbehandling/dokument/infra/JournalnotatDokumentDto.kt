package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.journalnotat.Journalnotat
import no.nav.tiltakspenger.saksbehandling.journalnotat.infra.JOURNALNOTAT_TITTEL
import no.nav.tiltakspenger.saksbehandling.journalnotat.infra.tilBegrunnelseTittel
import no.nav.tiltakspenger.saksbehandling.person.Navn

/**
 * Payload til journalnotat-malen i pdfgenrs (templates/tpts/journalnotat.typ).
 */
data class JournalnotatDokumentDto(
    val personalia: BrevPersonaliaDTO,
    val saksnummer: String,
    val tittel: String,
    val notatsdato: String,
    val saksbehandlerNavn: String,
    val beslutterNavn: String,
    val begrunnelseTittel: String,
    val begrunnelse: String,
)

suspend fun Journalnotat.tilJournalnotatDokumentJson(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    return JournalnotatDokumentDto(
        personalia = BrevPersonaliaDTO(
            ident = fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
        ),
        saksnummer = saksnummer.verdi,
        tittel = JOURNALNOTAT_TITTEL,
        notatsdato = notatsdato.format(norskDatoFormatter),
        saksbehandlerNavn = hentSaksbehandlersNavn(saksbehandler),
        beslutterNavn = hentSaksbehandlersNavn(beslutter),
        begrunnelseTittel = vedtakstype.tilBegrunnelseTittel(),
        begrunnelse = begrunnelse.verdi,
    ).let { serialize(it) }
}
