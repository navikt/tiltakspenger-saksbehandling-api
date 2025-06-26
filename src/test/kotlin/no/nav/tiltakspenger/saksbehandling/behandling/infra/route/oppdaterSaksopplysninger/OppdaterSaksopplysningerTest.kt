package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdaterSaksopplysninger

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSaksopplysningerForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettBehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltakskilde
import org.junit.jupiter.api.Test

internal class OppdaterSaksopplysningerTest {
    @Test
    fun `saksopplysninger blir oppdatert`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, behandling, _) = opprettBehandlingUnderBehandling(tac)
                behandling.saksopplysninger.fødselsdato shouldBe 1.januar(2001)
                val personopplysningerForBrukerFraPdl = ObjectMother.personopplysningKjedeligFyr(
                    fnr = sak.fnr,
                    fødselsdato = 2.januar(2001),
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
                        deltagelseFraOgMed = behandling.saksopplysningsperiode.fraOgMed,
                        deltagelseTilOgMed = behandling.saksopplysningsperiode.tilOgMed,
                        deltakelseStatus = TiltakDeltakerstatus.Deltar,
                        deltakelseProsent = 100.0f,
                        antallDagerPerUke = 5.0f,
                        kilde = Tiltakskilde.Arena,
                    ),
                )
                val (oppdatertSak, oppdatertBehandling, responseJson) = oppdaterSaksopplysningerForBehandlingId(
                    tac,
                    sak.id,
                    behandling.id,
                )
                oppdatertBehandling.saksopplysninger.fødselsdato shouldBe 2.januar(2001)
            }
        }
    }
}
