package no.nav.tiltakspenger.saksbehandling.søknad

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.SøknadServiceImpl
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.PostgresSøknadRepo

open class SøknadContext(
    sessionFactory: SessionFactory,
    oppgaveGateway: OppgaveGateway,
) {
    open val søknadRepo: SøknadRepo by lazy { PostgresSøknadRepo(sessionFactory = sessionFactory as PostgresSessionFactory) }
    val søknadService: SøknadService by lazy { SøknadServiceImpl(søknadRepo, oppgaveGateway) }
}
