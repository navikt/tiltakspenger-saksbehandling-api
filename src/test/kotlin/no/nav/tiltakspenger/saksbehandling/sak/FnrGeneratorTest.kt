package no.nav.tiltakspenger.saksbehandling.sak

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FnrGeneratorTest {

    @Test
    fun `ingen duplikater ved parallell generering`() = runBlocking {
        val generator = FnrGenerator()
        val antall = 1000

        val fnrs = (1..antall)
            .map { async(Dispatchers.Default) { generator.generer() } }
            .awaitAll()

        assertEquals(antall, fnrs.toSet().size, "Forventet ingen duplikater")
    }
}
