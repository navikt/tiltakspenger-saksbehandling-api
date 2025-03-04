package no.nav.tiltakspenger.vedtak.routes.søknad

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.felles.april
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.vedtak.context.ApplicationContext

fun nySøknadPåEksisterendeSak(
    fnr: Fnr,
    deltakelsesperiode: Periode = Periode(1.april(2025), 10.april(2025)),
    applicationContext: ApplicationContext,
) {
    runBlocking {
        val sak = applicationContext.sakContext.sakService.hentForFnr(fnr, ObjectMother.saksbehandler(), CorrelationId.generate()).getOrFail()
        val søknad = ObjectMother.nySøknad(
            fnr = fnr,
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            periode = deltakelsesperiode,
        )
        applicationContext.søknadContext.søknadService.nySøknad(
            søknad = søknad,
            systembruker = ObjectMother.systembrukerLageHendelser(),
        )
    }
}
