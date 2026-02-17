package no.nav.tiltakspenger.saksbehandling.dokument.infra

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.dokument.TittelOgTekstDTO
import no.nav.tiltakspenger.saksbehandling.dokument.toBrevDtoList
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDate

data class BrevKlageInnstillingDTO private constructor(
    val personalia: BrevPersonaliaDTO,
    val saksnummer: String,
    val saksbehandlerNavn: String,
    val datoForUtsending: String,
    val tilleggstekst: List<TittelOgTekstDTO>,
    val forhandsvisning: Boolean,
    val vedtaksdato: String,
    val innsendingsdato: String,
) {
    @JsonInclude
    val kontor: String = "Nav Tiltakspenger"

    companion object {
        suspend fun create(
            tilleggstekst: Brevtekster,
            hentBrukersNavn: suspend (Fnr) -> Navn,
            hentSaksbehandlersNavn: suspend (String) -> String,
            saksbehandlerNavIdent: String,
            saksnummer: Saksnummer,
            forhåndsvisning: Boolean,
            datoForUtsending: LocalDate,
            fnr: Fnr,
            vedtaksdato: LocalDate,
            innsendingsdato: LocalDate,
        ): String {
            val brukersNavn = hentBrukersNavn(fnr)
            val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
            return BrevKlageInnstillingDTO(
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
                vedtaksdato = vedtaksdato.format(norskDatoFormatter),
                innsendingsdato = innsendingsdato.format(norskDatoFormatter),
            ).let {
                serialize(it)
            }
        }
    }
}
