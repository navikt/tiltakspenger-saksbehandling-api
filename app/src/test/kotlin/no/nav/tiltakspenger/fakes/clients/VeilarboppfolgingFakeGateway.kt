package no.nav.tiltakspenger.fakes.clients

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.vedtak.felles.Navkontor
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.VeilarboppfolgingGateway

class VeilarboppfolgingFakeGateway : VeilarboppfolgingGateway {
    override suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor {
        return ObjectMother.navkontor()
    }
}
