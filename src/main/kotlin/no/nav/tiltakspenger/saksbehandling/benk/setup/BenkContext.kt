package no.nav.tiltakspenger.saksbehandling.benk.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRepo
import no.nav.tiltakspenger.saksbehandling.benk.infra.repo.BenkPostgresRepo
import no.nav.tiltakspenger.saksbehandling.benk.service.BenkService

open class BenkContext(
    sessionFactory: SessionFactory,
    tilgangskontrollService: TilgangskontrollService,
) {
    open val benkRepo: BenkRepo by lazy {
        BenkPostgresRepo(sessionFactory as PostgresSessionFactory)
    }
    open val benkService: BenkService by lazy {
        BenkService(benkRepo = benkRepo, tilgangskontrollService = tilgangskontrollService)
    }
}
