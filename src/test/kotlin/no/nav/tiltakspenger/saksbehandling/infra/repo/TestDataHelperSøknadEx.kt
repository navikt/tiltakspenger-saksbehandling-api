package no.nav.tiltakspenger.saksbehandling.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import java.time.LocalDate
import kotlin.random.Random

internal fun TestDataHelper.persisterSakOgSøknad(
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    sak: Sak = ObjectMother.nySak(
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    søknad: Søknad =
        ObjectMother.nySøknad(
            journalpostId = journalpostId,
            personopplysninger =
            ObjectMother.personSøknad(
                fnr = fnr,
            ),
            søknadstiltak =
            ObjectMother.søknadstiltak(
                deltakelseFom = deltakelseFom,
                deltakelseTom = deltakelseTom,
            ),
            barnetillegg = listOf(),
            sakId = sak.id,
            saksnummer = sak.saksnummer,
        ),
): Søknad {
    this.persisterSak(fnr, sak)
    this.søknadRepo.lagre(søknad)
    return søknadRepo.hentForSøknadId(søknad.id)!!.also {
        it shouldBe søknad
    }
}
