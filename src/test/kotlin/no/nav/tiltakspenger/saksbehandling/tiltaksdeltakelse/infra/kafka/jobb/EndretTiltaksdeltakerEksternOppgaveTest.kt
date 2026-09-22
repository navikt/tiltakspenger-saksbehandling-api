package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.tiltak.KometDeltakerStatusTypeDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.komet.KometTiltakHendelseDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.hentTiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.hentTiltaksdeltakerHendelserForEksternId
import org.junit.jupiter.api.Test
import java.util.UUID

class EndretTiltaksdeltakerEksternOppgaveTest {
    @Test
    fun `oppgavegrunnlaget fra Komet beholdes når nye hendelser endrer deltakelsen`() {
        withTestApplicationContextAndPostgres { tac ->
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
                startDato = 1.april(2025),
                sluttDato = 31.august(2025),
                status = KometTiltakHendelseDTO.DeltakerStatusDto(KometDeltakerStatusTypeDTO.DELTAR),
                dagerPerUke = 2.5F,
                prosentStilling = 50F,
            )
            tac.konsumer(melding)
            val hendelse = tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(eksternId.toString()).single()
            val oppgaveId = OppgaveId(UUID.randomUUID().toString())
            val klient = tac.oppgaveKlient as OppgaveFakeKlient
            klient.opprettOppgaveUtenDuplikatkontrollResponse = oppgaveId.right()
            val før = nå(tac.clock)

            tac.endretTiltaksdeltakerJobb.behandleHendelserForDeltaker(deltakelse.internDeltakelseId)

            val første = tac.eksternOppgaveRepo.hentForSakId(sak.id).single()
            første.sakId shouldBe sak.id
            første.oppgaveId shouldBe oppgaveId
            (første.opprettet in før..nå(tac.clock)) shouldBe true
            første.grunnlag shouldBe Oppgavegrunnlag.EndretTiltaksdeltakelse(
                hendelseId = hendelse.id,
                tiltaksdeltakerId = deltakelse.internDeltakelseId,
                eksternDeltakerId = eksternId.toString(),
                deltakelseFraOgMed = 1.april(2025),
                deltakelseTilOgMed = 31.august(2025),
                dagerPerUke = 2.5F,
                deltakelsesprosent = 50F,
                deltakerstatus = TiltakDeltakerstatus.Deltar,
                tilleggstekst = "Endret deltakelsesmengde.",
            )
            tac.sessionFactory.hentTiltaksdeltakerHendelse(hendelse.id)!!.oppgaveId shouldBe oppgaveId

            tac.konsumer(
                KometTiltakHendelseDTO(
                    id = eksternId,
                    startDato = null,
                    sluttDato = null,
                    status = KometTiltakHendelseDTO.DeltakerStatusDto(KometDeltakerStatusTypeDTO.IKKE_AKTUELL),
                    dagerPerUke = null,
                    prosentStilling = null,
                ),
            )
            val nyHendelse = tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(eksternId.toString()).single { it.id != hendelse.id }
            klient.opprettOppgaveUtenDuplikatkontrollResponse = null
            tac.endretTiltaksdeltakerJobb.behandleHendelserForDeltaker(deltakelse.internDeltakelseId)

            val referanser = tac.eksternOppgaveRepo.hentForSakId(sak.id)
            referanser.size shouldBe 2
            referanser.first() shouldBe første
            referanser.last().grunnlag shouldBe Oppgavegrunnlag.EndretTiltaksdeltakelse(
                hendelseId = nyHendelse.id,
                tiltaksdeltakerId = deltakelse.internDeltakelseId,
                eksternDeltakerId = eksternId.toString(),
                deltakelseFraOgMed = null,
                deltakelseTilOgMed = null,
                dagerPerUke = null,
                deltakelsesprosent = null,
                deltakerstatus = TiltakDeltakerstatus.IkkeAktuell,
                tilleggstekst = "Deltakelsen er ikke aktuell.",
            )
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.size shouldBe 1
        }
    }

    @Test
    fun `oppgavefeil lar Komet-hendelsen være ubehandlet uten ekstern oppgave`() {
        withTestApplicationContextAndPostgres { tac ->
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
            tac.konsumer(
                KometTiltakHendelseDTO(
                    id = eksternId,
                    startDato = null,
                    sluttDato = null,
                    status = KometTiltakHendelseDTO.DeltakerStatusDto(KometDeltakerStatusTypeDTO.IKKE_AKTUELL),
                    dagerPerUke = null,
                    prosentStilling = null,
                ),
            )
            val hendelse = tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(eksternId.toString()).single()
            val klient = tac.oppgaveKlient as OppgaveFakeKlient
            klient.opprettOppgaveUtenDuplikatkontrollResponse = ObjectMother.httpKlientUventetStatus().left()

            tac.endretTiltaksdeltakerJobb.behandleHendelserForDeltaker(deltakelse.internDeltakelseId)

            tac.eksternOppgaveRepo.hentForSakId(sak.id) shouldBe emptyList()
            tac.sessionFactory.hentTiltaksdeltakerHendelse(hendelse.id) shouldBe hendelse

            klient.opprettOppgaveUtenDuplikatkontrollResponse = null
            tac.endretTiltaksdeltakerJobb.behandleHendelserForDeltaker(deltakelse.internDeltakelseId)

            val referanse = tac.eksternOppgaveRepo.hentForSakId(sak.id).single()
            tac.sessionFactory.hentTiltaksdeltakerHendelse(hendelse.id)!!.oppgaveId shouldBe referanse.oppgaveId
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.size shouldBe 1
        }
    }

    private suspend fun TestApplicationContextMedPostgres.konsumer(melding: KometTiltakHendelseDTO) {
        tiltaksdeltakerKometConsumer.consume(melding.id, serialize(melding))
        clock.spol1timeFrem()
    }
}
