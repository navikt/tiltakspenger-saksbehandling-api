package no.nav.tiltakspenger.saksbehandling.dokument.infra

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.dokument.TittelOgTekstDTO
import no.nav.tiltakspenger.saksbehandling.dokument.toDto
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.person.Navn
import java.time.LocalDate

data class BrevKlageForvaltningsmeldingDTO private constructor(
    val personalia: BrevPersonaliaDTO,
    val saksnummer: String,
    val saksbehandlerNavn: String?,
    val datoForUtsending: String,
    val tilleggstekst: List<TittelOgTekstDTO>,
    val forhandsvisning: Boolean,
    val innsendingsdato: String,
) {
    @JsonInclude
    companion object {
        suspend fun create(
            tilleggstekst: Brevtekster,
            hentBrukersNavn: suspend (Fnr) -> Navn,
            hentSaksbehandlersNavn: suspend (String) -> String,
            saksbehandlerNavIdent: String?,
            saksnummer: Saksnummer,
            forhåndsvisning: Boolean,
            datoForUtsending: LocalDate,
            fnr: Fnr,
            innsendingsdato: LocalDate,
        ): String {
            val brukersNavn = hentBrukersNavn(fnr)
            val saksbehandlersNavn = saksbehandlerNavIdent?.let { hentSaksbehandlersNavn(it) }
            return BrevKlageForvaltningsmeldingDTO(
                personalia = BrevPersonaliaDTO(
                    ident = fnr.verdi,
                    fornavn = brukersNavn.fornavn,
                    etternavn = brukersNavn.mellomnavnOgEtternavn,
                ),
                saksnummer = saksnummer.verdi,
                tilleggstekst = tilleggstekst.toDto(),
                forhandsvisning = forhåndsvisning,
                saksbehandlerNavn = saksbehandlersNavn,
                datoForUtsending = datoForUtsending.format(norskDatoFormatter),
                innsendingsdato = innsendingsdato.format(norskDatoFormatter),
            ).let {
                serialize(it)
            }
        }
    }
}
