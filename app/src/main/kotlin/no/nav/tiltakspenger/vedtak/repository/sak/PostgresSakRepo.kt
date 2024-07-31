package no.nav.tiltakspenger.vedtak.repository.sak

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.SakId
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Førstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saker
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.sak.TynnSak
import no.nav.tiltakspenger.saksbehandling.ports.SakRepo
import no.nav.tiltakspenger.vedtak.repository.behandling.BehandlingDAO
import no.nav.tiltakspenger.vedtak.repository.vedtak.VedtakDAO
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalDateTime

private val LOG = KotlinLogging.logger {}
private val SECURELOG = KotlinLogging.logger("tjenestekall")

internal class PostgresSakRepo(
    private val behandlingRepo: BehandlingDAO,
    private val personopplysningerRepo: PostgresPersonopplysningerRepo,
    private val vedtakDAO: VedtakDAO,
    private val sessionFactory: PostgresSessionFactory,
) : SakRepo {
    override fun hentForIdent(fnr: Fnr): Saker {
        return Saker(
            fnr = fnr,
            saker = sessionFactory.withTransaction { txSession ->
                txSession.run(
                    queryOf(
                        sqlHentSakerForIdent,
                        mapOf("ident" to fnr.verdi),
                    ).map { row ->
                        row.toSak(txSession)
                    }.asList,
                )
            },
        )
    }

    override fun hentForSaksnummer(saksnummer: Saksnummer): Sak? {
        return sessionFactory.withTransaction { txSession ->
            txSession.run(
                queryOf(
                    sqlHentSakForSaksnummer,
                    mapOf("saksnummer" to saksnummer.toString()),
                ).map { row ->
                    row.toSak(txSession)
                }.asSingle,
            )
        }
    }

    override fun hentForJournalpostId(journalpostId: String): Sak? {
        return sessionFactory.withTransaction { txSession ->
            txSession.run(
                queryOf(
                    sqlHentForJournalpost,
                    mapOf(
                        "journalpostId" to journalpostId,
                    ),
                ).map { row ->
                    row.toSak(txSession)
                }.asSingle,
            )
        }
    }

    override fun hent(sakId: SakId): Sak? {
        return sessionFactory.withTransaction { txSession ->
            txSession.run(
                queryOf(
                    sqlHent,
                    mapOf("id" to sakId.toString()),
                ).map { row ->
                    row.toSak(txSession)
                }.asSingle,
            )
        }
    }

    override fun hentSakDetaljer(sakId: SakId): TynnSak? {
        return sessionFactory.withTransaction { txSession ->
            txSession.run(
                queryOf(
                    sqlHent,
                    mapOf("id" to sakId.toString()),
                ).map { row ->
                    row.toSakDetaljer()
                }.asSingle,
            )
        }
    }

    override fun lagre(sak: Sak, transactionContext: TransactionContext?): Sak {
        return sessionFactory.withTransaction(transactionContext) { txSession ->
            val sistEndret = hentSistEndret(sak.id, txSession)
            val opprettetSak = if (sistEndret == null) {
                opprettSak(sak, txSession)
            } else {
                oppdaterSak(sistEndret, sak, txSession)
            }
            personopplysningerRepo.lagre(
                sakId = sak.id,
                personopplysninger = sak.personopplysninger,
                txSession = txSession,
            )
            sak.behandlinger.filterIsInstance<Førstegangsbehandling>().forEach {
                behandlingRepo.lagre(it, txSession)
            }
            opprettetSak
        }
    }

    private fun hentSistEndret(sakId: SakId, txSession: TransactionalSession): LocalDateTime? = txSession.run(
        queryOf(
            sqlHentSistEndret,
            mapOf("id" to sakId.toString()),
        ).map { row -> row.localDateTime("sist_endret") }.asSingle,
    )

    private fun oppdaterSak(
        sistEndret: LocalDateTime,
        sak: Sak,
        txSession: TransactionalSession,
    ): Sak {
        SECURELOG.info { "Oppdaterer sak ${sak.id}" }

        val antRaderOppdatert = txSession.run(
            queryOf(
                sqlOppdaterSak,
                mapOf(
                    "id" to sak.id.toString(),
                    "ident" to sak.fnr.verdi,
                    "sistEndretOld" to sistEndret,
                    "sistEndret" to nå(),
                ),
            ).asUpdate,
        )
        if (antRaderOppdatert == 0) {
            throw IllegalStateException("Noen andre har endret denne saken ${sak.id}")
        }
        return sak
    }

    private fun opprettSak(sak: Sak, txSession: TransactionalSession): Sak {
        SECURELOG.info { "Oppretter sak ${sak.id}" }

        val nå = nå()

        txSession.run(
            queryOf(
                sqlOpprettSak,
                mapOf(
                    "id" to sak.id.toString(),
                    "ident" to sak.fnr.verdi,
                    "saksnummer" to sak.saksnummer.verdi,
                    "sistEndret" to nå,
                    "opprettet" to nå,
                ),
            ).asUpdate,
        )
        return sak
    }

    override fun hentNesteSaksnummer(): Saksnummer {
        val iDag = LocalDate.now()
        val saksnummerPrefiks = Saksnummer.genererSaksnummerPrefiks(iDag)
        return sessionFactory.withTransaction { txSession ->
            txSession.run(
                queryOf(
                    sqlHentNesteLøpenummer,
                    mapOf("saksnummerprefiks" to "$saksnummerPrefiks%"),
                ).map { row ->
                    row.string("saksnummer")
                        .let { Saksnummer.nesteSaksnummer(Saksnummer(it)) }
                }.asSingle,
            )
        } ?: Saksnummer.genererSaknummer(dato = iDag)
    }

    private fun Row.toSak(tx: TransactionalSession): Sak {
        val id = SakId.fromDb(string("id"))
        return Sak(
            sakDetaljer = toSakDetaljer(),
            behandlinger = behandlingRepo.hentForSak(id, tx),
            personopplysninger = personopplysningerRepo.hent(id, tx),
            vedtak = vedtakDAO.hentVedtakForSak(id, tx),
        )
    }

    private fun Row.toSakDetaljer(): TynnSak {
        val id = SakId.fromDb(string("id"))
        return TynnSak(
            id = id,
            fnr = Fnr.fromString(string("ident")),
            saksnummer = Saksnummer(verdi = string("saksnummer")),
        )
    }

    @Language("SQL")
    private val sqlOpprettSak = """
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
            :sistEndret,
            :opprettet
        )
    """.trimIndent()

    @Language("SQL")
    private val sqlOppdaterSak =
        """update sak set 
              ident = :ident,
              sist_endret = :sistEndret
           where id = :id
             and sist_endret = :sistEndretOld
        """.trimMargin()

    @Language("SQL")
    private val sqlHent =
        """select * from sak where id = :id""".trimIndent()

    @Language("SQL")
    private val sqlHentForJournalpost = """
        select * 
          from sak 
         where id = (select sakid 
                       from behandling 
                      where id = (select behandling_id
                                    from søknad
                                   where journalpost_id = :journalpostId
                                    )
                       )
    """.trimIndent()

    @Language("SQL")
    private val sqlHentSakerForIdent =
        """select * from sak where ident = :ident""".trimIndent()

    @Language("SQL")
    private val sqlHentSakForSaksnummer =
        """select * from sak where saksnummer = :saksnummer""".trimIndent()

    @Language("SQL")
    private val sqlHentSistEndret =
        """select sist_endret from sak where id = :id""".trimIndent()

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
