@file:Suppress("unused", "ktlint")

package db.migration

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresTransactionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toForsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.toUtbetalingsstatus
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.time.LocalDateTime

data class UtbetalingsvedtakRow(
    val utbetalingId: UtbetalingId,
    val sakId: SakId,
    val vedtakId: VedtakId,
    val rammevedtakId: VedtakId?,
    val forrigeVedtakId: VedtakId?,
    val sendtTilUtbetaling: LocalDateTime?,
    val status: Utbetalingsstatus?,
    val metadata: Forsøkshistorikk?,
    val utbetalingsrespons: String?,
)

class V122__migrer_utbetalinger : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val logger = KotlinLogging.logger {}
        val dataSource = context.configuration.dataSource
        val sessionFactory = PostgresSessionFactory(dataSource, SessionCounter(logger))

        sessionFactory.withTransactionContext { tx ->
            val tidligereUtbetalingsvedtak: List<UtbetalingsvedtakRow> = tx.withSession { session ->
                session.run(
                    sqlQuery(
                        """
                            select mv.*, r.id as rammevedtak_id, s.fnr 
                            from meldekortvedtak mv 
                            join sak s on s.id = mv.sak_id
                            left join rammevedtak r on r.behandling_id = mv.behandling_id
                        """,
                    ).map { row ->
                        UtbetalingsvedtakRow(
                            utbetalingId = UtbetalingId.random(),
                            sakId = SakId.fromString(row.string("sak_id")),
                            vedtakId = VedtakId.fromString(row.string("id")),
                            rammevedtakId = row.stringOrNull("rammevedtak_id")?.let { VedtakId.fromString(it) },
                            forrigeVedtakId = row.stringOrNull("forrige_vedtak_id")
                                ?.let { VedtakId.fromString(it) },
                            sendtTilUtbetaling = row.localDateTimeOrNull("sendt_til_utbetaling_tidspunkt"),
                            status = row.stringOrNull("status")?.toUtbetalingsstatus(),
                            row.stringOrNull("status_metadata")?.toForsøkshistorikk(),
                            utbetalingsrespons = row.stringOrNull("utbetaling_metadata"),
                        )
                    }.asList,
                )
            }

            logger.info { "Fant ${tidligereUtbetalingsvedtak.size} tidligere utbetalingsvedtak" }

            tx.withSession { session ->
                tidligereUtbetalingsvedtak.forEach { vedtak ->
                    val erRammevedtak = vedtak.rammevedtakId != null
                    val utbetalingId = vedtak.utbetalingId

                    val forrigeVedtak = vedtak.forrigeVedtakId?.let { forrigeVedtakId ->
                        val forrigeVedtak = tidligereUtbetalingsvedtak.find { it.vedtakId == forrigeVedtakId }
                        requireNotNull(forrigeVedtak) {
                            "Fant ikke forrige vedtak $forrigeVedtakId for $vedtak"
                        }

                        forrigeVedtak
                    }

                    opprettUtbetaling(
                        utbetalingId = utbetalingId,
                        sakId = vedtak.sakId,
                        meldekortVedtakId = if (erRammevedtak) null else vedtak.vedtakId,
                        rammevedtakId = vedtak.rammevedtakId,
                        forrigeUtbetalingId = forrigeVedtak?.utbetalingId,
                        sendtTilUtbetaling = vedtak.sendtTilUtbetaling,
                        status = vedtak.status,
                        metadata = vedtak.metadata,
                        utbetalingsrespons = vedtak.utbetalingsrespons,
                        session = session,
                    )

                    if (erRammevedtak) {
                        settRammevedtakUtbetaling(vedtak.rammevedtakId, utbetalingId, session)
                        logger.info { "Oppdaterte rammevedtake ${vedtak.rammevedtakId} med utbetaling $utbetalingId" }
                    } else {
                        settMeldekortVedtakUtbetaling(vedtak.vedtakId, utbetalingId, session)
                        logger.info { "Oppdaterte meldekortvedtaket ${vedtak.vedtakId} med utbetaling $utbetalingId" }
                    }

                }
            }
        }
    }

    private fun settMeldekortVedtakUtbetaling(
        vedtakId: VedtakId,
        utbetalingId: UtbetalingId,
        session: Session,
    ) {
        session.run(
            sqlQuery(
                """
                        update meldekortvedtak
                        set utbetaling_id = :utbetaling_id
                        where id = :id
                    """,
                "id" to vedtakId.toString(),
                "utbetaling_id" to utbetalingId.toString(),
            ).asUpdate,
        )
    }

    private fun settRammevedtakUtbetaling(
        vedtakId: VedtakId,
        utbetalingId: UtbetalingId,
        session: Session,
    ) {
        session.run(
            sqlQuery(
                """
                        update rammevedtak
                        set utbetaling_id = :utbetaling_id
                        where id = :id
                    """,
                "id" to vedtakId.toString(),
                "utbetaling_id" to utbetalingId.toString(),
            ).asUpdate,
        )
    }

    private fun opprettUtbetaling(
        utbetalingId: UtbetalingId,
        sakId: SakId,
        meldekortVedtakId: VedtakId?,
        rammevedtakId: VedtakId?,
        forrigeUtbetalingId: UtbetalingId?,
        sendtTilUtbetaling: LocalDateTime?,
        status: Utbetalingsstatus?,
        metadata: Forsøkshistorikk?,
        utbetalingsrespons: String?,
        session: Session,
    ) {
        session.run(
            sqlQuery(
                """
                        insert into utbetaling (
                            id,
                            sak_id,
                            meldekortvedtak_id,
                            rammevedtak_id,
                            forrige_utbetaling_id,
                            sendt_til_utbetaling_tidspunkt,
                            status,
                            status_metadata,          
                            utbetaling_metadata
                        ) values(
                            :id,
                            :sak_id,
                            :meldekortvedtak_id,
                            :rammevedtak_id,
                            :forrige_utbetaling_id,
                            :sendt_til_utbetaling_tidspunkt,
                            :status,
                            to_jsonb(:status_metadata::jsonb), 
                            to_jsonb(:utbetaling_metadata::jsonb)      
                        )
                    """,
                "id" to utbetalingId.toString(),
                "sak_id" to sakId.toString(),
                "meldekortvedtak_id" to meldekortVedtakId?.toString(),
                "rammevedtak_id" to rammevedtakId?.toString(),
                "forrige_utbetaling_id" to forrigeUtbetalingId?.toString(),
                "sendt_til_utbetaling_tidspunkt" to sendtTilUtbetaling,
                "status" to status.toString(),
                "status_metadata" to metadata?.toDbJson(),
                "utbetaling_metadata" to utbetalingsrespons,
            ).asUpdate,
        )
    }
}
