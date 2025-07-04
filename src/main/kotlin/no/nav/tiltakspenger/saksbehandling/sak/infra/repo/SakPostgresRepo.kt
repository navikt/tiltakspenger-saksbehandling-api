package no.nav.tiltakspenger.saksbehandling.sak.infra.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.BrukersMeldekortPostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortBehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saker
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.sak.SaksnummerGenerator
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadDAO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingsvedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakPostgresRepo
import org.intellij.lang.annotations.Language
import java.time.Clock
import java.time.LocalDate

class SakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val saksnummerGenerator: SaksnummerGenerator,
    private val clock: Clock,
) : SakRepo {
    val logger = KotlinLogging.logger { }

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
        logger.info { "Oppretter sak ${sak.id}" }
        val nå = nå(clock)
        sessionFactory.withSessionContext { sessionContext ->
            sessionContext.withSession { session ->
                if (sakFinnes(sak.id, session)) return@withSession

                session.run(
                    sqlQuery(
                        """
                        insert into sak (
                            id,
                            fnr,
                            saksnummer,
                            sist_endret,
                            opprettet,
                            skal_sendes_til_meldekort_api
                        ) values (
                            :id,
                            :fnr,
                            :saksnummer,
                            :sist_endret,
                            :opprettet,
                            :skal_sendes_til_meldekort_api
                        )
                        """,
                        "id" to sak.id.toString(),
                        "fnr" to sak.fnr.verdi,
                        "saksnummer" to sak.saksnummer.verdi,
                        "sist_endret" to nå,
                        "opprettet" to nå,
                        "skal_sendes_til_meldekort_api" to false,
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

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, context: TransactionContext?) {
        sessionFactory.withTransaction(context) { tx ->
            tx.run(
                queryOf(
                    """update sak set fnr = :nytt_fnr where fnr = :gammelt_fnr""",
                    mapOf(
                        "nytt_fnr" to nyttFnr.verdi,
                        "gammelt_fnr" to gammeltFnr.verdi,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun hentForSendingTilMeldekortApi(): List<Sak> {
        return sessionFactory.withSessionContext { sessionContext ->
            sessionContext.withSession { session ->
                session.run(
                    sqlQuery(
                        """
                            select * from sak where skal_sendes_til_meldekort_api = true
                        """,
                    ).map { row ->
                        row.toSak(sessionContext)
                    }.asList,
                )
            }
        }
    }

    override fun oppdaterSkalSendesTilMeldekortApi(
        sakId: SakId,
        skalSendesTilMeldekortApi: Boolean,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSessionContext(sessionContext) { sessionContext ->
            sessionContext.withSession { session ->
                session.run(
                    sqlQuery(
                        """
                            update sak
                                set skal_sendes_til_meldekort_api = :skal_sendes_til_meldekort_api 
                            where id = :id
                        """,
                        "skal_sendes_til_meldekort_api" to skalSendesTilMeldekortApi,
                        "id" to sakId.toString(),
                    ).asUpdate,
                )
            }
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

        fun Row.toSak(sessionContext: SessionContext): Sak {
            val id: SakId = SakId.fromString(string("id"))
            return sessionContext.withSession { session ->
                val behandlinger = BehandlingPostgresRepo.hentForSakId(id, session)
                val vedtaksliste: Vedtaksliste = RammevedtakPostgresRepo.hentForSakId(id, session)
                val meldekortBehandlinger =
                    MeldekortBehandlingPostgresRepo.hentForSakId(id, session) ?: MeldekortBehandlinger.empty()
                val meldeperiodekjeder = MeldeperiodePostgresRepo.hentMeldeperiodekjederForSakId(id, session)
                val soknader = SøknadDAO.hentForSakId(id, session)

                Sak(
                    id = id,
                    saksnummer = Saksnummer(verdi = string("saksnummer")),
                    fnr = Fnr.fromString(string("fnr")),
                    behandlinger = behandlinger,
                    vedtaksliste = vedtaksliste,
                    meldekortBehandlinger = meldekortBehandlinger,
                    utbetalinger = UtbetalingsvedtakPostgresRepo.hentForSakId(id, session),
                    meldeperiodeKjeder = meldeperiodekjeder,
                    brukersMeldekort = BrukersMeldekortPostgresRepo.hentForSakId(id, session),
                    soknader = soknader,
                )
            }
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
