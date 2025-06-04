package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSak
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SøknadRepoTest {
    @Test
    fun `hentApneSoknader - har åpen søknad - returnerer søknaden`() {
        withMigratedDb { testDataHelper ->
            val soknadRepo = testDataHelper.søknadRepo
            val fnr = Fnr.random()
            val soknad = testDataHelper.persisterSakOgSøknad(fnr = fnr)

            val apneSoknader = soknadRepo.hentApneSoknader(fnr)

            apneSoknader.size shouldBe 1
            apneSoknader.first() shouldBe soknad
        }
    }

    @Test
    fun `hentApneSoknader - har avbrutt søknad - returnerer tom liste`() {
        withMigratedDb { testDataHelper ->
            val soknadRepo = testDataHelper.søknadRepo
            val fnr = Fnr.random()
            val soknad = testDataHelper.persisterSakOgSøknad(fnr = fnr)
            soknadRepo.lagreAvbruttSøknad(
                soknad.copy(
                    avbrutt = Avbrutt(
                        tidspunkt = LocalDateTime.now(),
                        saksbehandler = "Z123456",
                        begrunnelse = "begrunnelse",
                    ),
                ),
            )

            val apneSoknader = soknadRepo.hentApneSoknader(fnr)

            apneSoknader.size shouldBe 0
        }
    }

    @Test
    fun `hentApneSoknader - har søknad til behandling - returnerer søknaden`() {
        withMigratedDb { testDataHelper ->
            val soknadRepo = testDataHelper.søknadRepo
            val fnr = Fnr.random()
            val sak = testDataHelper.persisterSak(fnr = fnr)
            val soknad = testDataHelper.persisterSakOgSøknad(
                fnr = fnr,
                sak = sak,
            )
            testDataHelper.persisterOpprettetSøknadsbehandling(
                fnr = fnr,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                søknad = soknad,
            )

            val apneSoknader = soknadRepo.hentApneSoknader(fnr)

            apneSoknader.size shouldBe 1
            apneSoknader.first() shouldBe soknad
        }
    }

    @Test
    fun `hentApneSoknader - søknadsbehandling er iverksatt - returnerer tom liste`() {
        withMigratedDb { testDataHelper ->
            val soknadRepo = testDataHelper.søknadRepo
            val fnr = Fnr.random()
            testDataHelper.persisterIverksattSøknadsbehandling(fnr = fnr)

            val apneSoknader = soknadRepo.hentApneSoknader(fnr)

            apneSoknader.size shouldBe 0
        }
    }
}
