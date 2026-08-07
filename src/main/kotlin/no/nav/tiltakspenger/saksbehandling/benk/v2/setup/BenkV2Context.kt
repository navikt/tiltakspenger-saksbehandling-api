package no.nav.tiltakspenger.saksbehandling.benk.v2.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Repo
import no.nav.tiltakspenger.saksbehandling.benk.v2.infra.repo.BenkV2PostgresRepo
import no.nav.tiltakspenger.saksbehandling.benk.v2.service.BenkV2Service

open class BenkV2Context(
    sessionFactory: SessionFactory,
    tilgangskontrollService: TilgangskontrollService,
) {
    open val benkV2Repo: BenkV2Repo by lazy {
        BenkV2PostgresRepo(sessionFactory as PostgresSessionFactory)
    }
    open val benkV2Service: BenkV2Service by lazy {
        BenkV2Service(benkV2Repo = benkV2Repo, tilgangskontrollService = tilgangskontrollService)
    }
}
