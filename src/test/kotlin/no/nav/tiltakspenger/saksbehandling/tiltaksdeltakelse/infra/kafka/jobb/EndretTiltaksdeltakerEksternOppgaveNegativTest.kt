package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.tiltak.KometDeltakerStatusTypeDTO
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.komet.KometTiltakHendelseDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.hentTiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.hentTiltaksdeltakerHendelserForEksternId
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * En testbegrensning avviser kildemarkeringen etter at oppgaven er opprettet og referansen skrevet.
 * Den negative databasetesten verifiserer at begge databaseskrivingene ligger i samme transaksjon.
 * Testen er isolert fordi den endrer skjemaet midlertidig.
 */
class EndretTiltaksdeltakerEksternOppgaveNegativTest {
    @Test
    @IsolatedDatabaseTest
    fun `referansen rulles tilbake når markering av Komet-hendelsen feiler`() {
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
            val melding = KometTiltakHendelseDTO(
                id = eksternId,
                startDato = null,
                sluttDato = null,
                status = KometTiltakHendelseDTO.DeltakerStatusDto(KometDeltakerStatusTypeDTO.IKKE_AKTUELL),
                dagerPerUke = null,
                prosentStilling = null,
            )
            tac.tiltaksdeltakerKometConsumer.consume(eksternId, serialize(melding))
            tac.clock.spol1timeFrem()
            val hendelse = tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(eksternId.toString()).single()
            val klient = tac.oppgaveKlient as OppgaveFakeKlient
            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "ALTER TABLE tiltaksdeltaker_kafka ADD CONSTRAINT test_avvis_oppgave_id CHECK (oppgave_id IS NULL)",
                    ).asExecute,
                )
            }
            try {
                tac.endretTiltaksdeltakerJobb.behandleHendelserForDeltaker(deltakelse.internDeltakelseId)

                klient.opprettedeOppgaveIder.size shouldBe 1
                tac.eksternOppgaveRepo.hentForSakId(sak.id) shouldBe emptyList()
                tac.sessionFactory.hentTiltaksdeltakerHendelse(hendelse.id) shouldBe hendelse
            } finally {
                tac.sessionFactory.withSession { session ->
                    session.run(
                        queryOf("ALTER TABLE tiltaksdeltaker_kafka DROP CONSTRAINT test_avvis_oppgave_id").asExecute,
                    )
                }
            }

            tac.endretTiltaksdeltakerJobb.behandleHendelserForDeltaker(deltakelse.internDeltakelseId)

            klient.opprettedeOppgaveIder.size shouldBe 2
            val referanse = tac.eksternOppgaveRepo.hentForSakId(sak.id).single()
            referanse.oppgaveId shouldBe klient.opprettedeOppgaveIder.last()
            tac.sessionFactory.hentTiltaksdeltakerHendelse(hendelse.id)!!.oppgaveId shouldBe referanse.oppgaveId
        }
    }
}
