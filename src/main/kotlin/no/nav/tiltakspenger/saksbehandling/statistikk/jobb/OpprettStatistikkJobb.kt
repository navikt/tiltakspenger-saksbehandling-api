package no.nav.tiltakspenger.saksbehandling.statistikk.jobb

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresTransactionContext.Companion.withSession
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortBehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.tilStatistikkMeldekortDTO
import java.time.Clock

class OpprettStatistikkJobb(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
    private val statistikkMeldekortRepo: StatistikkMeldekortRepo,
) {
    private val log = KotlinLogging.logger { }

    fun opprettMeldekortStatistikk() {
        sessionFactory.withTransactionContext { tx ->
            tx.withSession { session ->
                val iverksatteMeldekortbehandlinger = MeldekortBehandlingPostgresRepo.hentIverksatte(session)
                log.info { "Hentet ut ${iverksatteMeldekortbehandlinger.size} meldekortbehandlinger som kanskje mangler statistikk" }

                iverksatteMeldekortbehandlinger.forEach {
                    log.info { "Behandler meldekortbehandling for id ${it.id}, sakid ${it.sakId}, kjedeId ${it.kjedeId}" }
                    if (meldekortStatistikkFinnesIkke(it.sakId, it.kjedeId, session)) {
                        log.info { "Statistikkinnslag for meldekortbehandling med id ${it.id}, sakid ${it.sakId}, kjedeId ${it.kjedeId} finnes ikke" }
                        if (it is MeldekortBehandling.Behandlet) {
                            statistikkMeldekortRepo.lagre(it.tilStatistikkMeldekortDTO(clock), tx)
                            log.info { "Lagret statistikkinnslag for meldekortbehandling med id ${it.id}, sakid ${it.sakId}, kjedeId ${it.kjedeId}" }
                        } else {
                            throw IllegalStateException("Skal ikke opprette statistikk for meldekort som ikke er behandlet!")
                        }
                    } else {
                        log.info { "Statistikkinnslag for meldekortbehandling med id ${it.id}, sakid ${it.sakId}, kjedeId ${it.kjedeId} finnes fra før" }
                    }
                }
            }
        }
    }

    private fun meldekortStatistikkFinnesIkke(
        sakId: SakId,
        meldeperiodeKjedeId: MeldeperiodeKjedeId,
        session: Session,
    ): Boolean {
        return session.run(
            queryOf(
                """
                    select meldekortbehandling_id
                    from statistikk_meldekort
                    where meldeperiode_kjede_id = :meldeperiode_kjede_id 
                    and sak_id = :sak_id
                    """,
                mapOf(
                    "meldeperiode_kjede_id" to meldeperiodeKjedeId.toString(),
                    "sak_id" to sakId.toString(),
                ),
            ).map { row -> row.string("meldekortbehandling_id") }.asList,
        ).isEmpty()
    }
}
