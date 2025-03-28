package no.nav.tiltakspenger.saksbehandling.context

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.repository.søknad.PostgresSøknadRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.SøknadServiceImpl

open class SøknadContext(
    sessionFactory: SessionFactory,
    oppgaveGateway: OppgaveGateway,
) {
    open val søknadRepo: SøknadRepo by lazy { PostgresSøknadRepo(sessionFactory = sessionFactory as PostgresSessionFactory) }
    val søknadService: SøknadService by lazy { SøknadServiceImpl(søknadRepo, oppgaveGateway) }
}
