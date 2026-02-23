package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.NyKlagehendelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOpprettholdKlagebehandling
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KlagehendelsePostgresRepoTest {
    @Test
    fun `kan lagre og hente klagehendelse`() {
        withTestApplicationContextAndPostgres { tac ->
            val repo = tac.klagebehandlingContext.klagehendelseRepo
            val opprettet = nå(tac.clock)
            val klagehendelse = NyKlagehendelse(
                opprettet = opprettet,
                sistEndret = opprettet,
                eksternKlagehendelseId = "eksternKlagehendelseId",
                key = "key",
                value = "{}",
                sakId = null,
                klagebehandlingId = null,
            )
            repo.lagreNyHendelse(klagehendelse, null)
            repo.hentNyHendelse(klagehendelse.klagehendelseId) shouldBe NyKlagehendelse(
                klagehendelseId = klagehendelse.klagehendelseId,
                opprettet = LocalDateTime.parse("2025-05-01T01:02:04.456789"),
                sistEndret = LocalDateTime.parse("2025-05-01T01:02:04.456789"),
                eksternKlagehendelseId = klagehendelse.eksternKlagehendelseId,
                key = klagehendelse.key,
                value = klagehendelse.value,
                sakId = klagehendelse.sakId,
                klagebehandlingId = klagehendelse.klagebehandlingId,
            )
        }
    }
}
