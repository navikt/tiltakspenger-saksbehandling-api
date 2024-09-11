package no.nav.tiltakspenger.vedtak.context

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.service.SøknadServiceImpl
import no.nav.tiltakspenger.vedtak.repository.søknad.PostgresSøknadRepo

open class SøknadContext(
    sessionFactory: SessionFactory,
) {
    open val søknadRepo: SøknadRepo by lazy { PostgresSøknadRepo(sessionFactory = sessionFactory as PostgresSessionFactory) }
    val søknadService: SøknadService by lazy { SøknadServiceImpl(søknadRepo) }
}
