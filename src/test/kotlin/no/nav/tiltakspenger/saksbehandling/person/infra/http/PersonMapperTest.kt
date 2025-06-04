package no.nav.tiltakspenger.saksbehandling.person.infra.http

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.person.PersonopplysningerSøker
import no.nav.tiltakspenger.saksbehandling.person.infra.http.mapPersonopplysninger
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PersonMapperTest {
    @Test
    fun `serialisering av barn med manglende ident`() {
        val fnr = Fnr.random()
        mapPersonopplysninger(
            json = pdlResponseManglendeIdentPåBarn,
            fnr = fnr,
        ).also {
            it shouldBe
                PersonopplysningerSøker(
                    fnr = fnr,
                    fødselsdato = LocalDate.of(1984, 7, 4),
                    fornavn = "Lykkelig",
                    mellomnavn = null,
                    etternavn = "Eksamen",
                    fortrolig = false,
                    strengtFortrolig = false,
                    strengtFortroligUtland = false,
                )
        }
    }
}
