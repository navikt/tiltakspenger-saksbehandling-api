package no.nav.tiltakspenger.vedtak.repository.sak

import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saker
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.sak.SaksnummerGenerator
import no.nav.tiltakspenger.saksbehandling.domene.sak.TynnSak
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Vedtaksliste
import no.nav.tiltakspenger.saksbehandling.ports.SakRepo
import no.nav.tiltakspenger.vedtak.repository.behandling.BehandlingPostgresRepo
import no.nav.tiltakspenger.vedtak.repository.meldekort.MeldekortPostgresRepo
import no.nav.tiltakspenger.vedtak.repository.meldekort.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.vedtak.repository.utbetaling.UtbetalingsvedtakPostgresRepo
import no.nav.tiltakspenger.vedtak.repository.vedtak.RammevedtakPostgresRepo
import org.intellij.lang.annotations.Language
import java.time.LocalDate

internal class SakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val saksnummerGenerator: SaksnummerGenerator,
) : SakRepo {
    override fun hentForFnr(fnr: Fnr): Saker {
        val saker =
            sessionFactory.withSessionContext { sessionContext ->
                sessionContext.withSession { session ->
                    session.run(
                        queryOf(
                            sqlHentSakerForIdent,
                            mapOf("ident" to fnr.verdi),
                        ).map { row ->
                            row.toSak(sessionContext)
                        }.asList,
                    )
                }
            }
        return Saker(
            fnr = fnr,
            saker = saker,
        )
    }

    override fun hentForSaksnummer(saksnummer: Saksnummer): Sak? =
        sessionFactory.withSessionContext { sessionContext ->
            sessionContext.withSession { session ->
                session.run(
                    queryOf(
                        sqlHentSakForSaksnummer,
                        mapOf("saksnummer" to saksnummer.toString()),
                    ).map { row ->
                        row.toSak(sessionContext)
                    }.asSingle,
                )
            }
        }

    override fun hentForSakId(sakId: SakId): Sak? =
        sessionFactory.withSessionContext { sessionContext ->
            sessionContext.withSession { session ->
                session.run(
                    queryOf(
                        sqlHent,
                        mapOf("id" to sakId.toString()),
                    ).map { row ->
                        row.toSak(sessionContext)
                    }.asSingle,
                )
            }
        }

    override fun hentDetaljerForSakId(sakId: SakId): TynnSak? =
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    sqlHent,
                    mapOf("id" to sakId.toString()),
                ).map { row ->
                    row.toSakDetaljer()
                }.asSingle,
            )
        }

    override fun hentFnrForSaksnummer(
        saksnummer: Saksnummer,
        sessionContext: SessionContext?,
    ) =
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                        select ident from sak where saksnummer = :saksnummer
                    """.trimIndent(),
                    mapOf("saksnummer" to saksnummer.verdi),
                ).map { row ->
                    row.toFnr()
                }.asSingle,
            )
        }

    override fun hentFnrForSakId(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): Fnr? =
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                queryOf(
                    "select ident as fnr from sak  where sak.id = :sak_id",
                    mapOf("sak_id" to sakId.toString()),
                ).map { row ->
                    Fnr.fromString(row.string("fnr"))
                }.asSingle,
            )
        }

    override fun opprettSakOgFørstegangsbehandling(
        sak: Sak,
        transactionContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(transactionContext) { txSession ->
            sikkerlogg.info { "Oppretter sak ${sak.id}" }

            val nå = nå()
            txSession.run(
                queryOf(
                    """
                    insert into sak (
                        id,
                        ident,
                        saksnummer,
                        sist_endret,
                        opprettet
                    ) values (
                        :id,
                        :ident,
                        :saksnummer,
                        :sist_endret,
                        :opprettet
                    )
                    """.trimIndent(),
                    mapOf(
                        "id" to sak.id.toString(),
                        "ident" to sak.fnr.verdi,
                        "saksnummer" to sak.saksnummer.verdi,
                        "sist_endret" to nå,
                        "opprettet" to nå,
                    ),
                ).asUpdate,
            )
            BehandlingPostgresRepo.lagre(sak.førstegangsbehandling, txSession)
        }
    }

    override fun hentNesteSaksnummer(): Saksnummer {
        val iDag = LocalDate.now()
        val saksnummerPrefiks = Saksnummer.genererSaksnummerPrefiks(iDag)
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    sqlHentNesteLøpenummer,
                    mapOf("saksnummerprefiks" to "$saksnummerPrefiks%"),
                ).map { row ->
                    row
                        .string("saksnummer")
                        .let { Saksnummer(it).nesteSaksnummer() }
                }.asSingle,
            )
        } ?: saksnummerGenerator.generer(dato = iDag)
    }

    override fun hentForSøknadId(søknadId: SøknadId): Sak? =
        sessionFactory.withSessionContext { sessionContext ->
            sessionContext.withSession { session ->
                session.run(
                    queryOf(
                        """
                        select s.* from søknad sø
                        join behandling b on b.id = sø.behandling_id
                        join sak s on s.id = b.sak_id
                        where sø.id = :soknad_id
                        """.trimIndent(),
                        mapOf("soknad_id" to søknadId.toString()),
                    ).map { row ->
                        row.toSak(sessionContext)
                    }.asSingle,
                )
            }
        }

    companion object {

        private fun Row.toSak(sessionContext: SessionContext): Sak {
            val id = SakId.fromString(string("id"))
            return sessionContext.withSession { session ->
                val behandlinger = BehandlingPostgresRepo.hentForSakId(id, session)
                val vedtaksliste: Vedtaksliste = RammevedtakPostgresRepo.hentForSakId(id, session)
                val meldekortBehandlinger = vedtaksliste.førstegangsvedtak?.let {
                    MeldekortPostgresRepo.hentForSakId(id, session)
                } ?: MeldekortBehandlinger.empty(behandlinger.first().tiltakstype)
                val meldeperioder = MeldeperiodePostgresRepo.hentForSakId(id, session)

                Sak(
                    id = SakId.fromString(string("id")),
                    saksnummer = Saksnummer(verdi = string("saksnummer")),
                    fnr = Fnr.fromString(string("ident")),
                    behandlinger = behandlinger,
                    vedtaksliste = vedtaksliste,
                    meldekortBehandlinger = meldekortBehandlinger,
                    utbetalinger = UtbetalingsvedtakPostgresRepo.hentForSakId(id, session),
                    meldeperiodeKjeder = meldeperioder,
                )
            }
        }

        private fun Row.toSakDetaljer(): TynnSak {
            val id = SakId.fromString(string("id"))
            return TynnSak(
                id = id,
                fnr = Fnr.fromString(string("ident")),
                saksnummer = Saksnummer(verdi = string("saksnummer")),
            )
        }

        private fun Row.toFnr(): Fnr {
            return Fnr.fromString(string("ident"))
        }

        @Language("SQL")
        private val sqlHent =
            """select * from sak where id = :id""".trimIndent()

        @Language("SQL")
        private val sqlHentSakerForIdent =
            """select * from sak where ident = :ident""".trimIndent()

        @Language("SQL")
        private val sqlHentSakForSaksnummer =
            """select * from sak where saksnummer = :saksnummer""".trimIndent()

        @Language("SQL")
        private val sqlHentNesteLøpenummer =
            """
            SELECT saksnummer 
            FROM sak 
            WHERE saksnummer LIKE :saksnummerprefiks 
            ORDER BY saksnummer DESC 
            LIMIT 1
            """.trimIndent()
    }
}
