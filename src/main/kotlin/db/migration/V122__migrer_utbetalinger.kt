@file:Suppress("unused", "ktlint")

package db.migration

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresTransactionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toForsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
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
    val meldekortId: MeldekortId?,
    val opprettet: LocalDateTime,
    val rammevedtakId: VedtakId?,
    val forrigeVedtakId: VedtakId?,
    val sendtTilUtbetaling: LocalDateTime?,
    val status: Utbetalingsstatus?,
    val metadata: Forsøkshistorikk?,
    val utbetalingsrespons: String?,
    val journalpostId: JournalpostId?,
    val journalføringstidspunkt: LocalDateTime?,
)

class V122__migrer_utbetalinger : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val logger = KotlinLogging.logger {}
        val dataSource = context.configuration.dataSource
        val sessionFactory = PostgresSessionFactory(dataSource, SessionCounter(logger))

        sessionFactory.withTransactionContext { tx ->
            val utbetalingsvedtak: List<UtbetalingsvedtakRow> = tx.withSession { session ->
                session.run(
                    sqlQuery(
                        """
                            select uv.*, r.id as rammevedtak_id, s.fnr
                            from utbetalingsvedtak uv 
                            join sak s on s.id = uv.sak_id
                            left join rammevedtak r on r.behandling_id = uv.behandling_id
                            order by uv.opprettet
                        """,
                    ).map { row ->
                        val vedtakId = VedtakId.fromString(row.string("id"))
                        // uuid-delen av vedtak-iden brukes som id for å kjede utbetalingene på en sak av helved.
                        // Beholder den her for ikke å bryte eksisterende kjeder
                        val utbetalingId = UtbetalingId.fromString("utbetaling_${vedtakId.ulidPart()}")

                        UtbetalingsvedtakRow(
                            utbetalingId = utbetalingId,
                            sakId = SakId.fromString(row.string("sak_id")),
                            vedtakId = vedtakId,
                            rammevedtakId = row.stringOrNull("rammevedtak_id")?.let { VedtakId.fromString(it) },
                            forrigeVedtakId = row.stringOrNull("forrige_vedtak_id")
                                ?.let { VedtakId.fromString(it) },
                            sendtTilUtbetaling = row.localDateTimeOrNull("sendt_til_utbetaling_tidspunkt"),
                            status = row.stringOrNull("status")?.toUtbetalingsstatus(),
                            metadata = row.stringOrNull("status_metadata")?.toForsøkshistorikk(),
                            utbetalingsrespons = row.stringOrNull("utbetaling_metadata"),
                            opprettet = row.localDateTime("opprettet"),
                            meldekortId = row.stringOrNull("meldekort_id")?.let { MeldekortId.fromString(it) },
                            journalpostId = row.stringOrNull("journalpost_id")?.let { JournalpostId(it) },
                            journalføringstidspunkt = row.localDateTimeOrNull("journalføringstidspunkt"),
                        )
                    }.asList,
                )
            }

            logger.info { "Fant ${utbetalingsvedtak.size} utbetalingsvedtak" }

            tx.withSession { session ->
                utbetalingsvedtak.forEach { vedtak ->
                    val erRammevedtak = vedtak.rammevedtakId != null
                    val utbetalingId = vedtak.utbetalingId

                    val forrigeVedtak = vedtak.forrigeVedtakId?.let { forrigeVedtakId ->
                        val forrigeVedtak = utbetalingsvedtak.find { it.vedtakId == forrigeVedtakId }
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
                        opprettet = vedtak.opprettet,
                        session = session,
                    )

                    if (erRammevedtak) {
                        settRammevedtakUtbetaling(vedtak.rammevedtakId, utbetalingId, session)
                        logger.info { "Oppdaterte rammevedtaket ${vedtak.rammevedtakId} med utbetaling $utbetalingId" }
                    } else {
                        opprettMeldekortVedtak(
                            vedtakId = vedtak.vedtakId,
                            utbetalingId = vedtak.utbetalingId,
                            sakId = vedtak.sakId,
                            opprettet = vedtak.opprettet,
                            meldekortId = vedtak.meldekortId!!,
                            journalpostId = vedtak.journalpostId,
                            journalføringstidspunkt = vedtak.journalføringstidspunkt,
                            session = session,
                        )
                        logger.info { "Oppdaterte meldekortvedtaket ${vedtak.vedtakId} med utbetaling $utbetalingId" }
                    }

                }
            }
        }
    }

    private fun opprettMeldekortVedtak(
        vedtakId: VedtakId,
        utbetalingId: UtbetalingId,
        sakId: SakId,
        opprettet: LocalDateTime,
        meldekortId: MeldekortId,
        journalpostId: JournalpostId?,
        journalføringstidspunkt: LocalDateTime?,
        session: Session,
    ) {
        session.run(
            sqlQuery(
                """
                    insert into meldekortvedtak (
                        id,
                        utbetaling_id,
                        sak_id,
                        opprettet,
                        meldekort_id,
                        journalpost_id,
                        journalføringstidspunkt
                    ) values (
                        :id,
                        :utbetaling_id,
                        :sak_id,
                        :opprettet,
                        :meldekort_id,
                        :journalpost_id,
                        :journalføringstidspunkt
                    )
                    """,
                "id" to vedtakId.toString(),
                "utbetaling_id" to utbetalingId.toString(),
                "sak_id" to sakId.toString(),
                "opprettet" to opprettet,
                "meldekort_id" to meldekortId.toString(),
                "journalpost_id" to journalpostId?.toString(),
                "journalføringstidspunkt" to journalføringstidspunkt,
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
        opprettet: LocalDateTime,
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
                            utbetaling_metadata,
                            opprettet
                        ) values(
                            :id,
                            :sak_id,
                            :meldekortvedtak_id,
                            :rammevedtak_id,
                            :forrige_utbetaling_id,
                            :sendt_til_utbetaling_tidspunkt,
                            :status,
                            to_jsonb(:status_metadata::jsonb), 
                            to_jsonb(:utbetaling_metadata::jsonb),
                            :opprettet
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
                "opprettet" to opprettet,
            ).asUpdate,
        )
    }
}
