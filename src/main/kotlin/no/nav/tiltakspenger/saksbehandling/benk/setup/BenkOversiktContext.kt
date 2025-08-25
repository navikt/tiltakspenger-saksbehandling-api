package no.nav.tiltakspenger.saksbehandling.benk.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.benk.infra.repo.BenkOversiktPostgresRepo
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo
import no.nav.tiltakspenger.saksbehandling.benk.service.BenkOversiktService

open class BenkOversiktContext(
    sessionFactory: SessionFactory,
    tilgangskontrollService: TilgangskontrollService,
) {
    open val benkOversiktRepo: BenkOversiktRepo by lazy {
        BenkOversiktPostgresRepo(sessionFactory as PostgresSessionFactory)
    }
    open val benkOversiktService: BenkOversiktService by lazy {
        BenkOversiktService(benkOversiktRepo = benkOversiktRepo, tilgangskontrollService = tilgangskontrollService)
    }
}
