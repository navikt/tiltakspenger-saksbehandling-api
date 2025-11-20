package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.service

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TiltaksdeltakelseServiceTest {
    private val sakService: SakService = mockk<SakService>()
    private val personService: PersonService = mockk<PersonService>()
    private val tiltaksdeltakelseKlient: TiltaksdeltakelseKlient = mockk<TiltaksdeltakelseKlient>()

    private val tiltaksdeltakelseService: TiltaksdeltakelseService = TiltaksdeltakelseService(
        sakService,
        personService,
        tiltaksdeltakelseKlient,
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Nested
    inner class HentTiltaksdeltakelserForSak {
        val sak = ObjectMother.nySak()
        val person = ObjectMother.personopplysningKjedeligFyr(fnr = sak.fnr)

        @Test
        fun `Får tiltaksdeltakelser som overlapper med periode`() = runTest {
            val oppslagsperiode = Periode(
                fraOgMed = 1.januar(2025),
                tilOgMed = 31.januar(2025),
            )
            val tiltaksdeltakelser = listOf(
                ObjectMother.tiltaksdeltakelseMedArrangørnavn(
                    fom = 1.januar(2025),
                    tom = 31.januar(2025),
                ),
            )

            every { sakService.hentForSakId(sak.id) } returns sak
            coEvery { personService.hentEnkelPersonFnr(sak.fnr) } returns person.right()
            coEvery {
                tiltaksdeltakelseKlient.hentTiltaksdeltakelserMedArrangørnavn(
                    fnr = sak.fnr,
                    harAdressebeskyttelse = any(),
                    correlationId = any(),
                )
            } returns tiltaksdeltakelser

            val tiltaksdeltakelserForPeriode = tiltaksdeltakelseService.hentTiltaksdeltakelserForSak(
                sakId = sak.id,
                fraOgMed = oppslagsperiode.fraOgMed,
                tilOgMed = oppslagsperiode.tilOgMed,
                correlationId = CorrelationId.generate(),
            )

            tiltaksdeltakelserForPeriode.getOrFail().size shouldBe 1
            tiltaksdeltakelserForPeriode.getOrFail().all {
                it.periode?.overlapperMed(oppslagsperiode) == true shouldBe true
            }
        }

        @Test
        fun `Får tiltaksdeltakelser som delvis overlapper med periode`() = runTest {
            val oppslagsperiode = Periode(
                fraOgMed = 1.januar(2025),
                tilOgMed = 31.januar(2025),
            )
            val tiltaksdeltakelser = listOf(
                // Overlapper ikke
                ObjectMother.tiltaksdeltakelseMedArrangørnavn(
                    fom = 14.desember(2024),
                    tom = 1.januar(2025),
                ),
                // Overlapper
                ObjectMother.tiltaksdeltakelseMedArrangørnavn(
                    fom = 1.januar(2025),
                    tom = 31.januar(2025),
                ),
                // Overlapper ikke
                ObjectMother.tiltaksdeltakelseMedArrangørnavn(
                    fom = 31.januar(2025),
                    tom = 14.februar(2025),
                ),
            )

            every { sakService.hentForSakId(sak.id) } returns sak
            coEvery { personService.hentEnkelPersonFnr(sak.fnr) } returns person.right()
            coEvery {
                tiltaksdeltakelseKlient.hentTiltaksdeltakelserMedArrangørnavn(
                    fnr = sak.fnr,
                    harAdressebeskyttelse = any(),
                    correlationId = any(),
                )
            } returns tiltaksdeltakelser

            val tiltaksdeltakelserForPeriode = tiltaksdeltakelseService.hentTiltaksdeltakelserForSak(
                sakId = sak.id,
                fraOgMed = oppslagsperiode.fraOgMed,
                tilOgMed = oppslagsperiode.tilOgMed,
                correlationId = CorrelationId.generate(),
            )

            tiltaksdeltakelserForPeriode.getOrFail().size shouldBe 3
            tiltaksdeltakelserForPeriode.getOrFail().all {
                it.periode?.overlapperMed(oppslagsperiode) == true shouldBe true
            }
        }

        @Test
        fun `Får ikke tiltaksdeltakelser som ikke overlapper med periode`() = runTest {
            val oppslagsperiode = Periode(
                fraOgMed = 1.januar(2025),
                tilOgMed = 31.januar(2025),
            )
            val tiltaksdeltakelser = listOf(
                // Overlapper ikke
                ObjectMother.tiltaksdeltakelseMedArrangørnavn(
                    fom = 1.desember(2024),
                    tom = 31.desember(2024),
                ),
                // Overlapper
                ObjectMother.tiltaksdeltakelseMedArrangørnavn(
                    fom = 1.januar(2025),
                    tom = 31.januar(2025),
                ),
                // Overlapper ikke
                ObjectMother.tiltaksdeltakelseMedArrangørnavn(
                    fom = 1.februar(2025),
                    tom = 28.februar(2025),
                ),
            )

            every { sakService.hentForSakId(sak.id) } returns sak
            coEvery { personService.hentEnkelPersonFnr(sak.fnr) } returns person.right()
            coEvery {
                tiltaksdeltakelseKlient.hentTiltaksdeltakelserMedArrangørnavn(
                    fnr = sak.fnr,
                    harAdressebeskyttelse = any(),
                    correlationId = any(),
                )
            } returns tiltaksdeltakelser

            val tiltaksdeltakelserForPeriode = tiltaksdeltakelseService.hentTiltaksdeltakelserForSak(
                sakId = sak.id,
                fraOgMed = oppslagsperiode.fraOgMed,
                tilOgMed = oppslagsperiode.tilOgMed,
                correlationId = CorrelationId.generate(),
            )

            tiltaksdeltakelserForPeriode.getOrFail().size shouldBe 1
            tiltaksdeltakelserForPeriode.getOrFail().all {
                it.periode?.overlapperMed(oppslagsperiode) == true shouldBe true
            }
        }

        @Test
        fun `Feiler hvis oppslagsperiode ikke er satt`() = runTest {
            tiltaksdeltakelseService.hentTiltaksdeltakelserForSak(
                sakId = sak.id,
                fraOgMed = null,
                tilOgMed = null,
                correlationId = CorrelationId.generate(),
            ) shouldBe KunneIkkeHenteTiltaksdeltakelser.OppslagsperiodeMangler.left()

            tiltaksdeltakelseService.hentTiltaksdeltakelserForSak(
                sakId = sak.id,
                fraOgMed = 1.januar(2025),
                tilOgMed = null,
                correlationId = CorrelationId.generate(),
            ) shouldBe KunneIkkeHenteTiltaksdeltakelser.OppslagsperiodeMangler.left()

            tiltaksdeltakelseService.hentTiltaksdeltakelserForSak(
                sakId = sak.id,
                fraOgMed = null,
                tilOgMed = 1.januar(2025),
                correlationId = CorrelationId.generate(),
            ) shouldBe KunneIkkeHenteTiltaksdeltakelser.OppslagsperiodeMangler.left()
        }

        @Test
        fun `Feiler for negativ oppslagsperiode`() = runTest {
            tiltaksdeltakelseService.hentTiltaksdeltakelserForSak(
                sakId = sak.id,
                fraOgMed = 31.januar(2025),
                tilOgMed = 1.januar(2025),
                correlationId = CorrelationId.generate(),
            ) shouldBe KunneIkkeHenteTiltaksdeltakelser.NegativOppslagsperiode.left()
        }
    }
}
