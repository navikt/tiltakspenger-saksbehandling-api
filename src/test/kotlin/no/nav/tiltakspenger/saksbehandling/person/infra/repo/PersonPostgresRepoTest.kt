package no.nav.tiltakspenger.saksbehandling.person.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetRevurdering
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRammevedtakMedBehandletMeldekort
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import org.junit.jupiter.api.Test

class PersonPostgresRepoTest {

    @Test
    fun hentFnrForBehandlingId() {
        withMigratedDb { testDataHelper ->
            testDataHelper.persisterOpprettetRevurdering().also {
                val (sak, behandling) = it
                testDataHelper.personRepo.hentFnrForSakId(sak.id) shouldBe sak.fnr
                testDataHelper.personRepo.hentFnrForBehandlingId(behandling.id) shouldBe sak.fnr
                testDataHelper.personRepo.hentFnrForBehandlingId(behandling.id) shouldBe sak.fnr
                testDataHelper.personRepo.hentFnrForSaksnummer(sak.saksnummer) shouldBe sak.fnr
                testDataHelper.personRepo.hentFnrForSøknadId(sak.behandlinger.førstegangsBehandlinger.singleOrNullOrThrow()!!.søknad!!.id) shouldBe sak.fnr
            }
            val innvilgelsesperiode = Periode(2.januar(2023), 31.mars(2023))
            testDataHelper.persisterRammevedtakMedBehandletMeldekort(
                deltakelseFom = innvilgelsesperiode.fraOgMed,
                deltakelseTom = innvilgelsesperiode.tilOgMed,
            ).also {
                val (sak, meldekortbehandling) = it
                testDataHelper.personRepo.hentFnrForMeldekortId(meldekortbehandling.id) shouldBe sak.fnr
            }
        }
    }
}
