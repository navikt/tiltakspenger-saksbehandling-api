package no.nav.tiltakspenger.saksbehandling.repository.statistikk.stønad

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.statistikk.stønad.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.statistikk.stønad.StatistikkUtbetalingDTO
import org.intellij.lang.annotations.Language
import org.postgresql.util.PGobject
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

    fun toPGObject(value: Any?) = PGobject().also {
        it.type = "json"
        it.value = value?.let { v -> objectMapper.writeValueAsString(v) }
    }
}
