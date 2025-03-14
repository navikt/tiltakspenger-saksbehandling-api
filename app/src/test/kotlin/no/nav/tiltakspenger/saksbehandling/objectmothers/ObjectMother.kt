package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.saksbehandling.felles.Navkontor
import no.nav.tiltakspenger.saksbehandling.felles.OppgaveId
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.personopplysninger.Navn
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import java.time.Instant

object ObjectMother :
    SaksbehandlerMother,
    SystembrukerMother,
    SøknadMother,
    BehandlingMother,
    SakMother,
    PersonMother,
    MeldekortMother,
    UtbetalingsvedtakMother,
    RammevedtakMother,
    TiltakMother,
    SaksopplysningerMother,
    BarnetilleggMother {
    val saksId = SakId.random()
    val saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1111")
    val fnr = Fnr.random()

    fun navn() = Navn("Fornavn", "Mellomnavn", "Etternavn")
    fun navkontor() = Navkontor(kontornummer = "0220", kontornavn = "Nav Asker")
    fun oppgaveId(oppgaveId: OppgaveId = OppgaveId("100")) = oppgaveId
    fun accessToken(
        token: String = "token",
        expiresAt: Instant = Instant.MAX,
        invaliderCache: () -> Unit = {},
    ) = AccessToken(
        token = token,
        expiresAt = expiresAt,
        invaliderCache = invaliderCache,
    )
}
