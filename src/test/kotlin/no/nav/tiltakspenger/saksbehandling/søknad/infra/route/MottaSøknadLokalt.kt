package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad

fun nySøknadForFnr(
    fnr: Fnr,
    deltakelsesperiode: Periode? = null,
    barnetillegg: List<BarnetilleggFraSøknad> = emptyList(),
    applicationContext: ApplicationContext,
): Saksnummer {
    val periode = deltakelsesperiode ?: Periode(1.april(2025), 10.april(2025))

    return runBlocking {
        val sak = applicationContext.sakContext.sakService.hentEllerOpprettSak(
            fnr,
            CorrelationId.generate(),
        )
        val søknad = ObjectMother.nyInnvilgbarSøknad(
            fnr = fnr,
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            periode = periode,
            barnetillegg = barnetillegg,
        )
        applicationContext.søknadContext.søknadService.nySøknad(
            søknad = søknad,
        )

        sak.saksnummer
    }
}

fun nySakMedNySøknad(
    deltakelsesperiode: Periode? = null,
    barnetillegg: List<BarnetilleggFraSøknad> = emptyList(),
    applicationContext: ApplicationContext,
): Saksnummer {
    val periode = deltakelsesperiode ?: Periode(1.april(2025), 10.april(2025))

    return runBlocking {
        val sak = applicationContext.sakContext.sakService.hentEllerOpprettSak(
            Fnr.random(),
            CorrelationId.generate(),
        )
        val søknad = ObjectMother.nyInnvilgbarSøknad(
            fnr = sak.fnr,
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            periode = periode,
            barnetillegg = barnetillegg,
        )
        applicationContext.søknadContext.søknadService.nySøknad(
            søknad = søknad,
        )
        sak.saksnummer
    }
}
