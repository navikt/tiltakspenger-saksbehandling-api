package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeKlient
import org.junit.jupiter.api.Test

class OppdatertTiltaksdeltakelseJobbTest {

    @Test
    fun `ubehandlet endring trigger oppslag mot tiltakshistorikk og markøren nullstilles`() {
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

            // Simulerer det consumerne gjør ved mottak av en hendelse.
            tac.tiltakContext.tiltaksdeltakerRepo.registrerUbehandletEndring(
                id = tiltaksdeltakerId,
                sakId = sak.id,
                tidspunkt = nå(tac.clock).minusMinutes(20),
            )

            val deltaker = tac.tiltakContext.tiltaksdeltakerRepo
                .hentTiltaksdeltaker(tiltaksdeltakelse.eksternDeltakelseId).shouldNotBeNull()

            tac.oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker)

            // Endringen tolkes ikke — den trigger kun et ferskt oppslag mot tiltakshistorikk på nåværende ekstern id.
            val fakeKlient = tac.tiltakContext.tiltaksdeltakelseKlient as TiltaksdeltakelseFakeKlient
            fakeKlient.hentTiltaksdeltakelseKall.get()
                .shouldContainExactly(fnr to tiltaksdeltakelse.eksternDeltakelseId)

            // Markøren er nullstilt etter behandling.
            tac.tiltakContext.tiltaksdeltakerRepo
                .hentTiltaksdeltaker(tiltaksdeltakelse.eksternDeltakelseId)
                .shouldNotBeNull()
                .sisteUbehandletEndring.shouldBeNull()

            // Placeholder-logikken skal ikke opprette noen ny behandling.
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger shouldHaveSize 1
        }
    }

    @Test
    fun `deltakelse som ikke finnes i tiltakshistorikken nullstiller markøren uten feil`() {
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

            tac.tiltakContext.tiltaksdeltakerRepo.registrerUbehandletEndring(
                id = tiltaksdeltakerId,
                sakId = sak.id,
                tidspunkt = nå(tac.clock).minusMinutes(20),
            )

            val deltaker = tac.tiltakContext.tiltaksdeltakerRepo
                .hentTiltaksdeltaker(tiltaksdeltakelse.eksternDeltakelseId).shouldNotBeNull()

            tac.oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker)

            val fakeKlient = tac.tiltakContext.tiltaksdeltakelseKlient as TiltaksdeltakelseFakeKlient
            fakeKlient.hentTiltaksdeltakelseKall.get()
                .shouldContainExactly(fnr to tiltaksdeltakelse.eksternDeltakelseId)

            tac.tiltakContext.tiltaksdeltakerRepo
                .hentTiltaksdeltaker(tiltaksdeltakelse.eksternDeltakelseId)
                .shouldNotBeNull()
                .sisteUbehandletEndring.shouldBeNull()
        }
    }

    @Test
    fun `deltaker uten ubehandlet endring behandles ikke`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakerId = TiltaksdeltakerId.random()
            val deltakelsesperiode = 5.januar(2025) til 5.mai(2025)

            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = deltakelsesperiode,
                internDeltakelseId = tiltaksdeltakerId,
            )

            iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(deltakelsesperiode, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            val deltaker = tac.tiltakContext.tiltaksdeltakerRepo
                .hentTiltaksdeltaker(tiltaksdeltakelse.eksternDeltakelseId).shouldNotBeNull()
            deltaker.sisteUbehandletEndring.shouldBeNull()

            tac.oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker)

            val fakeKlient = tac.tiltakContext.tiltaksdeltakelseKlient as TiltaksdeltakelseFakeKlient
            fakeKlient.hentTiltaksdeltakelseKall.get().shouldBeEmpty()
        }
    }
}
