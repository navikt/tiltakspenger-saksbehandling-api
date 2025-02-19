package no.nav.tiltakspenger.vedtak.routes.behandling.oppdaterSaksopplysninger

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltakskilde
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.sendTilBeslutter
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.startBehandling
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.taBehanding
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.oppdaterSaksopplysningerForBehandlingId
import no.nav.tiltakspenger.vedtak.routes.routes
import org.json.JSONObject
import org.junit.jupiter.api.Test

internal class OppdaterSaksopplysningerTest {
    @Test
    fun `saksbehandler kan ta behandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _,behandling,_) = startBehandling(tac)
                behandling.saksopplysninger.fødselsdato shouldBe 1.januar(2001)
                taBehanding(tac, behandling.id, ObjectMother.saksbehandler123())
                val personopplysningerForBrukerFraPdl = ObjectMother.personopplysningKjedeligFyr(
                    fnr = sak.fnr,
                    fødselsdato =  2.januar(2001),
                )
                tac.leggTilPerson(
                    fnr = sak.fnr,
                    personopplysningerForBruker = personopplysningerForBrukerFraPdl,
                    tiltaksdeltagelse = Tiltaksdeltagelse(
                        eksternDeltagelseId = "TA12345",
                        gjennomføringId = null,
                        typeNavn = "Testnavn",
                        typeKode = TiltakstypeSomGirRett.JOBBKLUBB,
                        rettPåTiltakspenger = true,
                        deltakelsesperiode = behandling.,
                        deltakelseStatus = TiltakDeltakerstatus.Deltar,
                        deltakelseProsent = 100.0f,
                        antallDagerPerUke = 5.0f,
                        kilde = Tiltakskilde.Arena,
                    ),
                )
                oppdaterSaksopplysningerForBehandlingId(
                    tac,
                    sak.id,
                    behandlingId,
                    ObjectMother.saksbehandler123(),
                ).also {
                    tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                        it.saksopplysninger. shouldBe Behandlingsstatus.UNDER_BEHANDLING
                    }
                }
            }
        }
    }
}
