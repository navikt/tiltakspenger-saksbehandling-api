package no.nav.tiltakspenger.saksbehandling.fakes.clients

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.VeilarboppfolgingGateway

class VeilarboppfolgingFakeGateway : VeilarboppfolgingGateway {
    override suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor {
        return ObjectMother.navkontor()
    }
}
