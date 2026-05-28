package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.KontorType
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.Kontorhistorikkinnslag
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class KontorhistorikkTest {

    @Test
    fun `nyesteAktuelleKontor velger nyeste ARBEIDSOPPFOLGING-innslag når den finnes`() {
        val nyesteArbeidsoppfolging =
            arbeidsoppfolgingInnslag("0001", LocalDateTime.parse("2024-01-01T00:00:00"))
        val historikk = Kontorhistorikk(
            listOf(
                arenaInnslag("9999", LocalDateTime.parse("2026-05-01T10:00:00")),
                geografiskInnslag("8888", LocalDateTime.parse("2026-06-01T10:00:00")),
                nyesteArbeidsoppfolging,
                arbeidsoppfolgingInnslag("0002", LocalDateTime.parse("2020-01-01T00:00:00")),
            ),
        )

        historikk.nyesteAktuelleKontor() shouldBe nyesteArbeidsoppfolging
    }

    @Test
    fun `nyesteAktuelleKontor faller tilbake til nyeste ARENA når ARBEIDSOPPFOLGING mangler`() {
        val nyesteArena = arenaInnslag("0220", LocalDateTime.parse("2024-05-01T10:00:00"))
        val historikk = Kontorhistorikk(
            listOf(
                arenaInnslag("0111", LocalDateTime.parse("2023-01-01T10:00:00")),
                nyesteArena,
                geografiskInnslag("0333", LocalDateTime.parse("2025-12-31T23:59:59")),
            ),
        )

        historikk.nyesteAktuelleKontor() shouldBe nyesteArena
    }

    @Test
    fun `nyesteAktuelleKontor faller tilbake til nyeste GEOGRAFISK_TILKNYTNING når både ARBEIDSOPPFOLGING og ARENA mangler`() {
        val nyesteGeografisk = geografiskInnslag("0444", LocalDateTime.parse("2024-06-01T10:00:00"))
        val historikk = Kontorhistorikk(
            listOf(
                geografiskInnslag("0333", LocalDateTime.parse("2023-12-31T23:59:59")),
                nyesteGeografisk,
            ),
        )

        historikk.nyesteAktuelleKontor() shouldBe nyesteGeografisk
    }

    @Test
    fun `nyesteAktuelleKontor gir null for tom historikk`() {
        Kontorhistorikk(emptyList()).nyesteAktuelleKontor() shouldBe null
    }

    private fun arenaInnslag(kontorId: String, endretTidspunkt: LocalDateTime) =
        innslag(kontorId, KontorType.ARENA, endretTidspunkt)

    private fun geografiskInnslag(kontorId: String, endretTidspunkt: LocalDateTime) =
        innslag(kontorId, KontorType.GEOGRAFISK_TILKNYTNING, endretTidspunkt)

    private fun arbeidsoppfolgingInnslag(kontorId: String, endretTidspunkt: LocalDateTime) =
        innslag(kontorId, KontorType.ARBEIDSOPPFOLGING, endretTidspunkt)

    private fun innslag(
        kontorId: String,
        kontorType: KontorType,
        endretTidspunkt: LocalDateTime,
    ) = Kontorhistorikkinnslag(
        kontorId = kontorId,
        kontorNavn = "NAV $kontorId",
        kontorType = kontorType,
        endretTidspunkt = endretTidspunkt,
    )
}
