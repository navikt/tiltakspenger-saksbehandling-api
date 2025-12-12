package no.nav.tiltakspenger.saksbehandling.klage.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettKlagebehandlingService
import java.time.Clock

open class KlagebehandlingContext(
    private val sessionFactory: SessionFactory,
    private val sakService: SakService,
    private val clock: Clock,
    private val validerJournalpostService: ValiderJournalpostService,
) {
    open val klageRepo: KlagebehandlingFakeRepo by lazy {
        KlagebehandlingPostgresRepo(sessionFactory as PostgresSessionFactory)
    }
    open val opprettKlagebehandlingService: OpprettKlagebehandlingService by lazy {
        OpprettKlagebehandlingService(
            sakService = sakService,
            clock = clock,
            validerJournalpostService = validerJournalpostService,
            klageRepo = klageRepo,
        )
    }
}
