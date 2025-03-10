package no.nav.tiltakspenger.vedtak.routes.søknad

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.vedtak.context.ApplicationContext
import no.nav.tiltakspenger.vedtak.felles.april
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.sak.Saksnummer

fun nySøknadForFnr(
    fnr: Fnr,
    deltakelsesperiode: Periode? = null,
    applicationContext: ApplicationContext,
): Saksnummer {
    val periode = deltakelsesperiode ?: Periode(1.april(2025), 10.april(2025))

    return runBlocking {
        val sak = applicationContext.sakContext.sakService.hentEllerOpprettSak(
            fnr,
            ObjectMother.systembrukerLageHendelser(),
            CorrelationId.generate(),
        )
        val søknad = ObjectMother.nySøknad(
            fnr = fnr,
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            periode = periode,
        )
        applicationContext.søknadContext.søknadService.nySøknad(
            søknad = søknad,
            systembruker = ObjectMother.systembrukerLageHendelser(),
        )

        sak.saksnummer
    }
}

fun nySakMedNySøknad(
    deltakelsesperiode: Periode? = null,
    applicationContext: ApplicationContext,
): Saksnummer {
    val periode = deltakelsesperiode ?: Periode(1.april(2025), 10.april(2025))

    return runBlocking {
        val sak = applicationContext.sakContext.sakService.hentEllerOpprettSak(
            Fnr.random(),
            ObjectMother.systembrukerLageHendelser(),
            CorrelationId.generate(),
        )
        val søknad = ObjectMother.nySøknad(
            fnr = sak.fnr,
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            periode = periode,
        )
        applicationContext.søknadContext.søknadService.nySøknad(
            søknad = søknad,
            systembruker = ObjectMother.systembrukerLageHendelser(),
        )
        sak.saksnummer
    }
}
