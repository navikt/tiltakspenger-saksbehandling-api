package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.saksbehandling.klage.domene.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDate

data class BrevKlageAvvisningDTO(
    val personalia: BrevPersonaliaDTO,
    val saksnummer: String,
    val saksbehandlerNavn: String,
    val datoForUtsending: String,
    val tilleggstekst: List<TittelOgTekstDTO>,
    val forhandsvisning: Boolean,
) {
    data class TittelOgTekstDTO(
        val tittel: String,
        val tekst: String,
    )

    companion object {
        suspend fun create(
            tilleggstekst: List<TittelOgTekst>,
            hentBrukersNavn: suspend (Fnr) -> Navn,
            hentSaksbehandlersNavn: suspend (String) -> String,
            saksbehandlerNavIdent: String,
            saksnummer: Saksnummer,
            forhåndsvisning: Boolean,
            datoForUtsending: LocalDate,
            fnr: Fnr,
        ): String {
            val brukersNavn = hentBrukersNavn(fnr)
            val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
            return BrevKlageAvvisningDTO(
                personalia = BrevPersonaliaDTO(
                    ident = fnr.verdi,
                    fornavn = brukersNavn.fornavn,
                    etternavn = brukersNavn.mellomnavnOgEtternavn,
                ),
                saksnummer = saksnummer.verdi,
                tilleggstekst = tilleggstekst.toBrevDtoList(),
                forhandsvisning = forhåndsvisning,
                saksbehandlerNavn = saksbehandlersNavn,
                datoForUtsending = datoForUtsending.format(norskDatoFormatter),
            ).let {
                serialize(it)
            }
        }
    }
}
fun List<TittelOgTekst>.toBrevDtoList(): List<BrevKlageAvvisningDTO.TittelOgTekstDTO> =
    this.map { it.toBrevDto() }

fun TittelOgTekst.toBrevDto(): BrevKlageAvvisningDTO.TittelOgTekstDTO =
    BrevKlageAvvisningDTO.TittelOgTekstDTO(
        tittel = this.tittel.value,
        tekst = this.tekst.value,
    )
