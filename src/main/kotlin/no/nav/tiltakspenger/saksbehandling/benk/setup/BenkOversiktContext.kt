package no.nav.tiltakspenger.saksbehandling.benk.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.benk.infra.repo.BenkOversiktPostgresRepo
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo
import no.nav.tiltakspenger.saksbehandling.benk.service.BenkOversiktService

open class BenkOversiktContext(
    sessionFactory: SessionFactory,
    tilgangsstyringService: TilgangsstyringService,
) {
    open val benkOversiktRepo: BenkOversiktRepo by lazy {
        BenkOversiktPostgresRepo(sessionFactory as PostgresSessionFactory)
    }
    open val benkOversiktService: BenkOversiktService by lazy {
        BenkOversiktService(tilgangsstyringService = tilgangsstyringService, benkOversiktRepo = benkOversiktRepo)
    }
}
