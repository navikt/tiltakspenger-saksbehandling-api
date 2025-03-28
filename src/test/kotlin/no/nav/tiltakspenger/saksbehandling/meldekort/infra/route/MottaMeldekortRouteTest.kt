package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.frameldekortapi.mottaMeldekortRoutes
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class MottaMeldekortRouteTest {
    private fun utfyltMeldekortDTO(meldeperiode: Meldeperiode) = BrukerutfyltMeldekortDTO(
        id = MeldekortId.random().toString(),
        meldeperiodeId = meldeperiode.id.toString(),
        sakId = meldeperiode.sakId.toString(),
        periode = meldeperiode.periode.toDTO(),
        mottatt = LocalDateTime.now(),
        journalpostId = "1234",
        dager = meldeperiode.girRett.entries.associate {
            it.key to (if (it.value) BrukerutfyltMeldekortDTO.Status.DELTATT else BrukerutfyltMeldekortDTO.Status.IKKE_REGISTRERT)
        },
    )

    private suspend fun ApplicationTestBuilder.mottaMeldekortRequest(dto: BrukerutfyltMeldekortDTO) = defaultRequest(
        HttpMethod.Post,
        url {
            protocol = URLProtocol.HTTPS
            path("/meldekort/motta")
        },
    ) {
        setBody(serialize(dto))
    }

    @Test
    fun `Kan lagre meldekort fra bruker`() {
        val tac = TestApplicationContext()
        val meldeperiodeRepo = tac.meldekortContext.meldeperiodeRepo
        val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo

        val meldeperiode = ObjectMother.meldeperiode()
        meldeperiodeRepo.lagre(meldeperiode)

        val dto = utfyltMeldekortDTO(meldeperiode)

        runTest {
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        mottaMeldekortRoutes(
                            mottaBrukerutfyltMeldekortService = tac.mottaBrukerutfyltMeldekortService,
                        )
                    }
                }

                mottaMeldekortRequest(dto).apply {
                    status shouldBe HttpStatusCode.OK
                    brukersMeldekortRepo.hentForMeldekortId(MeldekortId.fromString(dto.id)).shouldNotBeNull()
                }
            }
        }
    }

    @Test
    fun `Skal lagre meldekort fra bruker og ignorere påfølgende requests med samme data, med ok-response`() {
        val tac = TestApplicationContext()
        val meldeperiodeRepo = tac.meldekortContext.meldeperiodeRepo
        val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo

        val meldeperiode = ObjectMother.meldeperiode()
        meldeperiodeRepo.lagre(meldeperiode)

        val dto = utfyltMeldekortDTO(meldeperiode)

        runTest {
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        mottaMeldekortRoutes(
                            mottaBrukerutfyltMeldekortService = tac.mottaBrukerutfyltMeldekortService,
                        )
                    }
                }

                mottaMeldekortRequest(dto).apply {
                    status shouldBe HttpStatusCode.OK
                    brukersMeldekortRepo.hentForMeldekortId(MeldekortId.fromString(dto.id)).shouldNotBeNull()
                }

                mottaMeldekortRequest(dto).apply {
                    status shouldBe HttpStatusCode.OK
                    brukersMeldekortRepo.hentForMeldekortId(MeldekortId.fromString(dto.id)).shouldNotBeNull()
                }

                mottaMeldekortRequest(dto).apply {
                    status shouldBe HttpStatusCode.OK
                    brukersMeldekortRepo.hentForMeldekortId(MeldekortId.fromString(dto.id)).shouldNotBeNull()
                }
            }
        }
    }

    @Test
    fun `Skal gi 409 ved forsøk på lagring av eksisterende meldekort med nye data, og ikke overskrive første lagring`() {
        val tac = TestApplicationContext()
        val meldeperiodeRepo = tac.meldekortContext.meldeperiodeRepo
        val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo

        val meldeperiode = ObjectMother.meldeperiode()
        meldeperiodeRepo.lagre(meldeperiode)

        val dto = utfyltMeldekortDTO(meldeperiode)
        val dtoMedDiff = dto.copy(mottatt = dto.mottatt.minusDays(1))

        runTest {
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        mottaMeldekortRoutes(
                            mottaBrukerutfyltMeldekortService = tac.mottaBrukerutfyltMeldekortService,
                        )
                    }
                }

                mottaMeldekortRequest(dto).apply {
                    status shouldBe HttpStatusCode.OK
                }

                mottaMeldekortRequest(dtoMedDiff).apply {
                    status shouldBe HttpStatusCode.Conflict
                    brukersMeldekortRepo.hentForMeldekortId(MeldekortId.fromString(dto.id))!!.mottatt shouldBe dto.mottatt
                }
            }
        }
    }
}
