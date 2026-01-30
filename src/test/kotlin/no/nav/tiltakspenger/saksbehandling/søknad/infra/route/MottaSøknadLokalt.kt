package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerRepo

fun nySøknadForFnr(
    fnr: Fnr,
    deltakelsesperiode: Periode? = null,
    barnetillegg: List<BarnetilleggFraSøknad> = emptyList(),
    applicationContext: ApplicationContext,
): Saksnummer {
    val periode = deltakelsesperiode ?: 1.til(10.april(2025))

    return runBlocking {
        val (sak, _) = applicationContext.sakContext.sakService.hentEllerOpprettSak(
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
    tiltaksdeltakerRepo: TiltaksdeltakerRepo,
): Saksnummer {
    val periode = deltakelsesperiode ?: 1.til(10.april(2025))

    return runBlocking {
        val (sak, _) = applicationContext.sakContext.sakService.hentEllerOpprettSak(
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

        // her lagrer vi tiltaksdeltaker for å unngå feil
        tiltaksdeltakerRepo.lagre(
            eksternId = søknad.tiltak.id,
            id = søknad.tiltak.tiltaksdeltakerId,
            tiltakstype = søknad.tiltak.typeKode,
        )

        applicationContext.søknadContext.søknadService.nySøknad(
            søknad = søknad,
        )
        sak.saksnummer
    }
}
