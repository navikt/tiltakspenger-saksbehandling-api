package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.TiltakshistorikkId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.TiltakshistorikkRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.Tiltakshistorikkinnslag

/** Lagrer og leser `tiltakshistorikk`. */
class TiltakshistorikkPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : TiltakshistorikkRepo {

    override fun lagre(innslag: Tiltakshistorikkinnslag, sessionContext: SessionContext) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        INSERT INTO tiltakshistorikk (
                            id,
                            sak_id,
                            tiltaksdeltaker_id,
                            ekstern_deltaker_id,
                            innhentet,
                            verdi,
                            endring,
                            behandling_id,
                            oppgave_id
                        ) VALUES (
                            :id,
                            :sak_id,
                            :tiltaksdeltaker_id,
                            :ekstern_deltaker_id,
                            :innhentet,
                            :verdi::jsonb,
                            :endring::jsonb,
                            :behandling_id,
                            :oppgave_id
                        )
                    """.trimIndent(),
                    "id" to innslag.id.toString(),
                    "sak_id" to innslag.sakId.toString(),
                    "tiltaksdeltaker_id" to innslag.tiltaksdeltakerId.toString(),
                    "ekstern_deltaker_id" to innslag.eksternDeltakerId,
                    "innhentet" to innslag.innhentet,
                    "verdi" to serialize(innslag.verdi.tilNåtilstandDbJson()),
                    "endring" to innslag.endring?.toDbJson(),
                    "behandling_id" to innslag.behandlingId?.toString(),
                    "oppgave_id" to innslag.oppgaveId?.toString(),
                ).asUpdate,
            )
        }
    }

    override fun hentSisteVerdi(
        tiltaksdeltakerId: TiltaksdeltakerId,
        sessionContext: SessionContext,
    ): Tiltaksdeltakelse.GirRett? =
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        SELECT verdi
                        FROM tiltakshistorikk
                        WHERE tiltaksdeltaker_id = :tiltaksdeltaker_id
                        ORDER BY innhentet DESC, id DESC
                        LIMIT 1
                    """.trimIndent(),
                    "tiltaksdeltaker_id" to tiltaksdeltakerId.toString(),
                ).map { row -> deserialize<TiltaksdeltakelseNåtilstandDbJson>(row.string("verdi")).tilDomene() }.asSingle,
            )
        }

    override fun hentForSakId(sakId: SakId, sessionContext: SessionContext?): List<Tiltakshistorikkinnslag> =
        sessionFactory.withSession(sessionContext) { session -> hentForSakId(sakId, session) }

    companion object {
        private val log = KotlinLogging.logger {}

        /**
         * Innslag som ikke kan leses, hoppes over og logges, slik at de ikke hindrer uthenting av hele saken.
         * Det gjelder blant annet innslag som ble lagret i et eldre format før V274 kopierte dem hit.
         */
        fun hentForSakId(sakId: SakId, session: Session): List<Tiltakshistorikkinnslag> =
            session.run(
                sqlQuery(
                    """
                        SELECT *
                        FROM tiltakshistorikk
                        WHERE sak_id = :sak_id
                        ORDER BY innhentet, id
                    """.trimIndent(),
                    "sak_id" to sakId.toString(),
                ).map { row -> row.tilTiltakshistorikkinnslagEllerNull() }.asList,
            )

        private fun Row.tilTiltakshistorikkinnslagEllerNull(): Tiltakshistorikkinnslag? =
            try {
                tilTiltakshistorikkinnslag()
            } catch (e: Exception) {
                log.error(e) {
                    "Kunne ikke lese innslag i tiltakshistorikk, og hopper over det. " +
                        "id=${string("id")}, sakId=${string("sak_id")}, tiltaksdeltakerId=${string("tiltaksdeltaker_id")}"
                }
                null
            }

        private fun Row.tilTiltakshistorikkinnslag() = Tiltakshistorikkinnslag(
            id = TiltakshistorikkId.fromString(string("id")),
            sakId = SakId.fromString(string("sak_id")),
            tiltaksdeltakerId = TiltaksdeltakerId.fromString(string("tiltaksdeltaker_id")),
            eksternDeltakerId = string("ekstern_deltaker_id"),
            innhentet = localDateTime("innhentet"),
            verdi = deserialize<TiltaksdeltakelseNåtilstandDbJson>(string("verdi")).tilDomene(),
            endring = stringOrNull("endring")?.tilTiltaksdeltakerEndring(),
            behandlingId = stringOrNull("behandling_id")?.let { RammebehandlingId.fromString(it) },
            oppgaveId = stringOrNull("oppgave_id")?.let { OppgaveId(it) },
        )
    }
}
