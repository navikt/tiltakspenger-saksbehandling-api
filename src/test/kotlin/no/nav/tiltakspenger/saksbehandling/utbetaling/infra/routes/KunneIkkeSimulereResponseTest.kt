package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsfeil
import org.junit.jupiter.api.Test

class KunneIkkeSimulereResponseTest {

    /**
     * Saksbehandler skal få vite hvorfor svaret ikke kunne tolkes, ikke bare at «noe» feilet.
     * 502 fordi feilen ligger i svaret fra oppdragssystemet, ikke i forespørselen eller hos oss.
     */
    @Test
    fun `ugyldig simulering svarer 502 med årsaken i meldingen`() {
        val (status, errorJson) = KunneIkkeSimulere.UgyldigSimulering(
            Simuleringsfeil.GjelderAnnenPerson(sakId = null, saksnummer = null),
        ).tilSimuleringErrorJson()

        status shouldBe HttpStatusCode.BadGateway
        errorJson.kode shouldBe "kunne_ikke_tolke_simulering"
        errorJson.melding shouldContain "gjelder en annen person"
    }
}
