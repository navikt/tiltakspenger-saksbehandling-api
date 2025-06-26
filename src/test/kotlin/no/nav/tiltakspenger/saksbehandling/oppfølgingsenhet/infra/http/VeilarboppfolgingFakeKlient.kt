package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.VeilarboppfolgingKlient

class VeilarboppfolgingFakeKlient : VeilarboppfolgingKlient {
    override suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor {
        return ObjectMother.navkontor()
    }
}
