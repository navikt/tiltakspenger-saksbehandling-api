@file:Suppress("unused", "ktlint")

package db.migration

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresTransactionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.infra.repo.toPGObject
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkStønadDTO.Innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakPostgresRepo
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V176__migrer_statistikk_stonad_innvilgelsesperioder : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val logger = KotlinLogging.logger {}
        val dataSource = context.configuration.dataSource
        val sessionFactory = PostgresSessionFactory(dataSource, SessionCounter(logger))

        sessionFactory.withTransactionContext { tx ->
            tx.withSession { session ->
                val skalOppdateres = hentIderForOppdatering(session)

                logger.info { "Fant ${skalOppdateres.size} StatistikkStønad som skal oppdateres" }

                skalOppdateres.forEach { (statistikkId, vedtakId) ->
                    val vedtak = RammevedtakPostgresRepo.hentForVedtakId(vedtakId, session)

                    if (vedtak == null) {
                        logger.error { "Fant ikke vedtak $vedtakId på statistikk-rad $statistikkId" }
                        throw IllegalStateException("Fant ikke vedtak $vedtakId på statistikk-rad $statistikkId")
                    }

                    val innvilgelsesperioder: List<Innvilgelsesperiode> =
                        vedtak.innvilgelsesperioder!!.periodisering.verdier.map {
                            Innvilgelsesperiode(
                                fraOgMed = it.periode.fraOgMed,
                                tilOgMed = it.periode.tilOgMed,
                                tiltaksdeltakelse = it.valgtTiltaksdeltakelse.eksternDeltakelseId,
                            )
                        }

                    val antallOppdatert = oppdaterStatistikk(statistikkId, innvilgelsesperioder, session)

                    if (antallOppdatert != 1) {
                        logger.error { "Forventet 1 oppdatert rad med id $statistikkId - fikk $antallOppdatert" }
                        throw IllegalStateException("Forventet 1 oppdatert rad med id $statistikkId - fikk $antallOppdatert")
                    }
                }
            }
        }
    }

    fun oppdaterStatistikk(id: String, innvilgelsesperioder: List<Innvilgelsesperiode>, session: Session): Int {
        return session.run(
            sqlQuery(
                """
                    update statistikk_stonad
                    set 
                        innvilgelsesperioder = :innvilgelsesperioder,
                        sist_endret = :sistEndret
                    where id = :id                    
                """.trimIndent(),
                "id" to id,
                "innvilgelsesperioder" to toPGObject(innvilgelsesperioder),
                "sistEndret" to LocalDateTime.now().truncatedTo(ChronoUnit.MICROS),
            ).asUpdate,
        )
    }

    fun hentIderForOppdatering(session: Session): List<Pair<String, VedtakId>> {
        return session.run(
            sqlQuery(
                """
                    select id, vedtak_id from saksbehandling.public.statistikk_stonad 
                    where resultat = 'Innvilgelse' and innvilgelsesperioder->>'tiltaksdeltakelse' is null
                """,
            ).map {
                val statistikkId = it.string("id")
                val vedtakId = VedtakId.fromString(it.string("vedtak_id"))

                statistikkId to vedtakId
            }.asList,
        )
    }
}
