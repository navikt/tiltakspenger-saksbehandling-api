package no.nav.tiltakspenger.saksbehandling.domene.personopplysninger

import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.personopplysninger.Navn
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

internal class NavnTest {

    val fornavn = "Fornavn"
    val mellomnavn = "Mellomnavn"
    val etternavn = "Etternavn"

    @Test
    fun `mellomnavn blir ikke med om det er null`() {
        val navnUtenMellomnavn = Navn(fornavn = fornavn, etternavn = etternavn)
        assertTrue { navnUtenMellomnavn.mellomnavnOgEtternavn == etternavn }

        val navnMedMellomnavn = Navn(fornavn = fornavn, mellomnavn = mellomnavn, etternavn = etternavn)
        assertTrue { navnMedMellomnavn.mellomnavnOgEtternavn == "$mellomnavn $etternavn" }
    }
}
