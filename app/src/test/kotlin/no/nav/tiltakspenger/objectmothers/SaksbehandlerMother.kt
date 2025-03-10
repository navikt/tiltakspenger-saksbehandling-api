package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.vedtak.felles.Systembrukerroller

interface SaksbehandlerMother {
    fun saksbehandler(
        navIdent: String = "Z12345",
        brukernavn: String = "Sak Behandler",
        epost: String = "Sak.Behandler@nav.no",
        roller: Saksbehandlerroller = Saksbehandlerroller(listOf(Saksbehandlerrolle.SAKSBEHANDLER)),
        klientId: String = "klientId",
        klientnavn: String = "klientnavn",
        @Suppress("UNCHECKED_CAST") scopes: GenerellSystembrukerroller<GenerellSystembrukerrolle> = Systembrukerroller(
            emptySet(),
        ) as GenerellSystembrukerroller<GenerellSystembrukerrolle>,
    ) = Saksbehandler(
        navIdent = navIdent,
        brukernavn = brukernavn,
        epost = epost,
        roller = roller,
        klientId = klientId,
        klientnavn = klientnavn,
        scopes = scopes,
    )

    fun saksbehandlerUtenTilgang(
        navIdent: String = "U12345",
        brukernavn: String = "Sak Behandler",
        epost: String = "Sak.Behandler@nav.no",
    ) = saksbehandler(
        roller = Saksbehandlerroller(emptyList()),
        navIdent = navIdent,
        epost = epost,
        brukernavn = brukernavn,
    )

    fun beslutter(
        navIdent: String = "B12345",
        brukernavn: String = "Sak Behandler",
        epost: String = "Sak.Behandler@nav.no",
    ) = saksbehandler(
        roller = Saksbehandlerroller(listOf(Saksbehandlerrolle.BESLUTTER)),
        navIdent = navIdent,
        epost = epost,
        brukernavn = brukernavn,
    )

    fun saksbehandlerMedKode6(
        navIdent: String = "Z12345",
        brukernavn: String = "Sak Behandler",
        epost: String = "Sak.Behandler@nav.no",
        klientId: String = "klientId",
        klientnavn: String = "klientnavn",
        @Suppress("UNCHECKED_CAST") scopes: GenerellSystembrukerroller<GenerellSystembrukerrolle> = Systembrukerroller(
            emptySet(),
        ) as GenerellSystembrukerroller<GenerellSystembrukerrolle>,
    ) = Saksbehandler(
        navIdent = navIdent,
        epost = epost,
        brukernavn = brukernavn,
        roller = Saksbehandlerroller(
            listOf(
                Saksbehandlerrolle.SAKSBEHANDLER,
                Saksbehandlerrolle.STRENGT_FORTROLIG_ADRESSE,
            ),
        ),
        klientId = klientId,
        klientnavn = klientnavn,
        scopes = scopes,
    )

    fun saksbehandlerMedKode7(
        navIdent: String = "Z12345",
        brukernavn: String = "Sak Behandler",
        epost: String = "Sak.Behandler@nav.no",
        klientId: String = "klientId",
        klientnavn: String = "klientnavn",
        @Suppress("UNCHECKED_CAST") scopes: GenerellSystembrukerroller<GenerellSystembrukerrolle> = Systembrukerroller(
            emptySet(),
        ) as GenerellSystembrukerroller<GenerellSystembrukerrolle>,
    ) = Saksbehandler(
        navIdent = navIdent,
        epost = epost,
        brukernavn = brukernavn,
        roller = Saksbehandlerroller(listOf(Saksbehandlerrolle.SAKSBEHANDLER, Saksbehandlerrolle.FORTROLIG_ADRESSE)),
        klientId = klientId,
        klientnavn = klientnavn,
        scopes = scopes,
    )

    fun saksbehandler123(
        klientId: String = "klientId",
        klientnavn: String = "klientnavn",
        @Suppress("UNCHECKED_CAST") scopes: GenerellSystembrukerroller<GenerellSystembrukerrolle> = Systembrukerroller(
            emptySet(),
        ) as GenerellSystembrukerroller<GenerellSystembrukerrolle>,
    ): Saksbehandler =
        Saksbehandler(
            navIdent = "123",
            brukernavn = "Test Testesen",
            epost = "Test.Testesen@nav.no",
            roller = Saksbehandlerroller(listOf(Saksbehandlerrolle.SAKSBEHANDLER)),
            klientId = klientId,
            klientnavn = klientnavn,
            scopes = scopes,
        )
}
