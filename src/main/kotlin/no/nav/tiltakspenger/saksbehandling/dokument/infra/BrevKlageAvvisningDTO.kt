package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.dokument.TittelOgTekstDTO
import no.nav.tiltakspenger.saksbehandling.dokument.toDTO
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDate

data class BrevKlageAvvisningDTO private constructor(
    val personalia: BrevPersonaliaDTO,
    val saksnummer: String,
    val saksbehandlerNavn: String,
    val datoForUtsending: String,
    val tilleggstekst: List<TittelOgTekstDTO>,
    val forhandsvisning: Boolean,
) {
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
                tilleggstekst = tilleggstekst.toDTO(),
                forhandsvisning = forhåndsvisning,
                saksbehandlerNavn = saksbehandlersNavn,
                datoForUtsending = datoForUtsending.format(norskDatoFormatter),
            ).let {
                serialize(it)
            }
        }
    }
}
