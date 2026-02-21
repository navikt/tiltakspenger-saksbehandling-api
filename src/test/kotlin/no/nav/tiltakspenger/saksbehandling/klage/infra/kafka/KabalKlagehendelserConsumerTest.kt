package no.nav.tiltakspenger.saksbehandling.klage.infra.kafka

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.KlagehendelseId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.GenerererKlageinstanshendelse.GenererKlageinstanshendelseUtfall
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagehendelsePostgresRepo
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KabalKlagehendelserConsumerTest {
    @Test
    fun `skal kunne parse klagebehandling avsluttet-hendelse`() {
        withTestApplicationContextAndPostgres { tac ->
            val klagehendelseId = KlageinstansKlagehendelseConsumer.consume(
                key = "some-unused-uuid",
                value = GenerererKlageinstanshendelse.avsluttetJson(utfall = GenererKlageinstanshendelseUtfall.RETUR),
                clock = tac.clock,
                lagreNyHendelse = tac.klagebehandlingContext.klagehendelseRepo::lagreNyHendelse,
            )!!
            tac.sessionFactory.withSession { session ->
                KlagehendelsePostgresRepo.hentHendelse(
                    klagehendelseId,
                    session,
                ) shouldBe expectedKlagehendelseKlagebehandlingAvsluttet(klagehendelseId)
            }
        }
    }

    private fun expectedKlagehendelseKlagebehandlingAvsluttet(
        klagehendelseId: KlagehendelseId,
        avsluttetTidspunkt: LocalDateTime = LocalDateTime.parse("2025-05-01T01:02:04.456789"),
        utfall: KlagehendelseKlagebehandlingAvsluttetUtfall = KlagehendelseKlagebehandlingAvsluttetUtfall.RETUR,
        journalpostreferanser: List<JournalpostId> = listOf(JournalpostId("123456789"), JournalpostId("987654321")),
    ): Klageinstanshendelse = Klageinstanshendelse.KlagebehandlingAvsluttet(
        klagehendelseId = klagehendelseId,
        opprettet = LocalDateTime.parse("2025-05-01T01:02:04.456789"),
        sistEndret = LocalDateTime.parse("2025-05-01T01:02:04.456789"),
        eksternKlagehendelseId = "0f4ea0c2-8b44-4266-a1c3-801006b06280",
        avsluttetTidspunkt = avsluttetTidspunkt,
        utfall = utfall,
        journalpostreferanser = journalpostreferanser,
    )
}
