package no.nav.tiltakspenger.vedtak.repository.person

import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.ports.PersonRepo

/*
    Dette repoet brukes av auditloggeren
 */
class PersonPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : PersonRepo {

    override fun hentFnrForSakId(sakId: SakId): Fnr? =
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """select ident from sak where id = :id""",
                    mapOf(
                        "id" to sakId.toString(),
                    ),
                ).map { row ->
                    Fnr.fromString(row.string("ident"))
                }.asSingle,
            )
        }

    override fun hentFnrForSaksnummer(saksnummer: Saksnummer): Fnr? =
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """select ident from sak where saksnummer = :saksnummer""",
                    mapOf(
                        "saksnummer" to saksnummer.verdi,
                    ),
                ).map { row ->
                    Fnr.fromString(row.string("ident"))
                }.asSingle,
            )
        }

    override fun hentFnrForBehandlingId(behandlingId: BehandlingId): Fnr? =
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """select s.ident from behandling b join sak s on s.id = b.sak_id where b.id = :behandlingId""",
                    mapOf(
                        "behandlingId" to behandlingId.toString(),
                    ),
                ).map { row ->
                    Fnr.fromString(row.string("ident"))
                }.asSingle,
            )
        }

    override fun hentFnrForSøknadId(søknadId: SøknadId): Fnr? =
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """select ident from søknad where id = :id""",
                    mapOf(
                        "id" to søknadId.toString(),
                    ),
                ).map { row ->
                    Fnr.fromString(row.string("ident"))
                }.asSingle,
            )
        }

    override fun hentFnrForMeldekortId(meldekortId: MeldekortId): Fnr? =
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                        select sak.ident from meldekortbehandling m
                        join public.sak sak on sak.id = m.sak_id
                        where m.id = :meldekort_id
                    """.trimMargin(),
                    mapOf(
                        "meldekort_id" to meldekortId.toString(),
                    ),
                ).map { row ->
                    Fnr.fromString(row.string("ident"))
                }.asSingle,
            )
        }
}
