package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.setup

import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererUtbetalingsvedtakGateway
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingHttpClient
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingsvedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingGateway
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.JournalførUtbetalingsvedtakService
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.OppdaterUtbetalingsstatusService
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SendUtbetalingerService
import java.time.Clock

open class UtbetalingContext(
    sessionFactory: SessionFactory,
    genererUtbetalingsvedtakGateway: GenererUtbetalingsvedtakGateway,
    journalførMeldekortGateway: JournalførMeldekortGateway,
    navIdentClient: NavIdentClient,
    entraIdSystemtokenClient: EntraIdSystemtokenClient,
    sakRepo: SakRepo,
    clock: Clock,
) {
    open val utbetalingGateway: UtbetalingGateway by lazy {
        UtbetalingHttpClient(
            baseUrl = Configuration.utbetalingUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.utbetalingScope) },
        )
    }
    open val utbetalingsvedtakRepo: UtbetalingsvedtakRepo by lazy {
        UtbetalingsvedtakPostgresRepo(
            sessionFactory as PostgresSessionFactory,
        )
    }
    val sendUtbetalingerService: SendUtbetalingerService by lazy {
        SendUtbetalingerService(
            utbetalingsvedtakRepo = utbetalingsvedtakRepo,
            utbetalingsklient = utbetalingGateway,
            clock = clock,
        )
    }
    val journalførUtbetalingsvedtakService by lazy {
        JournalførUtbetalingsvedtakService(
            utbetalingsvedtakRepo = utbetalingsvedtakRepo,
            journalførMeldekortGateway = journalførMeldekortGateway,
            genererUtbetalingsvedtakGateway = genererUtbetalingsvedtakGateway,
            navIdentClient = navIdentClient,
            sakRepo = sakRepo,
            clock = clock,
        )
    }

    val oppdaterUtbetalingsstatusService by lazy {
        OppdaterUtbetalingsstatusService(
            utbetalingsvedtakRepo = utbetalingsvedtakRepo,
            utbetalingGateway = utbetalingGateway,
            clock = clock,
        )
    }
}
