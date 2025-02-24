package no.nav.tiltakspenger.vedtak.repository.sak

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
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
import no.nav.tiltakspenger.vedtak.repository.meldekort.BrukersMeldekortPostgresRepo
import no.nav.tiltakspenger.vedtak.repository.meldekort.MeldekortBehandlingPostgresRepo
import no.nav.tiltakspenger.vedtak.repository.meldekort.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.vedtak.repository.søknad.SøknadDAO
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
                            sqlHentSakerForFnr,
                            mapOf("fnr" to fnr.verdi),
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

    override fun hentFnrForSaksnummer(
        saksnummer: Saksnummer,
        sessionContext: SessionContext?,
    ) =
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                        select fnr from sak where saksnummer = :saksnummer
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
                    "select fnr from sak  where sak.id = :sak_id",
                    mapOf("sak_id" to sakId.toString()),
                ).map { row ->
                    Fnr.fromString(row.string("fnr"))
                }.asSingle,
            )
        }

    override fun opprettSak(sak: Sak) {
        sikkerlogg.info { "Oppretter sak ${sak.id}" }
        val nå = nå()
        sessionFactory.withSessionContext { sessionContext ->
            sessionContext.withSession { session ->
                if (sakFinnes(sak.id, session)) return@withSession

                session.run(
                    queryOf(
                        """
                    insert into sak (
                        id,
                        fnr,
                        saksnummer,
                        sist_endret,
                        opprettet
                    ) values (
                        :id,
                        :fnr,
                        :saksnummer,
                        :sist_endret,
                        :opprettet
                    )
                        """.trimIndent(),
                        mapOf(
                            "id" to sak.id.toString(),
                            "fnr" to sak.fnr.verdi,
                            "saksnummer" to sak.saksnummer.verdi,
                            "sist_endret" to nå,
                            "opprettet" to nå,
                        ),
                    ).asUpdate,
                )
            }
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

    private fun sakFinnes(
        sakId: SakId,
        session: Session,
    ): Boolean =
        session.run(
            queryOf(sqlFinnes, sakId.toString()).map { row -> row.boolean("exists") }.asSingle,
        ) ?: throw RuntimeException("Kunne ikke avgjøre om sak finnes")

    companion object {

        private fun Row.toSak(sessionContext: SessionContext): Sak {
            val id = SakId.fromString(string("id"))
            return sessionContext.withSession { session ->
                val behandlinger = BehandlingPostgresRepo.hentForSakId(id, session)
                val vedtaksliste: Vedtaksliste = RammevedtakPostgresRepo.hentForSakId(id, session)
                val meldekortBehandlinger = vedtaksliste.førstegangsvedtak?.let {
                    MeldekortBehandlingPostgresRepo.hentForSakId(id, session)
                } ?: MeldekortBehandlinger.empty()
                val meldeperioder = MeldeperiodePostgresRepo.hentForSakId(id, session)
                val soknader = SøknadDAO.hentForSakId(id, session)
                Sak(
                    id = SakId.fromString(string("id")),
                    saksnummer = Saksnummer(verdi = string("saksnummer")),
                    fnr = Fnr.fromString(string("fnr")),
                    behandlinger = behandlinger,
                    vedtaksliste = vedtaksliste,
                    meldekortBehandlinger = meldekortBehandlinger,
                    utbetalinger = UtbetalingsvedtakPostgresRepo.hentForSakId(id, session),
                    meldeperiodeKjeder = meldeperioder,
                    brukersMeldekort = BrukersMeldekortPostgresRepo.hentForSakId(id, session),
                    soknader = soknader,
                )
            }
        }

        private fun Row.toSakDetaljer(): TynnSak {
            val id = SakId.fromString(string("id"))
            return TynnSak(
                id = id,
                fnr = Fnr.fromString(string("fnr")),
                saksnummer = Saksnummer(verdi = string("saksnummer")),
            )
        }

        private fun Row.toFnr(): Fnr {
            return Fnr.fromString(string("fnr"))
        }

        @Language("SQL")
        private val sqlHent =
            """select * from sak where id = :id""".trimIndent()

        @Language("SQL")
        private val sqlHentSakerForFnr =
            """select * from sak where fnr = :fnr""".trimIndent()

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

        @Language("SQL")
        private val sqlFinnes = "select exists(select 1 from sak where id = ?)"
    }
}
