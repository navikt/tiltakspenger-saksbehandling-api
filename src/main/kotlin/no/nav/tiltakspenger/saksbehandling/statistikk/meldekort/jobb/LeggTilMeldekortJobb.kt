package no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.jobb

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.tilStatistikkMeldekortDTO
import java.time.Clock

class LeggTilMeldekortJobb(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val statistikkMeldekortRepo: StatistikkMeldekortRepo,
) {
    private val log = KotlinLogging.logger { }

    // Jobb for å legge til godkjente meldekortbehandlinger i statistikk-tabellen som har blitt overskrevet.
    // Skal slettes når den har lagret det som mangler.
    fun leggTilManglendeMeldekortStatistikk() {
        sessionFactory.withSessionContext { sessionContext ->
            // hent alle godkjente korrigerte meldekortbehandlinger
            val godkjenteKorrigerteMeldekortbehandlinger = meldekortBehandlingRepo.hentGodkjenteKorrigerteBehandlinger(sessionContext)
            log.info { "Fant ${godkjenteKorrigerteMeldekortbehandlinger.size} godkjente korrigerte meldekort" }

            // henter meldekortbehandlinger for sak for å finne alle meldekortbehandlinger for samme kjede
            godkjenteKorrigerteMeldekortbehandlinger.forEach { korrigertBehandling ->
                log.info { "Henter meldekortbehandlinger for sak ${korrigertBehandling.sakId} for korrigert meldekort ${korrigertBehandling.id}" }
                val meldekortbehandlinger = meldekortBehandlingRepo.hentForSakId(korrigertBehandling.sakId, sessionContext)
                    ?: throw IllegalStateException("Fant ikke meldekortbehandlingen vi nettopp hentet, sakId ${korrigertBehandling.sakId}")

                val andreGodkjenteMeldekortbehandlingerSammeKjede = meldekortbehandlinger.godkjenteMeldekort.filter {
                    it.id != korrigertBehandling.id && it.kjedeId == korrigertBehandling.kjedeId
                }

                andreGodkjenteMeldekortbehandlingerSammeKjede.forEach {
                    // sjekk om finnes i statistikk_meldekort, hvis ikke: lagre
                    if (!finnesIMeldekortStatisikk(it.id, sessionContext)) {
                        statistikkMeldekortRepo.lagre(it.tilStatistikkMeldekortDTO(clock))
                        log.info { "Lagret statistikkinnslag for meldekortbehandling med id ${it.id}" }
                    } else {
                        log.info { "Meldekortbehandling med id ${it.id} finnes i statistikktabellen fra før" }
                    }
                }
                log.info { "Ferdig med å opprette statistikk for ${andreGodkjenteMeldekortbehandlingerSammeKjede.size} behandlinger for korrigert meldekort ${korrigertBehandling.id}" }
            }
        }
    }

    private fun finnesIMeldekortStatisikk(meldekortId: MeldekortId, sessionContext: SessionContext?): Boolean {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                queryOf(
                    "select exists(select 1 from statistikk_meldekort where meldekortbehandling_id = :meldekortbehandling_id)",
                    "meldekortbehandling_id" to meldekortId.toString(),
                ).map { row -> row.boolean("exists") }.asSingle,
            ) ?: false
        }
    }
}
