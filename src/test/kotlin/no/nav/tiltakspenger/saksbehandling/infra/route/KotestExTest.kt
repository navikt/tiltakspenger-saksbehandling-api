package no.nav.tiltakspenger.saksbehandling.infra.route

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import io.kotest.assertions.throwables.shouldThrow
import no.nav.tiltakspenger.libs.common.Fnr
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KotestExTest {
    private val tid1 = LocalDateTime.parse("2026-03-27T10:15:30")
    private val tid2 = LocalDateTime.parse("2026-03-28T10:15:30")
    private val fnr1 = Fnr.fromString("12345678901")
    private val fnr2 = Fnr.fromString("10987654321")

    @Test
    fun `nullable NonEmptyList med like og ulike LocalDateTime - positiv`() {
        assertEqual(
            nullableList(entries = nonEmptyListOf(entry(opprettet = tid1))),
            nullableList(entries = nonEmptyListOf(entry(opprettet = tid1))),
        )
        assertEqual(
            nullableList(entries = nonEmptyListOf(entry(opprettet = tid1))),
            nullableList(entries = nonEmptyListOf(entry(opprettet = tid2))),
        )
    }

    @Test
    fun `nullable NonEmptyList med en nullable localdatetime - negativ`() {
        assertNotEqual(
            nullableList(entries = nonEmptyListOf(entry(opprettet = tid1))),
            nullableList(entries = nonEmptyListOf(entry(opprettet = null))),
        )
        assertNotEqual(
            nullableList(entries = nonEmptyListOf(entry(opprettet = null))),
            nullableList(entries = nonEmptyListOf(entry(opprettet = tid1))),
        )
    }

    @Test
    fun `nullable NonEmptyList med null - positiv`() {
        assertEqual(nullableList(entries = null), nullableList(entries = null))
    }

    @Test
    fun `nullable NonEmptyList med ulik id - negativ`() {
        assertNotEqual(nullableList(id = "a", entries = null), nullableList(id = "b", entries = null))
    }

    @Test
    fun `nullable NonEmptyList med ulik nullstatus - negativ`() {
        assertNotEqual(nullableList(entries = null), nullableList(entries = nonEmptyListOf(entry())))
    }

    @Test
    fun `liste - positiv`() {
        assertEqual(
            listWrapper(entries = listOf(entry(opprettet = tid1))),
            listWrapper(entries = listOf(entry(opprettet = tid2))),
        )
    }

    @Test
    fun `liste - negativ`() {
        assertNotEqual(
            listWrapper(entries = listOf(entry(navn = "a"))),
            listWrapper(entries = listOf(entry(navn = "b"))),
        )
    }

    @Test
    fun `set - positiv`() {
        assertEqual(
            setWrapper(entries = setOf(entry("a", tid1), entry("b", tid1))),
            setWrapper(entries = setOf(entry("b", tid2), entry("a", tid2))),
        )
    }

    @Test
    fun `set - negativ`() {
        assertNotEqual(
            setWrapper(entries = setOf(entry("a", tid1), entry("b", tid1))),
            setWrapper(entries = setOf(entry("a", tid2), entry("c", tid2))),
        )
    }

    @Test
    fun `map - positiv`() {
        assertEqual(
            mapWrapper(entries = mapOf("a" to entry(opprettet = tid1))),
            mapWrapper(entries = mapOf("a" to entry(opprettet = tid2))),
        )
    }

    @Test
    fun `map - negativ`() {
        assertNotEqual(
            mapWrapper(entries = mapOf("a" to entry(navn = "a"))),
            mapWrapper(entries = mapOf("a" to entry(navn = "b"))),
        )
    }

    @Test
    fun `private felt - positiv`() {
        assertEqual(
            privateFieldWrapper(hidden = entry(opprettet = tid1)),
            privateFieldWrapper(hidden = entry(opprettet = tid2)),
        )
    }

    @Test
    fun `private felt - negativ`() {
        assertNotEqual(privateFieldWrapper(hidden = entry(navn = "a")), privateFieldWrapper(hidden = entry(navn = "b")))
    }

    @Test
    fun `private data class - positiv`() {
        assertEqual(privateWrapper(entry = entry(opprettet = tid1)), privateWrapper(entry = entry(opprettet = tid2)))
    }

    @Test
    fun `private data class - negativ`() {
        assertNotEqual(privateWrapper(entry = entry(navn = "a")), privateWrapper(entry = entry(navn = "b")))
    }

    @Test
    fun `public fnr - positiv`() {
        assertEqual(publicFnrWrapper(fnr = fnr1), publicFnrWrapper(fnr = fnr1))
    }

    @Test
    fun `public fnr - negativ`() {
        assertNotEqual(publicFnrWrapper(fnr = fnr1), publicFnrWrapper(fnr = fnr2))
    }

    @Test
    fun `private fnr - positiv`() {
        assertEqual(privateFnrWrapper(fnr = fnr1), privateFnrWrapper(fnr = fnr1))
    }

    @Test
    fun `private fnr - negativ`() {
        assertNotEqual(privateFnrWrapper(fnr = fnr1), privateFnrWrapper(fnr = fnr2))
    }

    @Test
    fun `nested public fnr - positiv`() {
        assertEqual(
            routeLikeFnrWrapper(fnr = fnr1, behandlingFnr = fnr1),
            routeLikeFnrWrapper(fnr = fnr1, behandlingFnr = fnr1),
        )
    }

    @Test
    fun `nested public fnr - negativ`() {
        assertNotEqual(
            routeLikeFnrWrapper(fnr = fnr1, behandlingFnr = fnr1),
            routeLikeFnrWrapper(fnr = fnr2, behandlingFnr = fnr2),
        )
    }

    @Test
    fun `nested private fnr - positiv`() {
        assertEqual(
            privateRouteLikeFnrWrapper(fnr = fnr1, behandlingFnr = fnr1),
            privateRouteLikeFnrWrapper(fnr = fnr1, behandlingFnr = fnr1),
        )
    }

    @Test
    fun `nested private fnr - negativ`() {
        assertNotEqual(
            privateRouteLikeFnrWrapper(fnr = fnr1, behandlingFnr = fnr1),
            privateRouteLikeFnrWrapper(fnr = fnr2, behandlingFnr = fnr2),
        )
    }

    private fun entry(
        navn: String = "første",
        opprettet: LocalDateTime? = tid1,
    ) = TestEntry(navn = navn, opprettet = opprettet)

    private fun nullableList(
        id: String = "id",
        entries: NonEmptyList<TestEntry>? = nonEmptyListOf(entry()),
    ) = TestWrapper(id = id, entries = entries)

    private fun listWrapper(entries: List<TestEntry> = listOf(entry())) = ListWrapper(entries = entries)

    private fun setWrapper(entries: Set<TestEntry> = setOf(entry())) = SetWrapper(entries = entries)

    private fun mapWrapper(entries: Map<String, TestEntry> = mapOf("a" to entry())) = MapWrapper(entries = entries)

    private fun privateFieldWrapper(
        id: String = "id",
        hidden: TestEntry = entry(),
    ) = PrivateFieldWrapper(id = id, hidden = hidden)

    private fun publicFnrWrapper(
        id: String = "id",
        fnr: Fnr = fnr1,
    ) = PublicFnrWrapper(id = id, fnr = fnr)

    private fun privateFnrWrapper(
        id: String = "id",
        fnr: Fnr = fnr1,
    ) = PrivateFnrWrapper(id = id, fnr = fnr)

    private fun routeLikeFnrWrapper(
        fnr: Fnr = fnr1,
        behandlingFnr: Fnr = fnr1,
    ) = RouteLikeFnrWrapper(
        fnr = fnr,
        behandling = BehandlingMedFnr(fnr = behandlingFnr),
    )

    private fun privateRouteLikeFnrWrapper(
        fnr: Fnr = fnr1,
        behandlingFnr: Fnr = fnr1,
    ) = PrivateRouteLikeFnrWrapper(
        fnr = fnr,
        behandling = PrivateBehandlingMedFnr(fnr = behandlingFnr),
    )

    private fun privateWrapper(entry: TestEntry = this.entry()) = PrivateWrapper(entry = entry)

    private fun <T : Any> assertEqual(
        actual: T,
        expected: T,
    ) {
        actual.shouldBeEqualToIgnoringLocalDateTime(expected)
    }

    private fun <T : Any> assertNotEqual(
        actual: T,
        expected: T,
    ) {
        shouldThrow<AssertionError> {
            actual.shouldBeEqualToIgnoringLocalDateTime(expected)
        }
    }
}

data class TestWrapper(
    val id: String,
    val entries: NonEmptyList<TestEntry>?,
)

data class ListWrapper(
    val entries: List<TestEntry>,
)

data class SetWrapper(
    val entries: Set<TestEntry>,
)

data class MapWrapper(
    val entries: Map<String, TestEntry>,
)

data class PrivateFieldWrapper(
    val id: String,
    private val hidden: TestEntry,
)

data class TestEntry(
    val navn: String,
    val opprettet: LocalDateTime?,
)

private data class PrivateWrapper(
    val entry: TestEntry,
)

data class PublicFnrWrapper(
    val id: String,
    val fnr: Fnr,
)

data class PrivateFnrWrapper(
    val id: String,
    private val fnr: Fnr,
)

data class RouteLikeFnrWrapper(
    val fnr: Fnr,
    val behandling: BehandlingMedFnr,
)

data class BehandlingMedFnr(
    val fnr: Fnr,
)

private data class PrivateRouteLikeFnrWrapper(
    private val fnr: Fnr,
    private val behandling: PrivateBehandlingMedFnr,
)

private data class PrivateBehandlingMedFnr(
    private val fnr: Fnr,
)
