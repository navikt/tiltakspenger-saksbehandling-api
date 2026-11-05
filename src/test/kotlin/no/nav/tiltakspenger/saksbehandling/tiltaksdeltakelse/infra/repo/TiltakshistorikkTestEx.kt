package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.LocalDateTime

/** Et innslag i `tiltakshistorikk` slik det ligger i databasen. */
data class LagretTiltakshistorikkinnslag(
    val id: String,
    val sakId: String,
    val tiltaksdeltakerId: String,
    val eksternDeltakerId: String,
    val innhentet: LocalDateTime,
    val verdi: String,
    val endring: String?,
    val behandlingId: String?,
    val oppgaveId: String?,
)

/**
 * Oppslag mot `tiltakshistorikk` som kun testene trenger, sortert etter når innslagene ble innhentet.
 * De hører derfor i testlaget, ikke som `@TestOnly` på [TiltakshistorikkPostgresRepo].
 */
fun PostgresSessionFactory.hentTiltakshistorikk(
    tiltaksdeltakerId: TiltaksdeltakerId,
): List<LagretTiltakshistorikkinnslag> = withSession { session ->
    session.run(
        sqlQuery(
            """
                SELECT *
                FROM tiltakshistorikk
                WHERE tiltaksdeltaker_id = :tiltaksdeltaker_id
                ORDER BY innhentet, id
            """.trimIndent(),
            "tiltaksdeltaker_id" to tiltaksdeltakerId.toString(),
        ).map { row ->
            LagretTiltakshistorikkinnslag(
                id = row.string("id"),
                sakId = row.string("sak_id"),
                tiltaksdeltakerId = row.string("tiltaksdeltaker_id"),
                eksternDeltakerId = row.string("ekstern_deltaker_id"),
                innhentet = row.localDateTime("innhentet"),
                verdi = row.string("verdi"),
                endring = row.stringOrNull("endring"),
                behandlingId = row.stringOrNull("behandling_id"),
                oppgaveId = row.stringOrNull("oppgave_id"),
            )
        }.asList,
    )
}
