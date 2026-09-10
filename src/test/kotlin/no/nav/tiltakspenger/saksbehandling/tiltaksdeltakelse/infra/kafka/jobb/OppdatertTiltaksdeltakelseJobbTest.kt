package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.getTiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.hentUbehandledeTiltaksdeltakerHendelser
import org.junit.jupiter.api.Test

class OppdatertTiltaksdeltakelseJobbTest {

    @Test
    fun `hendelse trigger oppslag mot tiltakshistorikk for å hente nå-tilstand`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakerId = TiltaksdeltakerId.random()
            val deltakelsesperiode = 5.januar(2025) til 5.mai(2025)

            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = deltakelsesperiode,
                internDeltakelseId = tiltaksdeltakerId,
            )

            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(deltakelsesperiode, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            val hendelse = getTiltaksdeltakerHendelse(
                sakId = sak.id,
                fom = deltakelsesperiode.fraOgMed,
                tom = deltakelsesperiode.tilOgMed.minusDays(2),
                deltakerstatus = TiltakDeltakerstatus.Avbrutt,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                hendelse,
                "melding",
                TiltaksdeltakerHendelseKilde.Komet,
                nå(tac.clock).minusMinutes(20),
            )

            // Kalles per deltaker, ikke via håndterEndretTiltaksdeltakerHendelser — den delte testdatabasen kan ha ubehandlede hendelser fra andre tester.
            tac.oppdatertTiltaksdeltakelseJobb.behandleHendelserForDeltaker(tiltaksdeltakerId)

            // Hendelsen tolkes ikke — den trigger kun et ferskt oppslag mot tiltakshistorikk på nåværende ekstern id.
            val fakeKlient = tac.tiltakContext.tiltaksdeltakelseKlient as TiltaksdeltakelseFakeKlient
            fakeKlient.hentTiltaksdeltakelseKall.get()
                .shouldContainExactly(fnr to tiltaksdeltakelse.eksternDeltakelseId)

            // Hendelsene markeres ikke av denne jobben ennå — det eies av EndretTiltaksdeltakerJobb så lenge begge finnes.
            tac.sessionFactory.hentUbehandledeTiltaksdeltakerHendelser().any { it.id == hendelse.id } shouldBe true

            // Placeholder-logikken skal ikke opprette noen ny behandling.
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger shouldHaveSize 1
        }
    }

    @Test
    fun `hendelser innenfor forsinkelsesvinduet trigger ikke oppslag`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakerId = TiltaksdeltakerId.random()
            val deltakelsesperiode = 5.januar(2025) til 5.mai(2025)

            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = deltakelsesperiode,
                internDeltakelseId = tiltaksdeltakerId,
            )

            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(deltakelsesperiode, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            val hendelse = getTiltaksdeltakerHendelse(
                sakId = sak.id,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                hendelse,
                "melding",
                TiltaksdeltakerHendelseKilde.Komet,
                nå(tac.clock).minusMinutes(10),
            )

            tac.oppdatertTiltaksdeltakelseJobb.behandleHendelserForDeltaker(tiltaksdeltakerId)

            val fakeKlient = tac.tiltakContext.tiltaksdeltakelseKlient as TiltaksdeltakelseFakeKlient
            fakeKlient.hentTiltaksdeltakelseKall.get().shouldBeEmpty()
        }
    }

    @Test
    fun `deltakelse som ikke finnes i tiltakshistorikken håndteres uten feil`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakerId = TiltaksdeltakerId.random()
            val deltakelsesperiode = 5.januar(2025) til 5.mai(2025)

            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = deltakelsesperiode,
                internDeltakelseId = tiltaksdeltakerId,
            )

            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(deltakelsesperiode, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            // Deltakelsen er borte fra kilden, f.eks. slettet eller feilregistrert.
            tac.oppdaterTiltaksdeltakelse(fnr, null)

            val hendelse = getTiltaksdeltakerHendelse(
                sakId = sak.id,
                tiltaksdeltakerId = tiltaksdeltakerId,
            )
            tac.tiltaksdeltakerHendelsePostgresRepo.lagre(
                hendelse,
                "melding",
                TiltaksdeltakerHendelseKilde.Komet,
                nå(tac.clock).minusMinutes(20),
            )

            tac.oppdatertTiltaksdeltakelseJobb.behandleHendelserForDeltaker(tiltaksdeltakerId)

            val fakeKlient = tac.tiltakContext.tiltaksdeltakelseKlient as TiltaksdeltakelseFakeKlient
            fakeKlient.hentTiltaksdeltakelseKall.get()
                .shouldContainExactly(fnr to tiltaksdeltakelse.eksternDeltakelseId)

            val ubehandletHendelse = tac.sessionFactory.hentUbehandledeTiltaksdeltakerHendelser()
                .single { it.id == hendelse.id }
            ubehandletHendelse.shouldNotBeNull()
        }
    }
}
