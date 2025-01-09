package no.nav.tiltakspenger.vedtak.repository.vedtak

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.distribusjon.domene.DistribusjonId
import no.nav.tiltakspenger.distribusjon.domene.VedtakSomSkalDistribueres
import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Vedtaksliste
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Vedtakstype
import no.nav.tiltakspenger.saksbehandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.vedtak.repository.behandling.BehandlingPostgresRepo
import java.time.LocalDate
import java.time.LocalDateTime

class RammevedtakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : RammevedtakRepo {

    override fun hentForVedtakId(vedtakId: VedtakId): Rammevedtak? {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    "select * from rammevedtak where id = :id",
                    mapOf(
                        "id" to vedtakId.toString(),
                    ),
                ).map { row ->
                    row.toVedtak(session)
                }.asSingle,
            )
        }
    }

    override fun hentForFnr(fnr: Fnr): List<Rammevedtak> {
        return sessionFactory
            .withSession { session ->
                session.run(
                    queryOf(
                        """
                            select v.*
                            from rammevedtak v
                            join sak s
                              on s.id = v.sak_id 
                            where s.ident = :ident
                            order by v.opprettet
                        """.trimIndent(),
                        mapOf(
                            "ident" to fnr.verdi,
                        ),
                    ).map { row ->
                        row.toVedtak(session)
                    }.asList,
                )
            }
    }

    override fun hentRammevedtakSomSkalJournalføres(
        limit: Int,
    ): List<Rammevedtak> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select *
                    from rammevedtak
                    where journalpost_id is null
                    order by opprettet
                    limit :limit
                    """.trimIndent(),
                    mapOf(
                        "limit" to limit,
                    ),
                ).map { row ->
                    row.toVedtak(session)
                }.asList,
            )
        }
    }

    override fun hentRammevedtakSomSkalDistribueres(limit: Int): List<VedtakSomSkalDistribueres> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select v.id,v.journalpost_id
                    from rammevedtak v
                    where v.journalpost_id is not null
                    and v.journalføringstidspunkt is not null
                    and v.distribusjonstidspunkt is null
                    and v.distribusjon_id is null
                    order by v.opprettet
                    limit :limit
                    """.trimIndent(),
                    mapOf(
                        "limit" to limit,
                    ),
                ).map { row ->
                    VedtakSomSkalDistribueres(
                        id = VedtakId.fromString(row.string("id")),
                        journalpostId = JournalpostId(row.string("journalpost_id")),
                    )
                }.asList,
            )
        }
    }

    override fun markerJournalført(id: VedtakId, vedtaksdato: LocalDate, brevJson: String, journalpostId: JournalpostId, tidspunkt: LocalDateTime) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    update rammevedtak set 
                        vedtaksdato = :vedtaksdato,
                        brev_json = to_jsonb(:brev_json),
                        journalpost_id = :journalpost_id,
                        journalføringstidspunkt = :tidspunkt
                    where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to id.toString(),
                        "vedtaksdato" to vedtaksdato,
                        "brev_json" to brevJson,
                        "journalpost_id" to journalpostId.toString(),
                        "tidspunkt" to tidspunkt,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun markerDistribuert(id: VedtakId, distribusjonId: DistribusjonId, tidspunkt: LocalDateTime) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    update rammevedtak
                    set distribusjon_id = :distribusjon_id,
                        distribusjonstidspunkt = :tidspunkt
                    where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to id.toString(),
                        "distribusjon_id" to distribusjonId.toString(),
                        "tidspunkt" to tidspunkt,
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
                    update rammevedtak
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

    override fun hentRammevedtakTilDatadeling(
        limit: Int,
    ): List<Rammevedtak> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select *
                    from rammevedtak
                    where sendt_til_datadeling is null
                    order by opprettet
                    limit $limit
                    """.trimIndent(),
                ).map { row ->
                    row.toVedtak(session)
                }.asList,
            )
        }
    }

    override fun lagre(
        vedtak: Rammevedtak,
        context: TransactionContext?,
    ) {
        sessionFactory.withTransaction(context) { tx ->
            lagreVedtak(vedtak, tx)
        }
    }

    companion object {
        fun hentForSakId(
            sakId: SakId,
            session: Session,
        ): Vedtaksliste {
            return session.run(
                queryOf(
                    "select * from rammevedtak where sak_id = :sak_id order by opprettet",
                    mapOf(
                        "sak_id" to sakId.toString(),
                    ),
                ).map { row ->
                    row.toVedtak(session)
                }.asList,
            ).let { Vedtaksliste(it) }
        }

        internal fun lagreVedtak(
            vedtak: Rammevedtak,
            session: Session,
        ) {
            session.run(
                queryOf(
                    """
                    insert into rammevedtak (
                        id, 
                        sak_id, 
                        behandling_id, 
                        vedtakstype, 
                        vedtaksdato, 
                        fra_og_med, 
                        til_og_med, 
                        saksbehandler, 
                        beslutter,
                        opprettet,
                        brev_json
                    ) values (
                        :id, 
                        :sak_id, 
                        :behandling_id, 
                        :vedtakstype, 
                        :vedtaksdato, 
                        :fra_og_med, 
                        :til_og_med, 
                        :saksbehandler, 
                        :beslutter,
                        :opprettet,
                        :brev_json
                    )
                    """.trimIndent(),
                    mapOf(
                        "id" to vedtak.id.toString(),
                        "sak_id" to vedtak.sakId.toString(),
                        "behandling_id" to vedtak.behandling.id.toString(),
                        "vedtakstype" to vedtak.vedtaksType.toString(),
                        "vedtaksdato" to vedtak.vedtaksdato,
                        "fra_og_med" to vedtak.periode.fraOgMed,
                        "til_og_med" to vedtak.periode.tilOgMed,
                        "saksbehandler" to vedtak.saksbehandlerNavIdent,
                        "beslutter" to vedtak.beslutterNavIdent,
                        "opprettet" to vedtak.opprettet,
                    ),
                ).asUpdate,
            )
        }

        private fun Row.toVedtak(session: Session): Rammevedtak {
            val id = VedtakId.fromString(string("id"))
            return Rammevedtak(
                id = id,
                sakId = SakId.fromString(string("sak_id")),
                behandling =
                BehandlingPostgresRepo.hentOrNull(
                    BehandlingId.fromString(string("behandling_id")),
                    session,
                )!!,
                vedtaksdato = localDateOrNull("vedtaksdato"),
                vedtaksType = Vedtakstype.valueOf(string("vedtakstype")),
                periode = Periode(fraOgMed = localDate("fra_og_med"), tilOgMed = localDate("til_og_med")),
                journalpostId = stringOrNull("journalpost_id")?.let { JournalpostId(it) },
                journalføringstidspunkt = localDateTimeOrNull("journalføringstidspunkt"),
                distribusjonId = stringOrNull("distribusjon_id")?.let { DistribusjonId(it) },
                distribusjonstidspunkt = localDateTimeOrNull("distribusjonstidspunkt"),
                sendtTilDatadeling = localDateTimeOrNull("sendt_til_datadeling"),
                brevJson = stringOrNull("brev_json"),
                opprettet = localDateTime("opprettet"),
            )
        }
    }
}
