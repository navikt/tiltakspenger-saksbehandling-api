package no.nav.tiltakspenger.saksbehandling.statistikk.meldekort

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class StatistikkMeldekortPostgresRepoTest {

    @Test
    fun `lagrer og henter rad med flere meldeperioder`() {
        withMigratedDb { testDataHelper ->
            val sessionFactory = testDataHelper.sessionFactory
            val dto = statistikkMeldekortDTO()

            sessionFactory.withSession { session ->
                StatistikkMeldekortPostgresRepo.lagre(dto, session)
            }

            val hentet = sessionFactory.withSession { session ->
                StatistikkMeldekortPostgresRepo.hentForMeldekortbehandlingId(dto.meldekortbehandlingId, session)
            }

            hentet shouldBe dto
        }
    }

    @Test
    fun `lagre er idempotent - upsert oppdaterer eksisterende rad`() {
        withMigratedDb { testDataHelper ->
            val sessionFactory = testDataHelper.sessionFactory
            val dto = statistikkMeldekortDTO()
            val oppdatert = dto.copy(
                sistEndret = dto.sistEndret.plusDays(1),
                meldeperioder = dto.meldeperioder.take(1),
                meldekortdager = dto.meldekortdager.take(1),
            )

            sessionFactory.withSession { session ->
                StatistikkMeldekortPostgresRepo.lagre(dto, session)
                StatistikkMeldekortPostgresRepo.lagre(oppdatert, session)
            }

            val hentet = sessionFactory.withSession { session ->
                StatistikkMeldekortPostgresRepo.hentForMeldekortbehandlingId(dto.meldekortbehandlingId, session)
            }

            hentet shouldBe oppdatert
        }
    }

    private fun statistikkMeldekortDTO(): StatistikkMeldekortDTO {
        val dagerPeriode1 = listOf(
            statistikkMeldekortDag(
                1.januar(2024),
                StatistikkMeldekortDTO.StatistikkMeldekortDag.MeldekortDagStatus.DELTATT_UTEN_LONN_I_TILTAKET,
                StatistikkMeldekortDTO.StatistikkMeldekortDag.Reduksjon.INGEN_REDUKSJON,
            ),
            statistikkMeldekortDag(
                2.januar(2024),
                StatistikkMeldekortDTO.StatistikkMeldekortDag.MeldekortDagStatus.FRAVAER_SYK,
                StatistikkMeldekortDTO.StatistikkMeldekortDag.Reduksjon.UKJENT,
            ),
        )
        val dagerPeriode2 = listOf(
            statistikkMeldekortDag(
                15.januar(2024),
                StatistikkMeldekortDTO.StatistikkMeldekortDag.MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
                StatistikkMeldekortDTO.StatistikkMeldekortDag.Reduksjon.YTELSEN_FALLER_BORT,
            ),
        )

        return StatistikkMeldekortDTO(
            sakId = "sak_${UUID.randomUUID()}",
            meldekortbehandlingId = "meldekortbehandling_${UUID.randomUUID()}",
            brukerId = "12345678901",
            saksnummer = "1001",
            vedtattTidspunkt = LocalDateTime.of(2024, 2, 1, 10, 0, 0),
            behandletAutomatisk = false,
            fraOgMed = 1.januar(2024),
            tilOgMed = 28.januar(2024),
            opprettet = LocalDateTime.of(2024, 2, 1, 9, 0, 0),
            sistEndret = LocalDateTime.of(2024, 2, 1, 10, 0, 0),
            meldeperioder = listOf(
                StatistikkMeldekortDTO.StatistikkMeldeperiode(
                    fraOgMed = 1.januar(2024),
                    tilOgMed = 14.januar(2024),
                    meldekortdager = dagerPeriode1,
                    meldeperiodeKjedeId = "2024-01-01/2024-01-14",
                ),
                StatistikkMeldekortDTO.StatistikkMeldeperiode(
                    fraOgMed = 15.januar(2024),
                    tilOgMed = 28.januar(2024),
                    meldekortdager = dagerPeriode2,
                    meldeperiodeKjedeId = "2024-01-15/2024-01-28",
                ),
            ),
            meldeperiodeKjedeId = "2024-01-01/2024-01-14",
            meldekortdager = dagerPeriode1,
        )
    }

    private fun statistikkMeldekortDag(
        dato: LocalDate,
        status: StatistikkMeldekortDTO.StatistikkMeldekortDag.MeldekortDagStatus,
        reduksjon: StatistikkMeldekortDTO.StatistikkMeldekortDag.Reduksjon,
    ) = StatistikkMeldekortDTO.StatistikkMeldekortDag(
        dato = dato,
        status = status,
        reduksjon = reduksjon,
    )
}
