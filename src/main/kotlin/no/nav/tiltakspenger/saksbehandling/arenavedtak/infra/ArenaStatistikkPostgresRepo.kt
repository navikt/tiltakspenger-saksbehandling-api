package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra

import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.arenavedtak.ArenaStatistikkRepo
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaStatistikkForMåned
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
}
