package no.nav.tiltakspenger.saksbehandling.statistikk

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortPostgresRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkPostgresRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.StatistikkStønadPostgresRepo
import java.time.Clock

class StatistikkPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) : StatistikkRepo {
    override fun lagre(statistikk: StatistikkDTO, context: TransactionContext?) {
        sessionFactory.withTransaction(context) { tx ->
            statistikk.saksstatistikk.forEach {
                SaksstatistikkPostgresRepo.lagre(it, tx)
            }
            statistikk.stønadsstatistikk.forEach {
                StatistikkStønadPostgresRepo.lagre(it, clock, tx)
            }
            statistikk.meldekortstatistikk.forEach {
                StatistikkMeldekortPostgresRepo.lagre(it, tx)
            }
            statistikk.utbetalingsstatistikk.forEach {
                StatistikkStønadPostgresRepo.lagre(it, tx)
            }
        }
    }

    override fun oppdaterFnr(
        gammeltFnr: Fnr,
        nyttFnr: Fnr,
        clock: Clock,
        transactionContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            SaksstatistikkPostgresRepo.oppdaterFnr(gammeltFnr, nyttFnr, tx)
            StatistikkStønadPostgresRepo.oppdaterFnr(gammeltFnr, nyttFnr, clock, tx)
            StatistikkMeldekortPostgresRepo.oppdaterFnr(gammeltFnr, nyttFnr, clock, tx)
        }
    }

    override fun oppdaterAdressebeskyttelse(sakId: SakId, transactionContext: TransactionContext?) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            SaksstatistikkPostgresRepo.oppdaterAdressebeskyttelse(sakId, tx)
        }
    }
}
