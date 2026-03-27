package no.nav.tiltakspenger.saksbehandling.sak

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SøknadstiltakIdGeneratorTest {

    @Test
    fun `ingen duplikater ved parallell generering`() = runBlocking {
        val generator = SøknadstiltakIdGenerator()
        val antall = 1000

        val ids = (1..antall)
            .map { async(Dispatchers.Default) { generator.generer() } }
            .awaitAll()

        assertEquals(antall, ids.toSet().size, "Forventet ingen duplikater")
    }
}
