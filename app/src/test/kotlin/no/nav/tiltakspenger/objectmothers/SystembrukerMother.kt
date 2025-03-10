package no.nav.tiltakspenger.objectmothers

import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.felles.Systembrukerrolle
import no.nav.tiltakspenger.saksbehandling.felles.Systembrukerroller

interface SystembrukerMother {
    fun systembrukerHenteData(
        klientId: String = "klientId",
        klientnavn: String = "klientnavn",
    ) = Systembruker(
        roller = Systembrukerroller(nonEmptyListOf(Systembrukerrolle.HENTE_DATA)),
        klientId = klientId,
        klientnavn = klientnavn,
    )

    fun systembrukerLageHendelser(
        klientId: String = "klientId",
        klientnavn: String = "klientnavn",
    ) = Systembruker(
        roller = Systembrukerroller(nonEmptyListOf(Systembrukerrolle.LAGE_HENDELSER)),
        klientId = klientId,
        klientnavn = klientnavn,
    )
}
