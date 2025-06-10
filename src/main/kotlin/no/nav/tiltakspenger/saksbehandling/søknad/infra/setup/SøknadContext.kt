package no.nav.tiltakspenger.saksbehandling.søknad.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadPostgresRepo

open class SøknadContext(
    sessionFactory: SessionFactory,
    oppgaveKlient: OppgaveKlient,
    sakService: SakService,
) {
    open val søknadRepo: SøknadRepo by lazy { SøknadPostgresRepo(sessionFactory = sessionFactory as PostgresSessionFactory) }
    val søknadService: SøknadService by lazy {
        SøknadService(
            søknadRepo,
            oppgaveKlient,
            sessionFactory,
            sakService,
        )
    }
}
