package no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.infra.repo

import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.infra.repo.toPGObject
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.StatistikkUtbetalingDTO
import org.intellij.lang.annotations.Language
import java.time.Clock

class StatistikkStønadPostgresRepo {
    companion object {
        fun oppdaterFnr(
            gammeltFnr: Fnr,
            nyttFnr: Fnr,
            clock: Clock,
            transactionalSession: TransactionalSession,
        ) {
            oppdaterFnrForStonad(
                gammeltFnr = gammeltFnr,
                nyttFnr = nyttFnr,
                clock = clock,
                session = transactionalSession,
            )
            oppdaterFnrForUtbetaling(
                gammeltFnr = gammeltFnr,
                nyttFnr = nyttFnr,
                clock = clock,
                session = transactionalSession,
            )
        }

        private fun oppdaterFnrForStonad(
            gammeltFnr: Fnr,
            nyttFnr: Fnr,
            clock: Clock,
            session: Session,
        ) {
            session.run(
                queryOf(
                    """
                        update statistikk_stonad set bruker_id = :nytt_fnr, sist_endret = :sist_endret where bruker_id = :gammelt_fnr
                    """.trimIndent(),
                    mapOf(
                        "nytt_fnr" to nyttFnr.verdi,
                        "gammelt_fnr" to gammeltFnr.verdi,
                        "sist_endret" to nå(clock),
                    ),
                ).asUpdate,
            )
        }

        private fun oppdaterFnrForUtbetaling(
            gammeltFnr: Fnr,
            nyttFnr: Fnr,
            clock: Clock,
            session: Session,
        ) {
            session.run(
                queryOf(
                    """
                    update statistikk_utbetaling set bruker_id = :nytt_fnr, sist_endret = :sist_endret where bruker_id = :gammelt_fnr
                    """.trimIndent(),
                    mapOf(
                        "nytt_fnr" to nyttFnr.verdi,
                        "gammelt_fnr" to gammeltFnr.verdi,
                        "sist_endret" to nå(clock),
                    ),
                ).asUpdate,
            )
        }

        internal fun lagre(
            dto: StatistikkStønadDTO,
            clock: Clock,
            tx: TransactionalSession,
        ) {
            tx.run(
                queryOf(
                    lagreStonadSql,
                    mapOf(
                        "id" to dto.id.toString(),
                        "brukerId" to dto.brukerId,
                        "sakId" to dto.sakId,
                        "saksnummer" to dto.saksnummer,
                        "resultat" to dto.resultat.toString(),
                        "sakDato" to dto.sakDato,
                        "ytelse" to dto.ytelse,
                        "soknadId" to dto.søknadId,
                        "soknadDato" to dto.søknadDato,
                        "gyldigFraDatoSoknad" to dto.søknadFraDato,
                        "gyldigTilDatoSoknad" to dto.søknadTilDato,
                        "vedtakId" to dto.vedtakId,
                        "type" to dto.vedtaksType,
                        "vedtakDato" to dto.vedtakDato,
                        "vedtaksperiode_fra_og_med" to dto.vedtaksperiodeFraOgMed,
                        "vedtaksperiode_til_og_med" to dto.vedtaksperiodeTilOgMed,
                        "fagsystem" to dto.fagsystem,
                        "sistEndret" to nå(clock),
                        "opprettet" to nå(clock),
                        "barnetillegg" to toPGObject(dto.barnetillegg),
                        "harBarnetillegg" to dto.harBarnetillegg,
                        "innvilgelsesperioder" to toPGObject(dto.innvilgelsesperioder),
                        "omgjorRammevedtakId" to dto.omgjørRammevedtakId,
                        "omgjorRammevedtak" to toPGObject(dto.omgjørRammevedtak),
                    ),
                ).asUpdate,
            )
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
                        "posteringsDato" to dto.posteringDato,
                        "gyldigFraDato" to dto.gyldigFraDatoPostering,
                        "gyldigTilDato" to dto.gyldigTilDatoPostering,
                        "utbetaling_id" to dto.utbetalingId,
                        "vedtak_id" to toPGObject(dto.vedtakId),
                        "opprettet" to dto.opprettet,
                        "sist_endret" to dto.sistEndret,
                        "bruker_id" to dto.brukerId,
                        "meldeperioder" to toPGObject(dto.meldeperioder),
                    ),
                ).asUpdate,
            )
        }
    }
}

@Language("SQL")
private val lagreStonadSql =
    """
        insert into statistikk_stonad (
        id,
        bruker_id,
        sak_id,
        saksnummer,
        resultat,
        sak_dato,
        ytelse,
        soknad_id,
        soknad_dato,
        gyldig_fra_dato_soknad,
        gyldig_til_dato_soknad,
        vedtak_id,
        type,
        vedtak_dato,
        vedtaksperiode_fra_og_med,
        vedtaksperiode_til_og_med,
        fagsystem,
        sist_endret,
        opprettet,
        barnetillegg,
        har_barnetillegg,
        innvilgelsesperioder,
        omgjor_rammevedtak_id,
        omgjor_rammevedtak
        ) values (
        :id,
        :brukerId,
        :sakId,
        :saksnummer,
        :resultat,
        :sakDato,
        :ytelse,
        :soknadId,
        :soknadDato,
        :gyldigFraDatoSoknad,
        :gyldigTilDatoSoknad,
        :vedtakId,
        :type,
        :vedtakDato,
        :vedtaksperiode_fra_og_med,
        :vedtaksperiode_til_og_med,
        :fagsystem,
        :sistEndret,
        :opprettet,
        :barnetillegg,
        :harBarnetillegg,
        :innvilgelsesperioder,
        :omgjorRammevedtakId,
        :omgjorRammevedtak
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
        posteringsdato,
        gyldig_fra_dato,
        gyldig_til_dato,
        utbetaling_id,
        vedtak_id,
        opprettet,
        sist_endret,
        bruker_id,
        meldeperioder
        ) values (
        :id,
        :sakId,
        :saksnummer,
        :belop,
        :ordinaerBelop,
        :barnetilleggBelop,
        :posteringsDato,
        :gyldigFraDato,
        :gyldigTilDato,
        :utbetaling_id,
        :vedtak_id,
        :opprettet,
        :sist_endret,
        :bruker_id,
        :meldeperioder
        )
    """.trimIndent()
