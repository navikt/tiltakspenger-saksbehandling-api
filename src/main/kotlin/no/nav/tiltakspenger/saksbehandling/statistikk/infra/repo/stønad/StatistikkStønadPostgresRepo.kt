package no.nav.tiltakspenger.saksbehandling.statistikk.infra.repo.stønad

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad.StatistikkUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.infra.repo.toPGObject
import org.intellij.lang.annotations.Language
import java.time.Clock

class StatistikkStønadPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) : StatistikkStønadRepo {
    override fun lagre(
        dto: StatistikkStønadDTO,
        context: TransactionContext?,
    ) {
        sessionFactory.withTransaction(context) { tx ->
            lagre(dto, tx)
        }
    }

    internal fun lagre(
        dto: StatistikkStønadDTO,
        tx: TransactionalSession,
    ) {
        tx.run(
            queryOf(
                lagreSql,
                mapOf(
                    "id" to dto.id.toString(),
                    "brukerId" to dto.brukerId,
                    "sakId" to dto.sakId,
                    "saksnummer" to dto.saksnummer,
                    "resultat" to dto.resultat,
                    "sakDato" to dto.sakDato,
                    "gyldigFraDato" to dto.sakFraDato,
                    "gyldigTilDato" to dto.sakTilDato,
                    "ytelse" to dto.ytelse,
                    "soknadId" to dto.søknadId,
                    "soknadDato" to dto.søknadDato,
                    "gyldigFraDatoSoknad" to dto.søknadFraDato,
                    "gyldigTilDatoSoknad" to dto.søknadTilDato,
                    "vedtakId" to dto.vedtakId,
                    "type" to dto.vedtaksType,
                    "vedtakDato" to dto.vedtakDato,
                    "fom" to dto.vedtakFom,
                    "tom" to dto.vedtakTom,
                    "fagsystem" to dto.fagsystem,
                    "sistEndret" to nå(clock),
                    "opprettet" to nå(clock),
                    "tiltaksdeltakelser" to toPGObject(dto.tiltaksdeltakelser),
                ),
            ).asUpdate,
        )
    }

    override fun lagre(
        dto: StatistikkUtbetalingDTO,
        context: TransactionContext?,
    ) {
        sessionFactory.withTransaction(context) { tx ->
            lagre(dto, tx)
        }
    }

    fun lagre(
        dto: StatistikkUtbetalingDTO,
        tx: TransactionalSession,
    ) {
        tx.run(
            queryOf(
                lagreUtbetalingSql,
                mapOf(
                    "id" to dto.id,
                    "sakId" to dto.sakId,
                    "saksnummer" to dto.saksnummer,
                    "belop" to dto.totalBeløp,
                    "ordinaerBelop" to dto.ordinærBeløp,
                    "barnetilleggBelop" to dto.barnetilleggBeløp,
                    "arsak" to dto.årsak,
                    "posteringsDato" to dto.posteringDato,
                    "gyldigFraDato" to dto.gyldigFraDatoPostering,
                    "gyldigTilDato" to dto.gyldigTilDatoPostering,
                    "utbetaling_id" to dto.utbetalingId,
                ),
            ).asUpdate,
        )
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr) {
        sessionFactory.withSession {
            it.run(
                queryOf(
                    """
                        update statistikk_stonad set bruker_id = :nytt_fnr where bruker_id = :gammelt_fnr
                    """.trimIndent(),
                    mapOf(
                        "nytt_fnr" to nyttFnr.verdi,
                        "gammelt_fnr" to gammeltFnr.verdi,
                    ),
                ).asUpdate,
            )
        }
    }

    // Denne brukes kun for tester
    override fun hent(sakId: SakId): List<StatistikkStønadDTO> = sessionFactory.withSession {
        it.run(
            queryOf(
                """
                    select *
                    from statistikk_stonad
                    where sak_id = :sak_id
                """.trimIndent(),
                mapOf(
                    "sak_id" to sakId.toString(),
                ),
            ).map { row -> row.toStatistikkStonadDTO() }
                .asList,
        )
    }

    @Language("SQL")
    private val lagreSql =
        """
        insert into statistikk_stonad (
        id,
        bruker_id,
        sak_id,
        saksnummer,
        resultat,
        sak_dato,
        gyldig_fra_dato,
        gyldig_til_dato,
        ytelse,
        soknad_id,
        soknad_dato,
        gyldig_fra_dato_soknad,
        gyldig_til_dato_soknad,
        vedtak_id,
        type,
        vedtak_dato,
        fra_og_med,
        til_og_med,
        fagsystem,
        sist_endret,
        opprettet,
        tiltaksdeltakelser
        ) values (
        :id,
        :brukerId,
        :sakId,
        :saksnummer,
        :resultat,
        :sakDato,
        :gyldigFraDato,
        :gyldigTilDato,
        :ytelse,
        :soknadId,
        :soknadDato,
        :gyldigFraDatoSoknad,
        :gyldigTilDatoSoknad,
        :vedtakId,
        :type,
        :vedtakDato,
        :fom,
        :tom,
        :fagsystem,
        :sistEndret,
        :opprettet,
        :tiltaksdeltakelser
        )
        """.trimIndent()

    @Language("SQL")
    private val lagreUtbetalingSql =
        """
        insert into statistikk_utbetaling (
        id,
        sak_id,
        saksnummer,
        belop,
        ordinar_belop,
        barnetillegg_belop,
        arsak,
        posteringsdato,
        gyldig_fra_dato,
        gyldig_til_dato,
        utbetaling_id
        ) values (
        :id,
        :sakId,
        :saksnummer,
        :belop,
        :ordinaerBelop,
        :barnetilleggBelop,
        :arsak,
        :posteringsDato,
        :gyldigFraDato,
        :gyldigTilDato,
        :utbetaling_id
        )
        """.trimIndent()

    private fun Row.toStatistikkStonadDTO() =
        StatistikkStønadDTO(
            id = uuid("id"),
            brukerId = string("bruker_id"),
            sakId = string("sak_id"),
            saksnummer = string("saksnummer"),
            resultat = string("resultat"),
            sakDato = localDate("sak_dato"),
            sakFraDato = localDate("gyldig_fra_dato"),
            sakTilDato = localDate("gyldig_til_dato"),
            ytelse = string("ytelse"),
            søknadId = stringOrNull("soknad_id"),
            søknadDato = localDateOrNull("soknad_dato"),
            søknadFraDato = localDateOrNull("gyldig_fra_dato_soknad"),
            søknadTilDato = localDateOrNull("gyldig_til_dato_soknad"),
            vedtakId = string("vedtak_id"),
            vedtaksType = string("type"),
            vedtakDato = localDate("vedtak_dato"),
            vedtakFom = localDate("fra_og_med"),
            vedtakTom = localDate("til_og_med"),
            fagsystem = string("fagsystem"),
            tiltaksdeltakelser = objectMapper.readValue(string("tiltaksdeltakelser")),
        )
}
