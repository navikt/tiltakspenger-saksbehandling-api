package no.nav.tiltakspenger.saksbehandling.person

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import org.junit.jupiter.api.Test

class FødselsnumreTest {
    private val gjeldende = Fnr.random()
    private val tidligere = Fnr.random()

    @Test
    fun `alle har det gjeldende nummeret først`() {
        Fødselsnumre(gjeldende, listOf(tidligere)).alle shouldBe listOf(gjeldende, tidligere)
        Fødselsnumre(gjeldende).alle shouldBe listOf(gjeldende)
    }

    @Test
    fun `det gjeldende nummeret kan ikke også stå blant de tidligere`() {
        shouldThrow<IllegalArgumentException> { Fødselsnumre(gjeldende, listOf(tidligere, gjeldende)) }
    }

    @Test
    fun `de tidligere numrene kan ikke ha duplikater`() {
        shouldThrow<IllegalArgumentException> { Fødselsnumre(gjeldende, listOf(tidligere, tidligere)) }
    }
}
