package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import org.junit.jupiter.api.Test

class OppdatertTiltaksdeltakelseJobbTest {

    private suspend fun TestApplicationContextMedPostgres.registrerEndringOgBehandle(
        sak: Sak,
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
        nåtilstand: TiltaksdeltakelseIntern? = tiltaksdeltakelse,
    ): Sak {
        oppdaterTiltaksdeltakelse(sak.fnr, nåtilstand)
        tiltakContext.tiltaksdeltakerRepo.registrerUbehandletEndring(
            id = tiltaksdeltakelse.internDeltakelseId,
            sakId = sak.id,
            tidspunkt = nå(clock).minusMinutes(20),
        )
        val deltaker = tiltakContext.tiltaksdeltakerRepo
            .hentTiltaksdeltaker(tiltaksdeltakelse.eksternDeltakelseId).shouldNotBeNull()

        oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker)

        return sakContext.sakRepo.hentForSakId(sak.id)!!
    }

    private fun TestApplicationContextMedPostgres.assertMarkørNullstilt(eksternDeltakelseId: String) {
        tiltakContext.tiltaksdeltakerRepo
            .hentTiltaksdeltaker(eksternDeltakelseId)
            .shouldNotBeNull()
            .sisteUbehandletEndringTidspunkt.shouldBeNull()
    }

    @Test
    fun `ingen endring i nå-tilstanden - markøren nullstilles uten revurdering`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = 5.januar(2025) til 5.mai(2025),
                internDeltakelseId = TiltaksdeltakerId.random(),
            )
            val sak = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            ).first

            val oppdatertSak = tac.registrerEndringOgBehandle(sak, tiltaksdeltakelse)

            // Endringen tolkes ikke — den trigger kun et ferskt oppslag mot tiltakshistorikk på nåværende ekstern id.
            val fakeKlient = tac.tiltakContext.tiltaksdeltakelseKlient as TiltaksdeltakelseFakeKlient
            fakeKlient.hentTiltaksdeltakelseKall.get()
                .shouldContainExactly(fnr to tiltaksdeltakelse.eksternDeltakelseId)

            tac.assertMarkørNullstilt(tiltaksdeltakelse.eksternDeltakelseId)
            oppdatertSak.rammebehandlinger shouldHaveSize 1
        }
    }

    @Test
    fun `deltakelse som ikke finnes i tiltakshistorikken nullstiller markøren uten feil`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = 5.januar(2025) til 5.mai(2025),
                internDeltakelseId = TiltaksdeltakerId.random(),
            )
            val sak = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            ).first

            // Deltakelsen er borte fra kilden, f.eks. slettet eller feilregistrert.
            val oppdatertSak = tac.registrerEndringOgBehandle(sak, tiltaksdeltakelse, nåtilstand = null)

            val fakeKlient = tac.tiltakContext.tiltaksdeltakelseKlient as TiltaksdeltakelseFakeKlient
            fakeKlient.hentTiltaksdeltakelseKall.get()
                .shouldContainExactly(fnr to tiltaksdeltakelse.eksternDeltakelseId)

            tac.assertMarkørNullstilt(tiltaksdeltakelse.eksternDeltakelseId)
            oppdatertSak.rammebehandlinger shouldHaveSize 1
        }
    }

    @Test
    fun `deltaker uten ubehandlet endring behandles ikke`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = 5.januar(2025) til 5.mai(2025),
                internDeltakelseId = TiltaksdeltakerId.random(),
            )
            iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            val deltaker = tac.tiltakContext.tiltaksdeltakerRepo
                .hentTiltaksdeltaker(tiltaksdeltakelse.eksternDeltakelseId).shouldNotBeNull()
            deltaker.sisteUbehandletEndringTidspunkt.shouldBeNull()

            tac.oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker)

            val fakeKlient = tac.tiltakContext.tiltaksdeltakelseKlient as TiltaksdeltakelseFakeKlient
            fakeKlient.hentTiltaksdeltakelseKall.get().shouldBeEmpty()
        }
    }

    @Test
    fun `avbrutt deltakelse - oppretter stans-revurdering`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = 5.januar(2025) til 5.mai(2025),
                internDeltakelseId = TiltaksdeltakerId.random(),
            )
            val sak = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            ).first

            val oppdatertSak = tac.registrerEndringOgBehandle(
                sak,
                tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
            )

            tac.assertMarkørNullstilt(tiltaksdeltakelse.eksternDeltakelseId)
            oppdatertSak.rammebehandlinger shouldHaveSize 2

            val revurdering = oppdatertSak.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
            val grunn = revurdering.automatiskOpprettetGrunn.shouldNotBeNull()
            grunn.hendelseId.shouldBeNull()
            grunn.endringer shouldBe listOf(TiltaksdeltakerEndring.AvbruttDeltakelse)
        }
    }

    @Test
    fun `forlenget deltakelse - oppretter innvilgelse-revurdering`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = 5.januar(2025) til 5.mai(2025),
                internDeltakelseId = TiltaksdeltakerId.random(),
            )
            val sak = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            ).first

            val oppdatertSak = tac.registrerEndringOgBehandle(
                sak,
                tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(
                    deltakelseFraOgMed = 5.januar(2025),
                    deltakelseTilOgMed = 5.juni(2025),
                ),
            )

            tac.assertMarkørNullstilt(tiltaksdeltakelse.eksternDeltakelseId)
            oppdatertSak.rammebehandlinger shouldHaveSize 2

            val revurdering = oppdatertSak.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
            val grunn = revurdering.automatiskOpprettetGrunn.shouldNotBeNull()
            grunn.endringer shouldBe listOf(TiltaksdeltakerEndring.Forlengelse(5.juni(2025)))
        }
    }

    @Test
    fun `endret startdato - oppretter omgjøring`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = 5.januar(2025) til 5.mai(2025),
                internDeltakelseId = TiltaksdeltakerId.random(),
            )
            val sak = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            ).first

            val oppdatertSak = tac.registrerEndringOgBehandle(
                sak,
                tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(
                    deltakelseFraOgMed = 6.januar(2025),
                ),
            )

            tac.assertMarkørNullstilt(tiltaksdeltakelse.eksternDeltakelseId)
            oppdatertSak.rammebehandlinger shouldHaveSize 2

            val revurdering = oppdatertSak.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
            val grunn = revurdering.automatiskOpprettetGrunn.shouldNotBeNull()
            grunn.endringer shouldBe listOf(TiltaksdeltakerEndring.EndretStartdato(6.januar(2025)))
        }
    }

    @Test
    fun `endring som ikke gir automatisk revurdering - markøren nullstilles uten revurdering`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = 5.januar(2025) til 5.mai(2025),
                internDeltakelseId = TiltaksdeltakerId.random(),
            )
            val sak = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            ).first

            // Kun status endret til noe annet enn avbrutt/ikke aktuell gir ingen automatisk revurdering.
            // Gosys-oppgaver er avviklet, så endringen logges kun.
            val oppdatertSak = tac.registrerEndringOgBehandle(
                sak,
                tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Venteliste),
            )

            tac.assertMarkørNullstilt(tiltaksdeltakelse.eksternDeltakelseId)
            oppdatertSak.rammebehandlinger shouldHaveSize 1
        }
    }
}
