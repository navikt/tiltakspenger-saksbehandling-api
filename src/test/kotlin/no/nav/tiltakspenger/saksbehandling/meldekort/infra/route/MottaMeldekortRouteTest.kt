package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nySakMedVedtak
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaMeldekortRequest
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tilUtfyltFraBruker
import org.junit.jupiter.api.Test

internal class MottaMeldekortRouteTest {

    @Test
    fun `Kan lagre meldekort fra bruker`() {
        runTest {
            withTestApplicationContext { tac ->
                val (sak) = nySakMedVedtak()
                tac.sakContext.sakRepo.opprettSak(sak)

                val meldeperiode = ObjectMother.meldeperiode(sakId = sak.id)
                tac.meldekortContext.meldeperiodeRepo.lagre(meldeperiode)

                val (_, brukersMeldekort) = mottaMeldekortRequest(
                    tac = tac,
                    meldeperiodeId = meldeperiode.id,
                    sakId = meldeperiode.sakId,
                    dager = meldeperiode.tilUtfyltFraBruker(),
                )
                brukersMeldekort.shouldNotBeNull()
            }
        }
    }

    @Test
    fun `Skal lagre meldekort fra bruker og ignorere påfølgende requests med samme data, med ok-response`() {
        withTestApplicationContext { tac ->
            val (sak) = nySakMedVedtak()
            tac.sakContext.sakRepo.opprettSak(sak)

            val meldeperiode = ObjectMother.meldeperiode(sakId = sak.id)
            tac.meldekortContext.meldeperiodeRepo.lagre(meldeperiode)

            val meldekortId = MeldekortId.random()
            val mottatt = nå(tac.clock)

            repeat(3) { requestNum ->
                val (_, brukersMeldekort) = mottaMeldekortRequest(
                    tac = tac,
                    meldeperiodeId = meldeperiode.id,
                    sakId = meldeperiode.sakId,
                    id = meldekortId,
                    dager = meldeperiode.tilUtfyltFraBruker(),
                    mottatt = mottatt,
                ) {
                    if (requestNum > 1) {
                        it shouldContain "allerede lagret med samme data"
                    }
                }

                brukersMeldekort.shouldNotBeNull()
            }
        }
    }

    @Test
    fun `Skal gi 409 ved forsøk på lagring av eksisterende meldekort med nye data, og ikke overskrive første lagring`() {
        runTest {
            withTestApplicationContext { tac ->
                val (sak) = nySakMedVedtak()
                tac.sakContext.sakRepo.opprettSak(sak)

                val meldeperiode = ObjectMother.meldeperiode(sakId = sak.id)
                tac.meldekortContext.meldeperiodeRepo.lagre(meldeperiode)

                val meldekortId = MeldekortId.random()
                val mottatt = nå(tac.clock)

                mottaMeldekortRequest(
                    tac = tac,
                    meldeperiodeId = meldeperiode.id,
                    sakId = meldeperiode.sakId,
                    id = meldekortId,
                    dager = meldeperiode.tilUtfyltFraBruker(),
                    mottatt = mottatt,
                )

                val (_, brukersMeldekort) = mottaMeldekortRequest(
                    tac = tac,
                    meldeperiodeId = meldeperiode.id,
                    sakId = meldeperiode.sakId,
                    id = meldekortId,
                    dager = meldeperiode.tilUtfyltFraBruker(),
                    mottatt = mottatt.minusDays(1),
                    forventetStatus = HttpStatusCode.Conflict,
                ) {
                    it shouldContain "allerede lagret med andre data"
                }

                brukersMeldekort!!.mottatt shouldBe mottatt
            }
        }
    }
}
