package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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
import no.nav.tiltakspenger.saksbehandling.oppgave.Oppgavegrunnlag.EndretTiltaksdeltakelse.Kilde
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltaker
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.komet.KometTiltakHendelseDTO
import org.junit.jupiter.api.Test
import java.util.UUID

class OppdatertTiltaksdeltakelseEksternOppgaveTest {
    @Test
    fun `oppgavegrunnlaget fra tiltakshistorikken beholdes når nye endringer gir ny oppgave`() {
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
            tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(antallDagerPerUke = 2.5F, deltakelseProsent = 50F))
            val deltaker = tac.registrerEndring(deltakelse)
            val oppgaveId = OppgaveId(UUID.randomUUID().toString())
            val klient = tac.oppgaveKlient as OppgaveFakeKlient
            klient.opprettOppgaveUtenDuplikatkontrollResponse = oppgaveId.right()
            val før = nå(tac.clock)

            tac.oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker)

            val første = tac.eksternOppgaveRepo.hentForSakId(sak.id).single()
            første.sakId shouldBe sak.id
            første.oppgaveId shouldBe oppgaveId
            første.tilleggstekst shouldBe "Endret deltakelsesmengde."
            (første.opprettet in før..nå(tac.clock)) shouldBe true
            første.grunnlag shouldBe Oppgavegrunnlag.EndretTiltaksdeltakelse(
                kilde = Kilde.Tiltakshistorikk(deltaker.sisteUbehandletEndringTidspunkt.shouldNotBeNull()),
                tiltaksdeltakerId = deltakelse.internDeltakelseId,
                eksternDeltakerId = eksternId.toString(),
                deltakelseFraOgMed = 1.april(2025),
                deltakelseTilOgMed = 31.august(2025),
                dagerPerUke = 2.5F,
                deltakelsesprosent = 50F,
                deltakerstatus = TiltakDeltakerstatus.Deltar,
            )
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt.shouldBeNull()

            tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.IkkeAktuell))
            val nyDeltaker = tac.registrerEndring(deltakelse)
            klient.opprettOppgaveUtenDuplikatkontrollResponse = null
            tac.oppdatertTiltaksdeltakelseJobb.behandleDeltaker(nyDeltaker)

            val referanser = tac.eksternOppgaveRepo.hentForSakId(sak.id)
            referanser.size shouldBe 2
            referanser.first() shouldBe første
            referanser.last().tilleggstekst shouldBe "Deltakelsen er ikke aktuell."
            referanser.last().grunnlag shouldBe Oppgavegrunnlag.EndretTiltaksdeltakelse(
                kilde = Kilde.Tiltakshistorikk(nyDeltaker.sisteUbehandletEndringTidspunkt.shouldNotBeNull()),
                tiltaksdeltakerId = deltakelse.internDeltakelseId,
                eksternDeltakerId = eksternId.toString(),
                deltakelseFraOgMed = 1.april(2025),
                deltakelseTilOgMed = 31.august(2025),
                dagerPerUke = deltakelse.antallDagerPerUke,
                deltakelsesprosent = deltakelse.deltakelseProsent,
                deltakerstatus = TiltakDeltakerstatus.IkkeAktuell,
            )
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.size shouldBe 1
        }
    }

    @Test
    fun `oppgavefeil lar markøren stå uten ekstern oppgave`() {
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
            tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.IkkeAktuell))
            val deltaker = tac.registrerEndring(deltakelse)
            val klient = tac.oppgaveKlient as OppgaveFakeKlient
            klient.opprettOppgaveUtenDuplikatkontrollResponse = ObjectMother.httpKlientUventetStatus().left()

            tac.oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker)

            tac.eksternOppgaveRepo.hentForSakId(sak.id) shouldBe emptyList()
            tac.hentDeltaker(deltakelse) shouldBe deltaker

            klient.opprettOppgaveUtenDuplikatkontrollResponse = null
            tac.oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker)

            val referanse = tac.eksternOppgaveRepo.hentForSakId(sak.id).single()
            referanse.oppgaveId shouldBe klient.opprettedeOppgaveIder.last()
            tac.hentDeltaker(deltakelse).sisteUbehandletEndringTidspunkt.shouldBeNull()
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.size shouldBe 1
        }
    }

    /** Hendelsens innhold er uten betydning, siden jobben henter nå-tilstanden fra tiltakshistorikken. */
    private suspend fun TestApplicationContextMedPostgres.registrerEndring(deltakelse: TiltaksdeltakelseIntern): Tiltaksdeltaker {
        val melding = KometTiltakHendelseDTO(
            id = UUID.fromString(deltakelse.eksternDeltakelseId),
            startDato = deltakelse.deltakelseFraOgMed,
            sluttDato = deltakelse.deltakelseTilOgMed,
            status = KometTiltakHendelseDTO.DeltakerStatusDto(KometDeltakerStatusTypeDTO.DELTAR),
            dagerPerUke = null,
            prosentStilling = null,
        )
        tiltaksdeltakerKometConsumer.consume(melding.id, serialize(melding))
        clock.spol1timeFrem()
        return hentDeltaker(deltakelse).also { it.sisteUbehandletEndringTidspunkt.shouldNotBeNull() }
    }

    private fun TestApplicationContextMedPostgres.hentDeltaker(deltakelse: TiltaksdeltakelseIntern): Tiltaksdeltaker =
        tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(deltakelse.eksternDeltakelseId).shouldNotBeNull()
}
