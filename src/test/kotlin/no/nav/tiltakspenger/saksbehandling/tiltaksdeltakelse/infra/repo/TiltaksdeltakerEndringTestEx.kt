package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import kotliquery.Row
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseKilde
import java.time.LocalDateTime

/**
 * En rad i `tiltaksdeltaker_endring` slik den ligger i databasen.
 * Prodkoden leser aldri tabellen; denne finnes kun for å se hva som faktisk ble skrevet.
 */
data class LagretTiltaksdeltakerEndring(
    val id: String,
    val eksternDeltakerId: String,
    val tiltaksdeltakerId: String,
    val sakId: String,
    val kilde: TiltaksdeltakerHendelseKilde,
    val verdi: String?,
    val endring: String?,
    val behandlingId: String?,
    val oppgaveId: String?,
    val opprettet: LocalDateTime,
    val behandletTidspunkt: LocalDateTime?,
)

/**
 * Oppslag mot `tiltaksdeltaker_endring` som kun testene trenger.
 * De hører derfor i testlaget, ikke som `@TestOnly` på [TiltaksdeltakerHendelsePostgresRepo].
 */
fun PostgresSessionFactory.hentTiltaksdeltakerEndringerForEksternId(
    eksternDeltakerId: String,
): List<LagretTiltaksdeltakerEndring> = hent("ekstern_deltaker_id", eksternDeltakerId)

fun PostgresSessionFactory.hentTiltaksdeltakerEndringer(
    tiltaksdeltakerId: TiltaksdeltakerId,
): List<LagretTiltaksdeltakerEndring> = hent("tiltaksdeltaker_id", tiltaksdeltakerId.toString())

private fun PostgresSessionFactory.hent(
    kolonne: String,
    verdi: String,
): List<LagretTiltaksdeltakerEndring> = withSession { session ->
    session.run(
        sqlQuery(
            """
                select *
                from tiltaksdeltaker_endring
                where $kolonne = :verdi
                order by opprettet
            """.trimIndent(),
            "verdi" to verdi,
        ).map { row -> row.tilLagretTiltaksdeltakerEndring() }.asList,
    )
}

private fun Row.tilLagretTiltaksdeltakerEndring() = LagretTiltaksdeltakerEndring(
    id = string("id"),
    eksternDeltakerId = string("ekstern_deltaker_id"),
    tiltaksdeltakerId = string("tiltaksdeltaker_id"),
    sakId = string("sak_id"),
    kilde = TiltaksdeltakerHendelseKilde.valueOf(string("kilde")),
    verdi = stringOrNull("verdi"),
    endring = stringOrNull("endring"),
    behandlingId = stringOrNull("behandling_id"),
    oppgaveId = stringOrNull("oppgave_id"),
    opprettet = localDateTime("opprettet"),
    behandletTidspunkt = localDateTimeOrNull("behandlet_tidspunkt"),
)
