package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klagehendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.NyKlagehendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KlagehendelsePostgresRepoTest {
    @Test
    fun `kan lagre og hente klagehendelse`() {
        withTestApplicationContextAndPostgres { tac ->
            val repo = tac.klagebehandlingContext.klagehendelseRepo
            val klagehendelse = NyKlagehendelse(
                opprettet = LocalDateTime.now(tac.clock),
                eksternKlagehendelseId = "eksternKlagehendelseId",
                key = "key",
                value = "{}",
            )
            repo.lagreNyHendelse(klagehendelse, null)
            tac.sessionFactory.withSession {
                KlagehendelsePostgresRepo.hentHendelse(klagehendelse.klagehendelseId, it) shouldBe Klagehendelse(
                    klagehendelseId = klagehendelse.klagehendelseId,
                    opprettet = LocalDateTime.parse("2025-05-01T01:02:04.456789"),
                    sistEndret = LocalDateTime.parse("2025-05-01T01:02:04.456789"),
                    eksternKlagehendelseId = klagehendelse.eksternKlagehendelseId,
                )
            }
        }
    }
}
