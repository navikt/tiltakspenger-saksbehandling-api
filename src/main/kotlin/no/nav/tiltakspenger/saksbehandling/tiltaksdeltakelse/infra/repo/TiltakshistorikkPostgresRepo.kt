package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.TiltakshistorikkRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.Tiltakshistorikkinnslag

/** Lagrer i `tiltakshistorikk`. */
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

    override fun erLikSisteVerdi(
        tiltaksdeltakerId: TiltaksdeltakerId,
        verdi: Tiltaksdeltakelse.GirRett,
        sessionContext: SessionContext,
    ): Boolean {
        val sisteVerdi = sessionFactory.withSession(sessionContext) { session ->
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
                ).map { row -> deserialize<TiltaksdeltakelseNåtilstandDbJson>(row.string("verdi")) }.asSingle,
            )
        }
        return sisteVerdi == verdi.tilNåtilstandDbJson()
    }
}
