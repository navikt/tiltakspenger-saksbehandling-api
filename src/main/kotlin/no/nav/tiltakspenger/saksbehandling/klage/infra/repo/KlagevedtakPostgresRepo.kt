package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtaksliste
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagevedtakRepo
import no.nav.tiltakspenger.saksbehandling.vedtak.VedtakSomSkalDistribueres
import java.time.LocalDate
import java.time.LocalDateTime

class KlagevedtakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : KlagevedtakRepo {

    override fun lagreVedtak(
        klagevedtak: Klagevedtak,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { tx ->
            tx.run(
                queryOf(
                    //language=SQL
                    """
                    insert into klagevedtak (
                        id,
                        sak_id,
                        klagebehandling_id,
                        opprettet,
                        journalpost_id,
                        journalføringstidspunkt,
                        distribusjon_id,
                        distribusjonstidspunkt,
                        sendt_til_datadeling,
                        vedtaksdato,
                        brev_json
                    ) values (
                        :id,
                        :sak_id,
                        :klagebehandling_id,
                        :opprettet,
                        :journalpost_id,
                        :journalforingstidspunkt,
                        :distribusjon_id,
                        :distribusjonstidspunkt,
                        :sendt_til_datadeling,
                        :vedtaksdato,
                        to_jsonb(:brev_json::jsonb)
                    ) on conflict (id) do update set
                        sak_id = :sak_id,
                        klagebehandling_id = :klagebehandling_id,
                        opprettet = :opprettet,
                        journalpost_id = :journalpost_id,
                        journalføringstidspunkt = :journalforingstidspunkt,
                        distribusjon_id = :distribusjon_id,
                        distribusjonstidspunkt = :distribusjonstidspunkt,
                        sendt_til_datadeling = :sendt_til_datadeling,
                        vedtaksdato = :vedtaksdato,
                        brev_json = to_jsonb(:brev_json::jsonb)
                    """.trimIndent(),
                    mapOf(
                        "id" to klagevedtak.id.toString(),
                        "sak_id" to klagevedtak.sakId.toString(),
                        "klagebehandling_id" to klagevedtak.behandling.id.toString(),
                        "opprettet" to klagevedtak.opprettet,
                        "journalpost_id" to klagevedtak.journalpostId?.toString(),
                        "journalforingstidspunkt" to klagevedtak.journalføringstidspunkt,
                        "distribusjon_id" to klagevedtak.distribusjonId?.toString(),
                        "distribusjonstidspunkt" to klagevedtak.distribusjonstidspunkt,
                        "sendt_til_datadeling" to klagevedtak.sendtTilDatadeling,
                        "vedtaksdato" to klagevedtak.vedtaksdato,
                        "brev_json" to klagevedtak.brevJson,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun markerJournalført(
        id: VedtakId,
        vedtaksdato: LocalDate,
        brevJson: String,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    ) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    update klagevedtak set 
                        vedtaksdato = :vedtaksdato,
                        brev_json = to_jsonb(:brev_json::jsonb),
                        journalpost_id = :journalpost_id,
                        journalføringstidspunkt = :journalforingstidspunkt
                    where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to id.toString(),
                        "vedtaksdato" to vedtaksdato,
                        "brev_json" to brevJson,
                        "journalpost_id" to journalpostId.toString(),
                        "journalforingstidspunkt" to tidspunkt,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun markerDistribuert(
        id: VedtakId,
        distribusjonId: DistribusjonId,
        distribusjonstidspunkt: LocalDateTime,
    ) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    update klagevedtak
                    set distribusjon_id = :distribusjon_id,
                        distribusjonstidspunkt = :distribusjonstidspunkt
                    where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to id.toString(),
                        "distribusjon_id" to distribusjonId.toString(),
                        "distribusjonstidspunkt" to distribusjonstidspunkt,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun markerSendtTilDatadeling(id: VedtakId, tidspunkt: LocalDateTime) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    update klagevedtak
                    set sendt_til_datadeling = :tidspunkt
                    where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to id.toString(),
                        "tidspunkt" to tidspunkt,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun hentKlagevedtakSomSkalJournalføres(limit: Int): List<Klagevedtak> {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    select
                      k.*,
                      s.fnr,
                      s.saksnummer
                    from klagevedtak k
                    join sak s on s.id = k.sak_id
                    where k.journalpost_id is null
                    order by k.opprettet
                    limit :limit
                    """,
                    "limit" to limit,
                ).map { fromRow(it, session) }.asList,
            )
        }
    }

    override fun hentKlagevedtakSomSkalDistribueres(limit: Int): List<VedtakSomSkalDistribueres> {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        select v.id,v.journalpost_id
                        from klagevedtak v
                        where v.journalpost_id is not null
                        and v.journalføringstidspunkt is not null
                        and v.distribusjonstidspunkt is null
                        and v.distribusjon_id is null
                        order by v.opprettet
                        limit :limit
                    """,
                    "limit" to limit,
                ).map { row ->
                    VedtakSomSkalDistribueres(
                        id = VedtakId.fromString(row.string("id")),
                        journalpostId = JournalpostId(row.string("journalpost_id")),
                    )
                }.asList,
            )
        }
    }

    companion object {
        fun hentForSakId(sakId: SakId, session: Session): Klagevedtaksliste {
            return session.run(
                sqlQuery(
                    """
                    select
                      k.*,
                      s.fnr,
                      s.saksnummer
                    from klagevedtak k
                    join sak s on s.id = k.sak_id
                    where s.id = :sakId
                    order by k.opprettet
                    """,
                    "sakId" to sakId.toString(),
                ).map { fromRow(it, session) }.asList,
            ).let { Klagevedtaksliste(it) }
        }

        private fun fromRow(
            row: Row,
            session: Session,
        ): Klagevedtak {
            val klagebehandlingId = KlagebehandlingId.fromString(row.string("klagebehandling_id"))
            val klagebehandling = KlagebehandlingPostgresRepo.hentOrNull(klagebehandlingId, session)!!
            return Klagevedtak(
                id = VedtakId.fromString(row.string("id")),
                opprettet = row.localDateTime("opprettet"),
                journalpostId = row.stringOrNull("journalpost_id")?.let { JournalpostId(it) },
                journalføringstidspunkt = row.localDateTimeOrNull("journalføringstidspunkt"),
                behandling = klagebehandling,
                distribusjonId = row.stringOrNull("distribusjon_id")?.let { DistribusjonId(it) },
                distribusjonstidspunkt = row.localDateTimeOrNull("distribusjonstidspunkt"),
                vedtaksdato = row.localDateOrNull("vedtaksdato"),
                sendtTilDatadeling = row.localDateTimeOrNull("sendt_til_datadeling"),
                brevJson = row.stringOrNull("brev_json"),
            )
        }
    }
}
