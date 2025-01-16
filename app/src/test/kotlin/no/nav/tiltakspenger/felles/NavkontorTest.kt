package no.nav.tiltakspenger.felles

import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

class NavkontorTest {
    private val kontornavn = "Nav Testheim"

    @Test
    fun `skal ikke kaste `() {
        Navkontor("0000", kontornavn)
        Navkontor("0001", kontornavn)
        Navkontor("1234", kontornavn)
        Navkontor("9999", kontornavn)
    }

    @Test
    fun `Skal kaste dersom for få siffer`() {
        shouldThrow<IllegalArgumentException> {
            Navkontor("123", kontornavn)
        }
    }

    @Test
    fun `Skal kaste dersom for mange siffer`() {
        shouldThrow<IllegalArgumentException> {
            Navkontor("12345", kontornavn)
        }
    }

    @Test
    fun `Skal kaste dersom ulovlige tegn`() {
        shouldThrow<IllegalArgumentException> {
            Navkontor("123-", kontornavn)
        }
        shouldThrow<IllegalArgumentException> {
            Navkontor("123/", kontornavn)
        }
    }

    @Test
    fun `Skal kaste dersom bokstaver`() {
        shouldThrow<IllegalArgumentException> {
            Navkontor("123a", kontornavn)
        }
        shouldThrow<IllegalArgumentException> {
            Navkontor("123A", kontornavn)
        }
    }

    @Test
    fun `Skal kaste med negative`() {
        shouldThrow<IllegalArgumentException> {
            Navkontor("-123", kontornavn)
        }
        shouldThrow<IllegalArgumentException> {
            Navkontor("-1234", kontornavn)
        }
    }
}
