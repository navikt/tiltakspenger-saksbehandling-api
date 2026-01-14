package no.nav.tiltakspenger.saksbehandling.klage.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.klage.service.OppdaterKlagebehandlingFormkravService
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettKlagebehandlingService
import java.time.Clock

open class KlagebehandlingContext(
    private val sessionFactory: SessionFactory,
    private val sakService: SakService,
    private val clock: Clock,
    private val validerJournalpostService: ValiderJournalpostService,
) {
    open val klageRepo: KlagebehandlingRepo by lazy {
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

    open val oppdaterKlagebehandlingFormkravService: OppdaterKlagebehandlingFormkravService by lazy {
        OppdaterKlagebehandlingFormkravService(
            sakService = sakService,
            clock = clock,
            validerJournalpostService = validerJournalpostService,
            klageRepo = klageRepo,
        )
    }
}
