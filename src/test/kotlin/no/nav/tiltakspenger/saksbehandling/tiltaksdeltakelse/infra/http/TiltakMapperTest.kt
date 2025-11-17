package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

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
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseMedArrangørnavn(
                arrangørnavn = arrangørnavn,
                harAdressebeskyttelse = false,
            )
            assertEquals(arrangørnavn, tiltaksdeltakelse.arrangørnavn, "arrangørnavn")
        }

        @Test
        fun `fjerner arrangørens navn om personen har adressebeskyttelse`() {
            val arrangørnavn = "Arrangør i Gjøvik AS"
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseMedArrangørnavn(
                arrangørnavn = arrangørnavn,
                harAdressebeskyttelse = true,
            )
            assertNull(tiltaksdeltakelse.arrangørnavn, "arrangørnavn")
        }
    }
}
