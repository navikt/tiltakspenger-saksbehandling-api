package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.felles.Systembrukerrolle
import no.nav.tiltakspenger.saksbehandling.felles.Systembrukerroller

interface SystembrukerMother {
    fun systembrukerHentEllerOpprettSakOgLagreSoknad(
        klientId: String = "klientId",
        klientnavn: String = "klientnavn",
    ) = Systembruker(
        roller = Systembrukerroller(
            nonEmptyListOf(
                Systembrukerrolle.HENT_ELLER_OPPRETT_SAK,
                Systembrukerrolle.LAGRE_SOKNAD,
            ),
        ),
        klientId = klientId,
        klientnavn = klientnavn,
    )

    fun systembrukerHentEllerOpprettSak(
        klientId: String = "klientId",
        klientnavn: String = "klientnavn",
    ) = Systembruker(
        roller = Systembrukerroller(nonEmptyListOf(Systembrukerrolle.HENT_ELLER_OPPRETT_SAK)),
        klientId = klientId,
        klientnavn = klientnavn,
    )

    fun systembrukerLagreSoknad(
        klientId: String = "klientId",
        klientnavn: String = "klientnavn",
    ) = Systembruker(
        roller = Systembrukerroller(nonEmptyListOf(Systembrukerrolle.LAGRE_SOKNAD)),
        klientId = klientId,
        klientnavn = klientnavn,
    )

    fun systembrukerLagreMeldekort(
        klientId: String = "klientId",
        klientnavn: String = "klientnavn",
    ) = Systembruker(
        roller = Systembrukerroller(nonEmptyListOf(Systembrukerrolle.LAGRE_MELDEKORT)),
        klientId = klientId,
        klientnavn = klientnavn,
    )
}
