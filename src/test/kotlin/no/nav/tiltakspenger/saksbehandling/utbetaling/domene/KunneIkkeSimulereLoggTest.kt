package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.github.oshai.kotlinlogging.KLogger
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import no.nav.tiltakspenger.libs.httpklient.Tidsgrenser
import no.nav.tiltakspenger.libs.httpklient.UriSynlighet
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.time.Duration.Companion.seconds

class KunneIkkeSimulereLoggTest {

    private val httpKlientError = HttpKlientError.UventetStatus(
        statusCode = 500,
        body = "",
        metadata = HttpKlientMetadata(
            method = "POST",
            uri = URI.create("http://helved.test/simulering"),
            uriSynlighet = UriSynlighet.VanligLogg,
            tidsgrenser = Tidsgrenser(svar = 30.seconds, oppkobling = 10.seconds),
            rawRequestString = "",
            rawResponseString = null,
            requestHeaders = emptyMap(),
            responseHeaders = emptyMap(),
            statusCode = 500,
            attempts = 1,
            attemptDurations = emptyList(),
            totalDuration = kotlin.time.Duration.ZERO,
            tidsstempler = HttpKlientTidsstempler.INGEN,
        ),
    )

    /** Et svar som bryter kontrakten vi tolker det etter skal en utvikler se på, derfor error. */
    @Test
    fun `UgyldigSimulering logges som error med årsaken og typen stemplet på meldingen`() {
        val logger = mockk<KLogger>(relaxed = true)
        val melding = slot<() -> Any?>()

        KunneIkkeSimulere.UgyldigSimulering(
            Simuleringsfeil.GjelderAnnenPerson(sakId = null, saksnummer = null),
        ).logg(logger, "kontekst")

        verify { logger.error(anyNullable<Throwable>(), capture(melding)) }

        melding.captured().toString().let {
            it shouldContain "gjelder en annen person"
            it shouldContain "kontekst"
            it shouldContain "KunneIkkeSimulere.UgyldigSimulering(GjelderAnnenPerson)"
        }
        verify(exactly = 0) { logger.warn(any<() -> Any?>()) }
    }

    /** Stengt er forventet utenfor åpningstidene til OS og skal ikke støye i loggen. */
    @Test
    fun `Stengt logges som debug`() {
        val logger = mockk<KLogger>(relaxed = true)

        KunneIkkeSimulere.Stengt(httpKlientError).logg(logger, "kontekst")

        verify(exactly = 1) { logger.debug(any<() -> Any?>()) }
        verify(exactly = 0) { logger.error(anyNullable<Throwable>(), any<() -> Any?>()) }
    }

    /** Timeout og UkjentFeil delegerer til loggFeil fra httpklient-biblioteket. */
    @Test
    fun `Timeout og UkjentFeil logges via loggFeil`() {
        val logger = mockk<KLogger>(relaxed = true)

        shouldNotThrowAny {
            KunneIkkeSimulere.Timeout(httpKlientError).logg(logger, "kontekst")
            KunneIkkeSimulere.UkjentFeil(httpKlientError).logg(logger, "kontekst")
        }
    }

    @Test
    fun `beskrivelsene av simuleringsfeil er lesbare for saksbehandler`() {
        Simuleringsfeil.JusteringOverMånedsskifte(
            meldeperiodeId = ObjectMother.meldeperiode(periode = Periode(27.januar(2025), 9.februar(2025))).id,
            postering = Periode(27.januar(2025), 2.februar(2025)),
            klassekode = OppsummeringGenerator.KLASSEKODE_JUSTERING,
        ).beskrivelse shouldContain "krysser et månedsskifte"
    }
}
