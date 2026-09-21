package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import io.kotest.matchers.string.shouldNotContain
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.personopplysning.Organisasjonsnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Samhandlerident
import no.nav.tiltakspenger.libs.common.random
import org.junit.jupiter.api.Test

class AktørTest {
    @Test
    fun `toString viser ikke identen`() {
        val fnr = Fnr.random()

        Aktør.Person(fnr).toString() shouldNotContain fnr.verdi
        Aktør.Organisasjon(Organisasjonsnummer("999111222")).toString() shouldNotContain "999111222"
        Aktør.Samhandler(Samhandlerident("80912345678")).toString() shouldNotContain "80912345678"
    }
}
