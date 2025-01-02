package no.nav.tiltakspenger.vedtak.clients.pdfgen

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.Navn
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.vedtak.clients.pdfgen.formattering.norskDatoFormatter
import java.time.LocalDate

@Suppress("unused")
private class BrevRevurderingStansDTO(
    val personalia: BrevPersonaliaDTO,
    val saksnummer: String,
    val datoForUtsending: String,
    val tiltaksnavn: String,
    val barnetillegg: Boolean = false,
    val rammevedtakFraDato: String,
    val rammevedtakTilDato: String,
    val kontor: String,
    val beslutterNavn: String,
    val saksbehandlerNavn: String,
)

internal suspend fun Rammevedtak.toRevurderingStans(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = hentSaksbehandlersNavn(beslutterNavIdent)

    return BrevRevurderingStansDTO(
        personalia = BrevPersonaliaDTO(
            ident = this.fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
            antallBarn = 0,
        ),
        tiltaksnavn = this.behandling.tiltaksnavn,
        rammevedtakFraDato = periode.fraOgMed.format(norskDatoFormatter),
        rammevedtakTilDato = periode.tilOgMed.format(norskDatoFormatter),
        saksnummer = saksnummer.verdi,
        barnetillegg = false,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        // TODO post-mvp: legg inn NORG integrasjon for å hente saksbehandlers kontor.
        kontor = "Nav Tiltak Øst-Viken",
        // Dette er vår dato, det brukes typisk når bruker klager på vedtaksbrev på dato ...
        datoForUtsending = vedtaksdato.format(norskDatoFormatter),
    ).let { serialize(it) }
}
