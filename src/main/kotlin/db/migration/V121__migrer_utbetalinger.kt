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

data class VedtakRow(
    val sakId: SakId,
    val meldekortVedtakId: VedtakId?,
    val rammevedtakId: VedtakId?,
    val forrigeUtbetalingVedtakId: VedtakId?,
    val sendtTilUtbetaling: LocalDateTime?,
    val status: Utbetalingsstatus?,
    val metadata: Forsøkshistorikk?,
    val utbetalingsrespons: String?,
)

class V121__migrer_utbetalinger : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val logger = KotlinLogging.logger {}
        val dataSource = context.configuration.dataSource
        val sessionFactory = PostgresSessionFactory(dataSource, SessionCounter(logger))

        sessionFactory.withTransactionContext { tx ->
            val meldekortvedtak: List<VedtakRow> = tx.withSession { session ->
                session.run(
                    sqlQuery(
                        """
                            select u.*, s.id as sak_id, s.fnr 
                            from meldekortvedtak u 
                            join sak s on s.id = u.sak_id
                        """,
                    ).map { row ->
                        VedtakRow(
                            sakId = SakId.fromString(row.string("sak_id")),
                            meldekortVedtakId = VedtakId.fromString(row.string("id")),
                            rammevedtakId = null,
                            forrigeUtbetalingVedtakId = row.stringOrNull("forrige_vedtak_id")
                                ?.let { VedtakId.fromString(it) },
                            sendtTilUtbetaling = row.localDateTimeOrNull("sendt_til_utbetaling_tidspunkt"),
                            status = row.stringOrNull("status")?.toUtbetalingsstatus(),
                            row.stringOrNull("status_metadata")?.toForsøkshistorikk(),
                            utbetalingsrespons = row.stringOrNull("utbetaling_metadata"),
                        )
                    }.asList,
                )
            }

            logger.info { "Fant ${meldekortvedtak.size} meldekortvedtak" }
        }
    }

    private fun settMeldekortVedtakUtbetaling(
        meldekortVedtakId: VedtakId,
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
                "id" to meldekortVedtakId.toString(),
                "utbetaling_id" to utbetalingId.toString(),
            ).asUpdate,
        )
    }

    private fun opprettUtbetaling(
        utbetalingId: UtbetalingId,
        sakId: SakId,
        meldekortVedtakId: VedtakId?,
        rammevedtakId: VedtakId?,
        forrigeUtbetalingVedtakId: VedtakId?,
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
                            forrige_utbetaling_vedtak_id,
                            sendt_til_utbetaling_tidspunkt,
                            status,
                            status_metadata,          
                            utbetaling_metadata
                        ) values(
                            :id,
                            :sak_id,
                            :meldekortvedtak_id,
                            :rammevedtak_id,
                            :forrige_utbetaling_vedtak_id,
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
                "forrige_utbetaling_vedtak_id" to forrigeUtbetalingVedtakId?.toString(),
                "sendt_til_utbetaling_tidspunkt" to sendtTilUtbetaling,
                "status" to status.toString(),
                "status_metadata" to metadata?.toDbJson(),
                "utbetaling_metadata" to utbetalingsrespons,
            ).asUpdate,
        )
    }
}
