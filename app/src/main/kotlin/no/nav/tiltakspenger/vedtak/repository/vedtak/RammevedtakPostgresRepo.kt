package no.nav.tiltakspenger.vedtak.repository.vedtak

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.VedtaksType
import no.nav.tiltakspenger.saksbehandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.vedtak.repository.behandling.BehandlingPostgresRepo
import java.time.LocalDateTime

private val SECURELOG = KotlinLogging.logger("tjenestekall")

internal class RammevedtakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : RammevedtakRepo {
    override fun hent(vedtakId: VedtakId): Rammevedtak? {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    "select v.*, s.saksnummer from rammevedtak v join sak s on s.id = v.sak_id where v.id = :id",
                    mapOf(
                        "id" to vedtakId.toString(),
                    ),
                ).map { row ->
                    row.toVedtak(session)
                }.asSingle,
            )
        }
    }

    override fun hentVedtakForIdent(ident: Fnr): List<Rammevedtak> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                               select v.*, 
                                      s.saksnummer
                                 from rammevedtak v
                               join sak s 
                                 on s.id = v.sak_id
                               where s.ident = :ident
                    """.trimIndent(),
                    mapOf(
                        "ident" to ident.verdi,
                    ),
                ).map { row ->
                    row.toVedtak(session)
                }.asList,
            )
        }.also {
            SECURELOG.info { "Hentet ${it.size} vedtak for ident $ident" }
        }
    }

    override fun lagreVedtak(
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
            sessionContext: SessionContext,
        ): List<Rammevedtak> {
            return sessionContext.withSession { session ->
                session.run(
                    queryOf(
                        "select v.*, s.saksnummer from rammevedtak v join sak s on s.id = v.sak_id where v.sak_id = :sakId",
                        mapOf(
                            "sakId" to sakId.toString(),
                        ),
                    ).map { row ->
                        row.toVedtak(session)
                    }.asList,
                )
            }
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
                        fom, 
                        tom, 
                        saksbehandler, 
                        beslutter,
                        opprettet
                    ) values (
                        :id, 
                        :sakId, 
                        :behandlingId, 
                        :vedtakstype, 
                        :vedtaksdato, 
                        :fom, 
                        :tom, 
                        :saksbehandler, 
                        :beslutter,
                        :opprettet
                    )
                    """.trimIndent(),
                    mapOf(
                        "id" to vedtak.id.toString(),
                        "sakId" to vedtak.sakId.toString(),
                        "behandlingId" to vedtak.behandling.id.toString(),
                        "vedtakstype" to vedtak.vedtaksType.toString(),
                        "vedtaksdato" to vedtak.vedtaksdato,
                        "fom" to vedtak.periode.fraOgMed,
                        "tom" to vedtak.periode.tilOgMed,
                        "saksbehandler" to vedtak.saksbehandler,
                        "beslutter" to vedtak.beslutter,
                        "opprettet" to LocalDateTime.now(),
                    ),
                ).asUpdate,
            )
        }

        private fun Row.toVedtak(session: Session): Rammevedtak {
            val id = VedtakId.fromString(string("id"))
            return Rammevedtak(
                id = id,
                sakId = SakId.fromString(string("sak_id")),
                saksnummer = Saksnummer(string("saksnummer")),
                behandling = BehandlingPostgresRepo.hentOrNull(
                    BehandlingId.fromString(string("behandling_id")),
                    session,
                )!!,
                vedtaksdato = localDateTime("vedtaksdato"),
                vedtaksType = VedtaksType.valueOf(string("vedtakstype")),
                periode = Periode(fraOgMed = localDate("fom"), tilOgMed = localDate("tom")),
                saksbehandler = string("saksbehandler"),
                beslutter = string("beslutter"),
            )
        }
    }
}