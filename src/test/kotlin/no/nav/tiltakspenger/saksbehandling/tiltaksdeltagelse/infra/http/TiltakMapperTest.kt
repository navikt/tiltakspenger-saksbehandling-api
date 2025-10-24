package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http

import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import kotlin.test.assertEquals

class TiltakMapperTest {
    @Nested
    inner class MapTiltakMedArrangørnavn {
        @Test
        fun `viser arrangørens navn`() {
            val arrangørnavn = "Arrangør i Gjøvik AS"
            val tiltaksdeltagelse = ObjectMother.tiltaksdeltagelseMedArrangørnavn(
                arrangørnavn = arrangørnavn,
                harAdressebeskyttelse = false,
            )
            assertEquals(arrangørnavn, tiltaksdeltagelse.arrangørnavn, "arrangørnavn")
        }

        @Test
        fun `fjerner arrangørens navn om personen har adressebeskyttelse`() {
            val arrangørnavn = "Arrangør i Gjøvik AS"
            val tiltaksdeltagelse = ObjectMother.tiltaksdeltagelseMedArrangørnavn(
                arrangørnavn = arrangørnavn,
                harAdressebeskyttelse = true,
            )
            assertNull(tiltaksdeltagelse.arrangørnavn, "arrangørnavn")
        }
    }
}
