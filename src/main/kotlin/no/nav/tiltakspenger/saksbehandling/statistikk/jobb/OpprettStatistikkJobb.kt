package no.nav.tiltakspenger.saksbehandling.statistikk.jobb

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.infra.repo.toPGObject
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.toStatistikkMeldeperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingPostgresRepo
import java.time.Clock

class OpprettStatistikkJobb(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger { }

    // TODO: Tas etter at utbetalingsstatistikken er oppdatert
    fun opprettMeldekortStatistikk() {
        // hent alle meldekortbehandlinger iverksatt før 2025-09-01 07:00:00.719033 +00:00
        // sjekk om det finnes statistikk-innslag for sakid+meldeperiodekjedeid
        // hvis ja: ignorer
        // hvis nei: Opprett innslag
    }

    fun leggTilMeldeperioderForUtbetalingsstatistikk() {
        sessionFactory.withSession { session ->
            val utbetalingerUtenMeldeperioder = hentStatistikkUtbetalingerUtenMeldeperioder(session)
            log.info { "Hentet ut ${utbetalingerUtenMeldeperioder.size} utbetalingsinnslag uten meldeperiode" }

            utbetalingerUtenMeldeperioder.forEach {
                log.info { "Behandler statistikk-innslag for utbetaling med id ${it.id}" }
                val utbetalingId = UtbetalingId.fromString(it.id)
                val utbetaling = UtbetalingPostgresRepo.hent(utbetalingId, session)
                    ?: throw IllegalStateException("Fant ikke utbetaling for id ${it.id}")
                val meldeperioder = utbetaling.beregning.beregninger.toList().map {
                    it.toStatistikkMeldeperiode()
                }
                oppdaterStatistikkUtbetaling(it.id, meldeperioder, session)
                log.info { "Oppdaterte statistikk-innslag for utbetaling med id ${it.id}" }
            }
        }
    }

    private fun oppdaterStatistikkUtbetaling(
        id: String,
        meldeperioder: List<StatistikkUtbetalingDTO.StatistikkMeldeperiode>,
        session: Session,
    ) {
        session.run(
            queryOf(
                """
                    update statistikk_utbetaling set meldeperioder = :meldeperioder, sist_endret = :sist_endret where id = :id
                """.trimIndent(),
                mapOf(
                    "meldeperioder" to toPGObject(meldeperioder),
                    "sist_endret" to nå(clock),
                    "id" to id,
                ),
            ).asUpdate,
        )
    }

    private fun hentStatistikkUtbetalingerUtenMeldeperioder(session: Session): List<StatistikkUtbetalingDTO> =
        session.run(
            queryOf(
                """
                    select *
                    from statistikk_utbetaling
                    where meldeperioder is null
                """.trimIndent(),
            ).map { row -> row.toStatistikkUtbetalingDTO() }
                .asList,
        )

    private fun Row.toStatistikkUtbetalingDTO() =
        StatistikkUtbetalingDTO(
            id = string("id"),
            sakId = string("sak_id"),
            saksnummer = string("saksnummer"),
            ordinærBeløp = int("ordinar_belop"),
            barnetilleggBeløp = int("barnetillegg_belop"),
            totalBeløp = int("belop"),
            posteringDato = localDate("posteringsdato"),
            gyldigFraDatoPostering = localDate("gyldig_fra_dato"),
            gyldigTilDatoPostering = localDate("gyldig_til_dato"),
            utbetalingId = string("utbetaling_id"),
            vedtakId = objectMapper.readValue(string("vedtak_id")),
            opprettet = localDateTimeOrNull("opprettet"),
            sistEndret = localDateTimeOrNull("sist_endret"),
            brukerId = string("bruker_id"),
            meldeperioder = emptyList(),
        )
}
