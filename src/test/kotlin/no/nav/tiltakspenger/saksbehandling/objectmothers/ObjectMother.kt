package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.FnrGenerator
import no.nav.tiltakspenger.saksbehandling.sak.delteSaksnummerGenerator
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration

/**
 * Delt instans slik at alle fnr fra [ObjectMother.gyldigFnr] er unike på tvers av hele testkjøringen.
 * Skal ikke ha flere instanser; to generatorer med samme startverdi gir samme sekvens og dermed kollisjoner.
 */
private val fnrGenerator = FnrGenerator()

/**
 * test-data instanser vi vil skal deles på tvers av test-interfacene våre
 */
interface MotherOfAllMothers {
    /**
     * Ny tikkende klokke per oppslag: ingen delt klokketilstand mellom tester.
     * En test som trenger monotone tidsstempler på tvers av flere mother-kall må lage sin egen [no.nav.tiltakspenger.libs.common.TikkendeKlokke] og sende den inn.
     */
    val clock: Clock get() = TikkendeKlokke()

    /** Nytt, unikt saksnummer per kall via [no.nav.tiltakspenger.saksbehandling.sak.delteSaksnummerGenerator]; saksnummer har unik indeks i sak-tabellen. */
    fun nesteSaksnummer(): Saksnummer = delteSaksnummerGenerator.generer()
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
    /**
     * Genererer et nytt, unikt fnr per kall (trådsikkert).
     * Unikheten gjør at tester ikke deler person, og dermed kan kjøre mot samme skjema uten å rydde mellom seg.
     * Fnr valideres ikke utover 11 siffer i testene; alt utenfor er faket.
     */
    fun gyldigFnr(): Fnr = fnrGenerator.generer()

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
