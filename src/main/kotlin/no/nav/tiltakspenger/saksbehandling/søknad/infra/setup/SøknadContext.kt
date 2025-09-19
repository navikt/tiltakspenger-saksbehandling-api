package no.nav.tiltakspenger.saksbehandling.søknad.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.DigitalsøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.DigitalsøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.DigitalsøknadPostgresRepo

open class SøknadContext(
    sessionFactory: SessionFactory,
    sakService: SakService,
) {
    open val digitalsøknadRepo: DigitalsøknadRepo by lazy { DigitalsøknadPostgresRepo(sessionFactory = sessionFactory as PostgresSessionFactory) }
    val digitalsøknadService: DigitalsøknadService by lazy {
        DigitalsøknadService(
            digitalsøknadRepo,
            sessionFactory,
            sakService,
        )
    }
}
