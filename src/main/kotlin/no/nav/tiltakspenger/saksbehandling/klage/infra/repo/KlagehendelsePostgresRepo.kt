package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klagehendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.KlagehendelseId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.NyKlagehendelse
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagehendelseRepo

class KlagehendelsePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : KlagehendelseRepo {

    /**
     * Ignorerer duplikate hendelser basert på ekstern_id, da det kan skje at samme hendelse kommer inn flere ganger fra Kafka.
     */
    override fun lagreNyHendelse(
        nyKlagehendelse: NyKlagehendelse,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    //language=SQL
                    """
                     insert into klagehendelse (
                        id,
                        opprettet,
                        sist_endret,
                        ekstern_id,
                        mottatt_data
                    ) values (
                        :id,
                        :opprettet,
                        :sist_endret,
                        :ekstern_id,
                        to_jsonb(:mottatt_data::jsonb)
                    )
                    on conflict (ekstern_id) do nothing
                    """.trimIndent(),
                    "id" to nyKlagehendelse.klagehendelseId.toString(),
                    "ekstern_id" to nyKlagehendelse.eksternKlagehendelseId,
                    "opprettet" to nyKlagehendelse.opprettet,
                    "sist_endret" to nyKlagehendelse.opprettet,
                    "mottatt_data" to """
                            {
                                "key": "${nyKlagehendelse.key}",
                                "value": "${nyKlagehendelse.value}"
                            }
                    """.trimIndent(),
                ).asUpdate,
            )
        }
    }

    companion object {
        fun hentHendelse(
            klagehendelseId: KlagehendelseId,
            session: Session,
        ): Klagehendelse {
            return session.run(
                sqlQuery(
                    "select * from klagehendelse where id = :id",
                    "id" to klagehendelseId.toString(),
                ).map { row ->
                    Klagehendelse(
                        klagehendelseId = KlagehendelseId.fromString(row.string("id")),
                        eksternKlagehendelseId = row.string("ekstern_id"),
                        opprettet = row.localDateTime("opprettet"),
                        sistEndret = row.localDateTime("sist_endret"),
                    )
                }.asSingle,
            )!!
        }
    }
}
