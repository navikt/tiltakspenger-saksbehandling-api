package no.nav.tiltakspenger.saksbehandling.person.identhendelser

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class IdenthendelseServiceTest {
    @Test
    fun `behandleIdenthendelse - finnes ingen sak - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val identhendelseRepository = testDataHelper.identhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val identhendelseService = IdenthendelseService(sakPostgresRepo, identhendelseRepository)
                val gammeltFnr = Fnr.random()

                identhendelseService.behandleIdenthendelse(
                    Aktor(
                        listOf(
                            Identifikator(Fnr.random().verdi, Type.FOLKEREGISTERIDENT, true),
                            Identifikator(gammeltFnr.verdi, Type.FOLKEREGISTERIDENT, false),
                            Identifikator("1234567890123", Type.AKTORID, true),
                        ),
                    ),
                )

                identhendelseRepository.hent(gammeltFnr) shouldBe emptyList()
            }
        }
    }

    @Test
    fun `behandleIdenthendelse - finnes en sak - lagrer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val identhendelseRepository = testDataHelper.identhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val identhendelseService = IdenthendelseService(sakPostgresRepo, identhendelseRepository)
                val gammeltFnr = Fnr.random()
                val nyttFnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = gammeltFnr)
                testDataHelper.persisterSakOgSøknad(
                    fnr = gammeltFnr,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = gammeltFnr),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )

                identhendelseService.behandleIdenthendelse(
                    Aktor(
                        listOf(
                            Identifikator(nyttFnr.verdi, Type.FOLKEREGISTERIDENT, true),
                            Identifikator(gammeltFnr.verdi, Type.FOLKEREGISTERIDENT, false),
                            Identifikator("1234567890123", Type.AKTORID, true),
                        ),
                    ),
                )

                val identhendelseDb = identhendelseRepository.hent(gammeltFnr).first()
                identhendelseDb.gammeltFnr shouldBe gammeltFnr
                identhendelseDb.nyttFnr shouldBe nyttFnr
                identhendelseDb.sakId shouldBe sak.id
                identhendelseDb.produsertHendelse shouldBe null
                identhendelseDb.oppdatertDatabase shouldBe null
            }
        }
    }

    @Test
    fun `behandleIdenthendelse - finnes sak på nytt fnr - ignorerer`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val identhendelseRepository = testDataHelper.identhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val identhendelseService = IdenthendelseService(sakPostgresRepo, identhendelseRepository)
                val gammeltFnr = Fnr.random()
                val nyttFnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = nyttFnr)
                testDataHelper.persisterSakOgSøknad(
                    fnr = nyttFnr,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = nyttFnr),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )

                identhendelseService.behandleIdenthendelse(
                    Aktor(
                        listOf(
                            Identifikator(nyttFnr.verdi, Type.FOLKEREGISTERIDENT, true),
                            Identifikator(gammeltFnr.verdi, Type.FOLKEREGISTERIDENT, false),
                            Identifikator("1234567890123", Type.AKTORID, true),
                        ),
                    ),
                )

                identhendelseRepository.hent(gammeltFnr) shouldBe emptyList()
                identhendelseRepository.hent(nyttFnr) shouldBe emptyList()
            }
        }
    }

    @Test
    fun `behandleIdenthendelse - finnes sak på nytt og gammelt fnr - feiler`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val identhendelseRepository = testDataHelper.identhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val identhendelseService = IdenthendelseService(sakPostgresRepo, identhendelseRepository)
                val gammeltFnr = Fnr.random()
                val nyttFnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = gammeltFnr)
                testDataHelper.persisterSakOgSøknad(
                    fnr = gammeltFnr,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = gammeltFnr),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val sak2 = ObjectMother.nySak(fnr = nyttFnr, saksnummer = Saksnummer.genererSaknummer(løpenr = "1000"))
                testDataHelper.persisterSakOgSøknad(
                    fnr = nyttFnr,
                    sak = sak2,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = nyttFnr),
                        sakId = sak2.id,
                        saksnummer = sak2.saksnummer,
                    ),
                )

                assertFailsWith<IllegalStateException> {
                    identhendelseService.behandleIdenthendelse(
                        Aktor(
                            listOf(
                                Identifikator(nyttFnr.verdi, Type.FOLKEREGISTERIDENT, true),
                                Identifikator(gammeltFnr.verdi, Type.FOLKEREGISTERIDENT, false),
                                Identifikator("1234567890123", Type.AKTORID, true),
                            ),
                        ),
                    )
                }
            }
        }
    }

    @Test
    fun `behandleIdenthendelse - finnes sak på to gamle fnr - feiler`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val identhendelseRepository = testDataHelper.identhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val identhendelseService = IdenthendelseService(sakPostgresRepo, identhendelseRepository)
                val gammeltFnr = Fnr.random()
                val gammeltFnr2 = Fnr.random()
                val nyttFnr = Fnr.random()
                val sak = ObjectMother.nySak(fnr = gammeltFnr)
                testDataHelper.persisterSakOgSøknad(
                    fnr = gammeltFnr,
                    sak = sak,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = gammeltFnr),
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                    ),
                )
                val sak2 = ObjectMother.nySak(fnr = gammeltFnr2, saksnummer = Saksnummer.genererSaknummer(løpenr = "1000"))
                testDataHelper.persisterSakOgSøknad(
                    fnr = gammeltFnr2,
                    sak = sak2,
                    søknad = ObjectMother.nySøknad(
                        personopplysninger = ObjectMother.personSøknad(fnr = gammeltFnr2),
                        sakId = sak2.id,
                        saksnummer = sak2.saksnummer,
                    ),
                )

                assertFailsWith<IllegalStateException> {
                    identhendelseService.behandleIdenthendelse(
                        Aktor(
                            listOf(
                                Identifikator(nyttFnr.verdi, Type.FOLKEREGISTERIDENT, true),
                                Identifikator(gammeltFnr.verdi, Type.FOLKEREGISTERIDENT, false),
                                Identifikator(gammeltFnr2.verdi, Type.FOLKEREGISTERIDENT, false),
                                Identifikator("1234567890123", Type.AKTORID, true),
                            ),
                        ),
                    )
                }

                identhendelseRepository.hent(gammeltFnr) shouldBe emptyList()
                identhendelseRepository.hent(gammeltFnr2) shouldBe emptyList()
            }
        }
    }

    @Test
    fun `behandleIdenthendelse - ingen gjeldende ident - feiler`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            runBlocking {
                val identhendelseRepository = testDataHelper.identhendelseRepository
                val sakPostgresRepo = testDataHelper.sakRepo
                val identhendelseService = IdenthendelseService(sakPostgresRepo, identhendelseRepository)

                assertFailsWith<IllegalArgumentException> {
                    identhendelseService.behandleIdenthendelse(
                        Aktor(
                            listOf(
                                Identifikator(Fnr.random().verdi, Type.FOLKEREGISTERIDENT, false),
                                Identifikator(Fnr.random().verdi, Type.FOLKEREGISTERIDENT, false),
                                Identifikator("1234567890123", Type.AKTORID, true),
                            ),
                        ),
                    )
                }
            }
        }
    }
}
