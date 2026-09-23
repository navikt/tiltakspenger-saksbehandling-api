package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import java.util.UUID

class InternOppgaveIdTest {
    @Test
    fun `tilfeldige ider er unike og kan leses tilbake fra streng`() {
        val id = InternOppgaveId.random()

        id shouldNotBe InternOppgaveId.random()
        id.toString() shouldStartWith "internoppgave_"
        InternOppgaveId.fromString(id.toString()) shouldBe id
        InternOppgaveId.fromUUID(id.uuid()) shouldBe id
    }

    @Test
    fun `uuid kan konverteres til og fra oppgaveid`() {
        val uuid = UUID.fromString("01945ed3-3200-7000-8000-000000000001")
        val id = InternOppgaveId.fromUUID(uuid)

        id.uuid() shouldBe uuid
        InternOppgaveId.fromString(id.toString()) shouldBe id
        InternOppgaveId.fromUUID(id.uuid()) shouldBe id
    }

    @Test
    fun `strengformatet har stabilt prefiks og ulid`() {
        val streng = "internoppgave_00000000000000000000000000"

        InternOppgaveId.fromString(streng).toString() shouldBe streng
        InternOppgaveId.fromUUID(UUID(0, 0)).toString() shouldBe streng
    }

    @Test
    fun `avviser feil prefiks og ugyldig ulid`() {
        listOf(
            "sak_00000000000000000000000000",
            "internoppgavefeil_00000000000000000000000000",
            "internoppgave",
            "internoppgave_",
            "internoppgave_ugyldig",
        ).forEach {
            shouldThrow<IllegalArgumentException> { InternOppgaveId.fromString(it) }
        }
    }
}
