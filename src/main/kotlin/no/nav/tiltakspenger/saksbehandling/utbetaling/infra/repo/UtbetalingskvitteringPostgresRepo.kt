package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import kotliquery.Row
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.felles.Kafkametadata
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toForsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.RåUtbetalingskvittering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingskobling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingskvitteringId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingskvitteringRepo
import java.time.LocalDateTime

class UtbetalingskvitteringPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : UtbetalingskvitteringRepo {
    override fun lagreNy(
        key: String?,
        value: String?,
        kafkametadata: Kafkametadata,
        mottatt: LocalDateTime,
    ): Boolean =
        sessionFactory.withSession { session ->
            val id = UtbetalingskvitteringId.random()
            session.run(
                sqlQuery(
                    """
                    insert into utbetalingskvittering (id, mottatt, key, value, kafka)
                    values (:id, :mottatt, :key, :value, :kafka::jsonb)
                    on conflict (
                        (kafka ->> 'topic'),
                        ((kafka ->> 'partisjon')::int),
                        ((kafka ->> 'offset')::bigint)
                    ) do nothing
                    """,
                    "id" to id.toString(),
                    "mottatt" to mottatt,
                    "key" to key,
                    "value" to value,
                    "kafka" to serialize(
                        mapOf(
                            "topic" to kafkametadata.topic,
                            "partisjon" to kafkametadata.partisjon,
                            "offset" to kafkametadata.offset,
                            "tidspunkt" to kafkametadata.tidspunkt.toString(),
                            "tidspunktstype" to kafkametadata.tidspunktstype,
                            "headere" to kafkametadata.headere,
                        ),
                    ),
                ).asUpdate,
            ) == 1
        }

    override fun hentUbehandledeIder(nå: LocalDateTime, limit: Int): List<UtbetalingskvitteringId> =
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    select q.id
                    from utbetalingskvittering q
                    where q.behandlet is null
                      and (
                        q.forsokshistorikk is null
                        or (q.forsokshistorikk ->> 'nesteForsøk')::timestamp <= :na
                      )
                      and (
                        q.key is null
                        or not exists (
                            select 1
                            from utbetalingskvittering eldre
                            where q.key is not null
                              and eldre.key is not null
                              and eldre.key = q.key
                              and (eldre.kafka ->> 'topic') = (q.kafka ->> 'topic')
                              and (eldre.kafka ->> 'partisjon')::int = (q.kafka ->> 'partisjon')::int
                              and (eldre.kafka ->> 'offset')::bigint < (q.kafka ->> 'offset')::bigint
                              and eldre.behandlet is null
                        )
                      )
                    order by
                        q.kafka ->> 'topic',
                        (q.kafka ->> 'partisjon')::int,
                        (q.kafka ->> 'offset')::bigint
                    limit :limit
                    """,
                    "na" to nå,
                    "limit" to limit,
                ).map { UtbetalingskvitteringId.fromString(it.string("id")) }.asList,
            )
        }

    override fun hent(id: UtbetalingskvitteringId, context: TransactionContext): RåUtbetalingskvittering =
        sessionFactory.withSession(context) { session ->
            session.run(
                sqlQuery(
                    """
                    select id, key, value, mottatt, behandlet, forsokshistorikk, sak_id, utbetaling_id
                    from utbetalingskvittering
                    where id = :id
                    for update
                    """,
                    "id" to id.toString(),
                ).map { it.tilUtbetalingskvittering() }.asSingle,
            ) ?: error("Fant ikke utbetalingskvittering med id $id")
        }

    override fun markerBehandlet(
        id: UtbetalingskvitteringId,
        behandlet: LocalDateTime,
        kobling: Utbetalingskobling?,
        context: TransactionContext,
    ) {
        sessionFactory.withSession(context) { session ->
            session.run(
                sqlQuery(
                    """
                    update utbetalingskvittering
                    set behandlet = :behandlet,
                        sak_id = coalesce(:sak_id, sak_id),
                        utbetaling_id = coalesce(:utbetaling_id, utbetaling_id),
                        behandlet_feil = null,
                        forbigaaende = null
                    where id = :id
                    """,
                    "id" to id.toString(),
                    "behandlet" to behandlet,
                    "sak_id" to kobling?.let { it.sakId.toString() },
                    "utbetaling_id" to kobling?.let { it.utbetalingId.toString() },
                ).asUpdate,
            )
        }
    }

    override fun lagreFeil(
        id: UtbetalingskvitteringId,
        feil: String,
        forbigående: Boolean,
        forsøkshistorikk: Forsøkshistorikk,
        kobling: Utbetalingskobling?,
    ) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    update utbetalingskvittering
                    set sak_id = coalesce(:sak_id, sak_id),
                        utbetaling_id = coalesce(:utbetaling_id, utbetaling_id),
                        behandlet_feil = :feil,
                        forbigaaende = :forbigaaende,
                        forsokshistorikk = :forsokshistorikk::jsonb
                    where id = :id
                    """,
                    "id" to id.toString(),
                    "feil" to feil,
                    "forbigaaende" to forbigående,
                    "forsokshistorikk" to forsøkshistorikk.toDbJson(),
                    "sak_id" to kobling?.let { it.sakId.toString() },
                    "utbetaling_id" to kobling?.let { it.utbetalingId.toString() },
                ).asUpdate,
            )
        }
    }

    override fun antallPermanenteFeil(): Long =
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    select count(*) as antall
                    from utbetalingskvittering
                    where behandlet is null
                      and forbigaaende = false
                    """,
                ).map { it.long("antall") }.asList,
            ).single()
        }

    override fun antallForbigåendeFeil(eldreEnn: LocalDateTime): Long =
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    select count(*) as antall
                    from utbetalingskvittering
                    where behandlet is null
                      and forbigaaende = true
                      and mottatt <= :eldre_enn
                    """,
                    "eldre_enn" to eldreEnn,
                ).map { it.long("antall") }.asList,
            ).single()
        }

    private fun Row.tilUtbetalingskvittering() = RåUtbetalingskvittering(
        id = UtbetalingskvitteringId.fromString(string("id")),
        key = stringOrNull("key"),
        value = stringOrNull("value"),
        mottatt = localDateTime("mottatt"),
        behandlet = localDateTimeOrNull("behandlet"),
        forsøkshistorikk = stringOrNull("forsokshistorikk")?.toForsøkshistorikk(),
        sakId = stringOrNull("sak_id")?.let { SakId.fromString(it) },
        utbetalingId = stringOrNull("utbetaling_id")?.let { UtbetalingId.fromString(it) },
    )
}
