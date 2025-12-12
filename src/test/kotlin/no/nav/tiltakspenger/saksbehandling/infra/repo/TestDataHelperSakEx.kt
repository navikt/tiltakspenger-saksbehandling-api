package no.nav.tiltakspenger.saksbehandling.infra.repo

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import java.util.UUID.randomUUID

/**
 * @param fnr brukes ikke dersom [sak] er oppgitt.
 * @param sakId brukes ikke dersom [sak] er oppgitt.
 * @param saksnummer brukes ikke dersom [sak] er oppgitt.
 */
internal fun TestDataHelper.persisterSak(
    fnr: Fnr = Fnr.random(),
    sakId: SakId = SakId.random(),
    saksnummer: Saksnummer = this.saksnummerGenerator.neste(),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
    ),
): Sak {
    this.sakRepo.opprettSak(sak)
    return sakRepo.hentForSakId(sak.id)!!
}
