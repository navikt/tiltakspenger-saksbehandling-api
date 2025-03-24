package no.nav.tiltakspenger.saksbehandling.repository.sak

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.felles.nå
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.repository.behandling.BehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.BrukersMeldekortPostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.MeldekortBehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.søknad.SøknadDAO
import no.nav.tiltakspenger.saksbehandling.repository.utbetaling.UtbetalingsvedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.vedtak.RammevedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saker
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.SaksnummerGenerator
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vedtak.Vedtaksliste
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.SakRepo
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

    override fun oppdaterFørsteOgSisteDagSomGirRett(
        sakId: SakId,
        førsteDagSomGirRett: LocalDate?,
        sisteDagSomGirRett: LocalDate?,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSessionContext(sessionContext) { sc ->
            sc.withSession { session ->
                session.run(
                    queryOf(
                        """
                        update sak set første_dag_som_gir_rett = :forste_dag_som_gir_rett, siste_dag_som_gir_rett = :siste_dag_som_gir_rett where id = :sak_id
                        """.trimIndent(),
                        mapOf(
                            "forste_dag_som_gir_rett" to førsteDagSomGirRett,
                            "siste_dag_som_gir_rett" to sisteDagSomGirRett,
                            "sak_id" to sakId.toString(),
                        ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun hentSakerSomMåGenerereMeldeperioderFra(ikkeGenererEtter: LocalDate, limit: Int): List<SakId> {
        return sessionFactory.withSessionContext { sessionContext ->
            sessionContext.withSession { session ->
                session.run(
                    queryOf(
                        // language=SQL
                        """
                            select s.id, s.siste_dag_som_gir_rett, max(m.til_og_med) as til_og_med
                            from sak s
                            left join meldeperiode m on s.id = m.sak_id
                            group by s.id
                            having (
                                -- Case 1: Has meldeperioder but needs more
                                (
                                    max(m.til_og_med) is not null 
                                    and max(m.til_og_med) < s.siste_dag_som_gir_rett
                                    and max(m.til_og_med) < :ikkeGenererEtter
                                )
                                or
                                -- Case 2: Has no meldeperioder (max will be null)
                                (
                                    max(m.til_og_med) is null
                                    and s.første_dag_som_gir_rett <= :ikkeGenererEtter
                                )
                            )
                            limit $limit;
                        """.trimIndent(),
                        mapOf("ikkeGenererEtter" to ikkeGenererEtter),
                    ).map {
                        SakId.fromString(it.string("id"))
                    }.asList,
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
                val meldeperioder = MeldeperiodePostgresRepo.hentForSakId(id, session)
                val soknader = SøknadDAO.hentForSakId(id, session)
                Sak(
                    id = id,
                    saksnummer = Saksnummer(verdi = string("saksnummer")),
                    fnr = Fnr.fromString(string("fnr")),
                    behandlinger = behandlinger,
                    vedtaksliste = vedtaksliste,
                    meldekortBehandlinger = meldekortBehandlinger,
                    utbetalinger = UtbetalingsvedtakPostgresRepo.hentForSakId(id, session),
                    meldeperiodeKjeder = meldeperioder,
                    brukersMeldekort = BrukersMeldekortPostgresRepo.hentForSakId(id, session),
                    soknader = soknader,
                ).also { sak ->
                    localDateOrNull("første_dag_som_gir_rett").also {
                        require(sak.førsteDagSomGirRett == it) {
                            "Vedtakslisten vår er master på første dag som gir rett (${sak.førsteDagSomGirRett}). Kolonnen sak.første_dag_som_gir_rett er ikke oppdatert ($it). For sak-id: $id, saksnummer: ${sak.saksnummer}"
                        }
                    }
                    localDateOrNull("siste_dag_som_gir_rett").also {
                        require(sak.sisteDagSomGirRett == it) {
                            "Vedtakslisten vår er master på siste dag som gir rett (${sak.sisteDagSomGirRett}). Kolonnen sak.siste_dag_som_gir_rett er ikke oppdatert ($it). For sak-id: $id, saksnummer: ${sak.saksnummer}"
                        }
                    }
                }
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
