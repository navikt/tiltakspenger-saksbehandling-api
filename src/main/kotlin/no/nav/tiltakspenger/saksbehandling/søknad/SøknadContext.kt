package no.nav.tiltakspenger.saksbehandling.søknad

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadServiceImpl
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.PostgresSøknadRepo

open class SøknadContext(
    sessionFactory: SessionFactory,
    oppgaveGateway: OppgaveGateway,
) {
    open val søknadRepo: SøknadRepo by lazy { PostgresSøknadRepo(sessionFactory = sessionFactory as PostgresSessionFactory) }
    val søknadService: no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService by lazy {
        no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadServiceImpl(
            søknadRepo,
            oppgaveGateway,
        )
    }
}
