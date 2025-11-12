package no.nav.tiltakspenger.saksbehandling.statistikk.meldekort

import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.infra.repo.toPGObject
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime

class StatistikkMeldekortPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : StatistikkMeldekortRepo {
    override fun lagre(
        dto: StatistikkMeldekortDTO,
        context: TransactionContext?,
    ) {
        sessionFactory.withTransaction(context) { tx ->
            tx.run(
                queryOf(
                    lagreMeldekortSql,
                    mapOf(
                        "meldeperiode_kjede_id" to dto.meldeperiodeKjedeId,
                        "sak_id" to dto.sakId,
                        "meldekortbehandling_id" to dto.meldekortBehandlingId,
                        "bruker_id" to dto.brukerId,
                        "saksnummer" to dto.saksnummer,
                        "vedtatt_tidspunkt" to dto.vedtattTidspunkt,
                        "behandlet_automatisk" to dto.behandletAutomatisk,
                        "fra_og_med" to dto.fraOgMed,
                        "til_og_med" to dto.tilOgMed,
                        "meldekortdager" to toPGObject(dto.meldekortdager),
                        "opprettet" to dto.opprettet,
                        "sist_endret" to dto.sistEndret,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun oppdaterFnr(
        gammeltFnr: Fnr,
        nyttFnr: Fnr,
        context: TransactionContext?,
    ) {
        sessionFactory.withTransaction(context) { tx ->
            tx.run(
                queryOf(
                    """
                        update statistikk_meldekort set bruker_id = :nytt_fnr, sist_endret = :sist_endret where bruker_id = :gammelt_fnr
                    """.trimIndent(),
                    mapOf(
                        "nytt_fnr" to nyttFnr.verdi,
                        "gammelt_fnr" to gammeltFnr.verdi,
                        "sist_endret" to LocalDateTime.now(),
                    ),
                ).asUpdate,
            )
        }
    }

    @Language("SQL")
    private val lagreMeldekortSql =
        """
        insert into statistikk_meldekort (
        meldeperiode_kjede_id,
        sak_id,
        meldekortbehandling_id,
        bruker_id,
        saksnummer,
        vedtatt_tidspunkt,
        behandlet_automatisk,
        fra_og_med,
        til_og_med,
        meldekortdager,
        opprettet,
        sist_endret
        ) values (
        :meldeperiode_kjede_id,
        :sak_id,
        :meldekortbehandling_id,
        :bruker_id,
        :saksnummer,
        :vedtatt_tidspunkt,
        :behandlet_automatisk,
        :fra_og_med,
        :til_og_med,
        :meldekortdager,
        :opprettet,
        :sist_endret
        ) on conflict (meldekortbehandling_id) do update set
        meldeperiode_kjede_id = :meldeperiode_kjede_id,
        sak_id = :sak_id,
        vedtatt_tidspunkt = :vedtatt_tidspunkt,
        behandlet_automatisk = :behandlet_automatisk,
        fra_og_med = :fra_og_med,
        til_og_med = :til_og_med,
        meldekortdager = :meldekortdager,
        sist_endret = :sist_endret
        """.trimIndent()
}
