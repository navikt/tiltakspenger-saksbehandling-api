package no.nav.tiltakspenger.vedtak.repository.uføre

import io.kotest.matchers.shouldBe
import kotliquery.sessionOf
import no.nav.tiltakspenger.objectmothers.foreldrepengerVedtak
import no.nav.tiltakspenger.objectmothers.innsendingMedYtelse
import no.nav.tiltakspenger.vedtak.db.DataSource
import no.nav.tiltakspenger.vedtak.db.PostgresTestcontainer
import no.nav.tiltakspenger.vedtak.db.flywayMigrate
import no.nav.tiltakspenger.vedtak.repository.innsending.PostgresInnsendingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.Random

@Testcontainers
class UføreVedtakDAOTest {
    companion object {
        @Container
        val postgresContainer = PostgresTestcontainer
    }

    @BeforeEach
    fun setup() {
        flywayMigrate()
    }

    @Test
    fun `lagre og hente med null felter`() {
        val foreldrepengerVedtakDAO = ForeldrepengerVedtakDAO()
        val repository = PostgresInnsendingRepository(foreldrepengerVedtakDAO = foreldrepengerVedtakDAO)
        val ident = Random().nextInt().toString()
        val innsending = innsendingMedYtelse(ident = ident)
        repository.lagre(innsending)

        val foreldrepengerVedtak = foreldrepengerVedtak()

        sessionOf(DataSource.hikariDataSource).use {
            it.transaction { txSession ->
                foreldrepengerVedtakDAO.lagre(innsending.id, listOf(foreldrepengerVedtak), txSession)
            }
        }

        val hentet = sessionOf(DataSource.hikariDataSource).use {
            it.transaction { txSession ->
                foreldrepengerVedtakDAO.hentForInnsending(innsendingId = innsending.id, txSession = txSession)
            }
        }

        hentet.size shouldBe 1
        hentet.first() shouldBe foreldrepengerVedtak.copy(
            id = hentet.first().id,
        )
    }
}
