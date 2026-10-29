package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.journalnotat.Journalnotat
import no.nav.tiltakspenger.saksbehandling.person.Navn
import java.time.LocalDate

/**
 * Payload til journalnotat-malen i pdfgenrs (templates/tpts/journalnotat.typ).
 * Malen setter selv tittelen på notatet og overskriften for hver begrunnelse ut fra typen.
 */
data class JournalnotatDokumentDto(
    val personalia: BrevPersonaliaDTO,
    val saksnummer: String,
    val notatsdato: LocalDate,
    val saksbehandlerNavn: String,
    val beslutterNavn: String,
    val begrunnelser: List<BegrunnelseDto>,
) {
    data class BegrunnelseDto(
        val type: BegrunnelsestypeDto,
        val begrunnelse: String,
    )

    enum class BegrunnelsestypeDto {
        VILKÅRSVURDERING,
        BARNETILLEGG,
        MELDEKORT,
    }
}

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
        notatsdato = notatsdato,
        saksbehandlerNavn = hentSaksbehandlersNavn(saksbehandler),
        beslutterNavn = hentSaksbehandlersNavn(beslutter),
        begrunnelser = begrunnelser.map {
            JournalnotatDokumentDto.BegrunnelseDto(
                type = it.type.tilDto(),
                begrunnelse = it.begrunnelse.verdi,
            )
        },
    ).let { serialize(it) }
}

private fun Journalnotat.Begrunnelsestype.tilDto(): JournalnotatDokumentDto.BegrunnelsestypeDto = when (this) {
    Journalnotat.Begrunnelsestype.VILKÅRSVURDERING -> JournalnotatDokumentDto.BegrunnelsestypeDto.VILKÅRSVURDERING
    Journalnotat.Begrunnelsestype.BARNETILLEGG -> JournalnotatDokumentDto.BegrunnelsestypeDto.BARNETILLEGG
    Journalnotat.Begrunnelsestype.MELDEKORT -> JournalnotatDokumentDto.BegrunnelsestypeDto.MELDEKORT
}
