package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import kotliquery.Row
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
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
                        sak_id,
                        klagebehandling_id,
                        mottatt_data
                    ) values (
                        :id,
                        :opprettet,
                        :sist_endret,
                        :ekstern_id,
                        :sak_id,
                        :klagebehandling_id,
                        to_jsonb(:mottatt_data::jsonb)
                    )
                    on conflict (ekstern_id) do nothing
                    """.trimIndent(),
                    "id" to nyKlagehendelse.klagehendelseId.toString(),
                    "ekstern_id" to nyKlagehendelse.eksternKlagehendelseId,
                    "opprettet" to nyKlagehendelse.opprettet,
                    "sist_endret" to nyKlagehendelse.sistEndret,
                    "sak_id" to nyKlagehendelse.sakId?.toString(),
                    "klagebehandling_id" to nyKlagehendelse.klagebehandlingId?.toString(),
                    "mottatt_data" to """
                            {
                                "key": "${nyKlagehendelse.key}",
                                "value": ${nyKlagehendelse.value}
                            }
                    """.trimIndent(),
                ).asUpdate,
            )
        }
    }

    override fun knyttHendelseTilSakOgKlagebehandling(
        nyKlagehendelse: NyKlagehendelse,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    //language=SQL
                    """
                     update klagehendelse
                     set sak_id = :sak_id, klagebehandling_id = :klagebehandling_id, sist_endret = :sist_endret
                     where id = :id
                    """.trimIndent(),
                    "id" to nyKlagehendelse.klagehendelseId.toString(),
                    "sak_id" to nyKlagehendelse.sakId?.toString(),
                    "klagebehandling_id" to nyKlagehendelse.klagebehandlingId?.toString(),
                    "sist_endret" to nyKlagehendelse.sistEndret,
                ).asUpdate,
            )
        }
    }

    override fun hentNyHendelse(
        klagehendelseId: KlagehendelseId,
        sessionContext: SessionContext?,
    ): NyKlagehendelse? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    "select id,ekstern_id,opprettet,sist_endret,mottatt_data,sak_id,klagebehandling_id from klagehendelse where id = :id",
                    "id" to klagehendelseId.toString(),
                ).map { row ->
                    fromRow(row)
                }.asSingle,
            )
        }
    }

    override fun hentUbehandledeHendelser(limit: Int): List<NyKlagehendelse> {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery("select id,ekstern_id,opprettet,sist_endret,mottatt_data,sak_id,klagebehandling_id from klagehendelse where sak_id is null limit $limit").map {
                    fromRow(it)
                }.asList,
            )
        }
    }

    private fun fromRow(row: Row): NyKlagehendelse {
        val keyValueJson = objectMapper.readTree(row.string("mottatt_data"))
        return NyKlagehendelse(
            klagehendelseId = KlagehendelseId.fromString(row.string("id")),
            eksternKlagehendelseId = row.string("ekstern_id"),
            opprettet = row.localDateTime("opprettet"),
            sistEndret = row.localDateTime("sist_endret"),
            key = keyValueJson.get("key")!!.asString(),
            value = keyValueJson.get("value")!!.toString(),
            sakId = row.stringOrNull("sak_id")?.let { SakId.fromString(it) },
            klagebehandlingId = row.stringOrNull("klagebehandling_id")?.let { KlagebehandlingId.fromString(it) },
        )
    }
}
