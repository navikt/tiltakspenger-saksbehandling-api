package no.nav.tiltakspenger.fakes.clients

import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.ports.VeilarboppfolgingGateway

class VeilarboppfolgingFakeGateway : VeilarboppfolgingGateway {
    override suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor {
        return ObjectMother.navkontor()
    }
}
