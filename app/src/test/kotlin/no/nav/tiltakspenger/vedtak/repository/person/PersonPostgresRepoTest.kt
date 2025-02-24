package no.nav.tiltakspenger.vedtak.repository.person

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.persisterOpprettetRevurderingDeprecated
import no.nav.tiltakspenger.db.withMigratedDb
import org.junit.jupiter.api.Test

class PersonPostgresRepoTest {

    @Test
    fun hentFnrForBehandlingId() {
        withMigratedDb { testDataHelper ->
            val (sak, revurdering) = testDataHelper.persisterOpprettetRevurderingDeprecated()
            testDataHelper.personRepo.hentFnrForSakId(sak.id) shouldBe sak.fnr
            testDataHelper.personRepo.hentFnrForBehandlingId(sak.førstegangsbehandling!!.id) shouldBe sak.fnr
            testDataHelper.personRepo.hentFnrForBehandlingId(revurdering.id) shouldBe sak.fnr
            testDataHelper.personRepo.hentFnrForSaksnummer(sak.saksnummer) shouldBe sak.fnr
            testDataHelper.personRepo.hentFnrForSøknadId(sak.førstegangsbehandling!!.søknad!!.id) shouldBe sak.fnr
            // testDataHelper.personRepo.hentFnrForMeldekortId() TODO: Implement
            // testDataHelper.personRepo.hentFnrForVedtakId(sak.rammevedtak!!.id) shouldBe sak.fnr TODO: Fixme
        }
    }
}
