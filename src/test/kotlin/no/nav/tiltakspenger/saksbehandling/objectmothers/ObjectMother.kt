package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.person.Navn
import java.time.Instant
import kotlin.time.Duration

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
    BehandlingRevurderingMother,
    KlagebehandlingMother,
    SakMother,
    PersonMother,
    MeldekortMother,
    MeldekortvedtakMother,
    RammevedtakMother,
    TiltakMother,
    SaksopplysningerMother,
    BarnetilleggMother,
    SimuleringMother,
    UtbetalingMother,
    OppdaterBehandlingKommandoMother,
    InnvilgelsesperioderMother {
    fun gyldigFnr() = Fnr.fromString("12345678911")

    fun navn() = Navn("Fornavn", "Mellomnavn", "Etternavn")

    fun navkontor() = Navkontor(kontornummer = "0220", kontornavn = "Nav Asker")

    fun oppgaveId(oppgaveId: OppgaveId = OppgaveId("100")) = oppgaveId

    fun accessToken(
        token: String = "token",
        expiresAt: Instant = Instant.MAX,
    ) = AccessToken(
        token = token,
        expiresAt = expiresAt,
    )

    /** For tester som trenger en ferdig [HttpKlientError] uten å bygge metadata selv, f.eks. til domenefeiltyper som wrapper den. */
    fun httpKlientUventetStatus(
        statusCode: Int = 500,
        body: String = "feil fra tjenesten",
    ) = HttpKlientError.UventetStatus(
        statusCode = statusCode,
        body = body,
        metadata = HttpKlientMetadata(
            rawRequestString = "POST http://test/endepunkt",
            rawResponseString = body,
            requestHeaders = emptyMap(),
            responseHeaders = emptyMap(),
            statusCode = statusCode,
            attempts = 1,
            attemptDurations = listOf(Duration.ZERO),
            totalDuration = Duration.ZERO,
            tidsstempler = HttpKlientTidsstempler.INGEN,
        ),
    )
}
