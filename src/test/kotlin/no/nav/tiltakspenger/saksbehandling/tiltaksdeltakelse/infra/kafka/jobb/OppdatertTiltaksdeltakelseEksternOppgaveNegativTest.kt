package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * En testbegrensning avviser nullstillingen av markøren etter at oppgaven er opprettet og referansen skrevet.
 * Den negative databasetesten verifiserer at begge databaseskrivingene ligger i samme transaksjon.
 * Testen er isolert fordi den endrer skjemaet midlertidig.
 */
class OppdatertTiltaksdeltakelseEksternOppgaveNegativTest {
    @Test
    @IsolatedDatabaseTest
    fun `referansen rulles tilbake når nullstilling av markøren feiler`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val eksternId = UUID.randomUUID()
            val deltakelse = ObjectMother.tiltaksdeltakelse(
                periode = 1.april(2025) til 31.august(2025),
                eksternTiltaksdeltakelseId = eksternId.toString(),
            )
            val (sak) = opprettSøknadsbehandlingUnderBehandling(
                tac = tac,
                fnr = ObjectMother.gyldigFnr(),
                tiltaksdeltakelse = deltakelse,
            )
            tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.IkkeAktuell))
            val repo = tac.tiltakContext.tiltaksdeltakerRepo
            repo.registrerUbehandletEndring(deltakelse.internDeltakelseId, sak.id, nå(tac.clock).minusMinutes(20))
            val deltaker = repo.hentTiltaksdeltaker(eksternId.toString()).shouldNotBeNull()
            val klient = tac.oppgaveKlient as OppgaveFakeKlient
            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "ALTER TABLE tiltaksdeltaker ADD CONSTRAINT test_avvis_nullstilling CHECK (siste_ubehandlet_endring IS NOT NULL) NOT VALID",
                    ).asExecute,
                )
            }
            try {
                tac.oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker)

                klient.opprettedeOppgaveIder.size shouldBe 1
                tac.eksternOppgaveRepo.hentForSakId(sak.id) shouldBe emptyList()
                repo.hentTiltaksdeltaker(eksternId.toString()) shouldBe deltaker
            } finally {
                tac.sessionFactory.withSession { session ->
                    session.run(
                        queryOf("ALTER TABLE tiltaksdeltaker DROP CONSTRAINT test_avvis_nullstilling").asExecute,
                    )
                }
            }

            tac.oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker)

            klient.opprettedeOppgaveIder.size shouldBe 2
            val referanse = tac.eksternOppgaveRepo.hentForSakId(sak.id).single()
            referanse.oppgaveId shouldBe klient.opprettedeOppgaveIder.last()
            repo.hentTiltaksdeltaker(eksternId.toString()).shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldBeNull()
        }
    }
}
