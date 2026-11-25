package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra

import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.arenavedtak.ArenaStatistikkRepo
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaSisteDagMedRettEtter
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaStatistikkForMåned
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaUtbetalingerForMåned
import java.time.Instant

class ArenaStatistikkPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : ArenaStatistikkRepo {
    override fun sistHentet(): Instant? {
        return sessionFactory.withSession { session ->
            session.run(sqlQuery("select max(hentet) as hentet from arena_statistikk_per_maned").map { it.instantOrNull("hentet") }.asSingle)
        }
    }

    override fun lagre(statistikk: List<ArenaStatistikkForMåned>, hentet: Instant) {
        sessionFactory.withTransaction { tx ->
            statistikk.forEach {
                tx.run(
                    sqlQuery(
                        """
                        insert into arena_statistikk_per_maned (maned, saker_med_rett, nye_rettigheter, gjenopptak, endringer, stans, hentet)
                        values (:maned, :saker_med_rett, :nye_rettigheter, :gjenopptak, :endringer, :stans, :hentet)
                        on conflict (maned) do update set
                            saker_med_rett = excluded.saker_med_rett,
                            nye_rettigheter = excluded.nye_rettigheter,
                            gjenopptak = excluded.gjenopptak,
                            endringer = excluded.endringer,
                            stans = excluded.stans,
                            hentet = excluded.hentet
                        """.trimIndent(),
                        "maned" to it.måned.atDay(1),
                        "saker_med_rett" to it.sakerMedRett,
                        "nye_rettigheter" to it.nyeRettigheter,
                        "gjenopptak" to it.gjenopptak,
                        "endringer" to it.endringer,
                        "stans" to it.stans,
                        "hentet" to hentet,
                    ).asUpdate,
                )
            }
        }
    }

    override fun sisteDagMedRettSistHentet(): Instant? {
        return sessionFactory.withSession { session ->
            session.run(sqlQuery("select max(hentet) as hentet from arena_siste_dag_med_rett").map { it.instantOrNull("hentet") }.asSingle)
        }
    }

    override fun lagreSisteDagMedRett(fordeling: ArenaSisteDagMedRettEtter, hentet: Instant) {
        sessionFactory.withTransaction { tx ->
            tx.run(sqlQuery("delete from arena_siste_dag_med_rett").asUpdate)
            fordeling.perSisteDagMedRett.forEach {
                tx.run(
                    sqlQuery(
                        """
                        insert into arena_siste_dag_med_rett (etter, siste_dag_med_rett, saker, personer, hentet)
                        values (:etter, :siste_dag_med_rett, :saker, :personer, :hentet)
                        """.trimIndent(),
                        "etter" to fordeling.etter,
                        "siste_dag_med_rett" to it.sisteDagMedRett,
                        "saker" to it.saker,
                        "personer" to it.personer,
                        "hentet" to hentet,
                    ).asUpdate,
                )
            }
        }
    }

    override fun utbetalingerSistHentet(): Instant? {
        return sessionFactory.withSession { session ->
            session.run(sqlQuery("select max(hentet) as hentet from arena_utbetalinger_per_maned").map { it.instantOrNull("hentet") }.asSingle)
        }
    }

    override fun lagreUtbetalinger(statistikk: List<ArenaUtbetalingerForMåned>, hentet: Instant) {
        sessionFactory.withTransaction { tx ->
            statistikk.forEach {
                tx.run(
                    sqlQuery(
                        """
                        insert into arena_utbetalinger_per_maned (
                            maned, personer_med_utbetaling, dager_utbetalt, belop_grunnsats, belop_barnetillegg,
                            personer_med_beregnet_ikke_utbetalt, belop_beregnet_ikke_utbetalt, hentet
                        )
                        values (
                            :maned, :personer_med_utbetaling, :dager_utbetalt, :belop_grunnsats, :belop_barnetillegg,
                            :personer_med_beregnet_ikke_utbetalt, :belop_beregnet_ikke_utbetalt, :hentet
                        )
                        on conflict (maned) do update set
                            personer_med_utbetaling = excluded.personer_med_utbetaling,
                            dager_utbetalt = excluded.dager_utbetalt,
                            belop_grunnsats = excluded.belop_grunnsats,
                            belop_barnetillegg = excluded.belop_barnetillegg,
                            personer_med_beregnet_ikke_utbetalt = excluded.personer_med_beregnet_ikke_utbetalt,
                            belop_beregnet_ikke_utbetalt = excluded.belop_beregnet_ikke_utbetalt,
                            hentet = excluded.hentet
                        """.trimIndent(),
                        "maned" to it.måned.atDay(1),
                        "personer_med_utbetaling" to it.personerMedUtbetaling,
                        "dager_utbetalt" to it.dagerUtbetalt,
                        "belop_grunnsats" to it.beløpGrunnsats,
                        "belop_barnetillegg" to it.beløpBarnetillegg,
                        "personer_med_beregnet_ikke_utbetalt" to it.personerMedBeregnetIkkeUtbetalt,
                        "belop_beregnet_ikke_utbetalt" to it.beløpBeregnetIkkeUtbetalt,
                        "hentet" to hentet,
                    ).asUpdate,
                )
            }
        }
    }
}
