package no.nav.tiltakspenger.saksbehandling.db

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak

internal fun TestDataHelper.persisterSak(
    fnr: Fnr = Fnr.random(),
    sak: Sak = ObjectMother.nySak(
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
): Sak {
    this.sakRepo.opprettSak(sak)
    return sakRepo.hentForSakId(sak.id)!!
}
