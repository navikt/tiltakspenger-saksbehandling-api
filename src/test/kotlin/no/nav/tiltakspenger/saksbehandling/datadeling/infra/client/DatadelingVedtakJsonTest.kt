package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import org.junit.jupiter.api.Test

class DatadelingVedtakJsonTest {
    @Test
    fun `avslag med grunn InnvilgetTiltakspenger sendes som LIVSOPPHOLDSYTELSER til datadeling`() {
        // TODO: Lag et avslagsvedtak med grunn InnvilgetTiltakspenger, f.eks. med ObjectMother.nyRammevedtakAvslag.
        // Kall toDatadelingJson().
        // Forvent at hjemmelen er LIVSOPPHOLDSYTELSER, siden datadeling ikke kjenner den nye koden.
        TODO("Ikke implementert")
    }
}
