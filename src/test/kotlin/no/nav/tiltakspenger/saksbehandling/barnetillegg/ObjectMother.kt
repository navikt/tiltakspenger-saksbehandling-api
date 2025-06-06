package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggMother
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingMother
import no.nav.tiltakspenger.saksbehandling.common.KlokkeMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.person.Navn
import java.time.Instant

/**
 * test-data instanser vi vil skal deles på tvers av test-interfacene våre
 */
interface MotherOfAllMothers {
    val clock get() = KlokkeMother.clock
}

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
    BarnetilleggMother,
    SimuleringMother {
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
