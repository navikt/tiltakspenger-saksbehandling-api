package no.nav.tiltakspenger.saksbehandling.dev.tiltaksdeltakelse.infra.repo

import kotliquery.Session
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakelseDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.toDbJson
import java.time.LocalDateTime

/**
 * Tiltaksdeltakelser satt via dev-endepunktet ved lokal kjøring.
 * Tabellen finnes kun lokalt, se [no.nav.tiltakspenger.saksbehandling.dev.infra.repo.lokalFlywayMigrate].
 */
object LokalTiltaksdeltakelsePostgresRepo {
    fun lagre(
        fnr: Fnr,
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
        sistEndret: LocalDateTime,
        session: Session,
    ) {
        session.run(
            sqlQuery(
                """
                    insert into lokal.tiltaksdeltakelse (ekstern_id, fnr, deltakelse, sist_endret)
                    values (:ekstern_id, :fnr, :deltakelse::jsonb, :sist_endret)
                    on conflict (ekstern_id) do update set
                        fnr = excluded.fnr,
                        deltakelse = excluded.deltakelse,
                        sist_endret = excluded.sist_endret
                """.trimIndent(),
                "ekstern_id" to tiltaksdeltakelse.eksternDeltakelseId,
                "fnr" to fnr.verdi,
                "deltakelse" to serialize(tiltaksdeltakelse.toDbJson()),
                "sist_endret" to sistEndret,
            ).asUpdate,
        )
    }

    fun hentForFnr(fnr: Fnr, session: Session): List<TiltaksdeltakelseIntern> =
        session.run(
            sqlQuery(
                "select deltakelse from lokal.tiltaksdeltakelse where fnr = :fnr",
                "fnr" to fnr.verdi,
            ).map { row -> deserialize<TiltaksdeltakelseDb>(row.string("deltakelse")).toDomain() }.asList,
        )
}
