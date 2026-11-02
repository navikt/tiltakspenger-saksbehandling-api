package no.nav.tiltakspenger.saksbehandling.statistikk.kodeverk.infra.repo

import kotliquery.TransactionalSession
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.statistikk.kodeverk.Tiltakstypekodeverkrad
import java.time.Clock
import java.time.Instant

object StatistikkKodeverkTiltakstypePostgresRepo {
    /**
     * Lukker gjeldende rader som ikke lenger finnes i [gjeldende], og legger til rader som mangler.
     * En endret stønadstype gir derfor en lukket og en ny rad.
     * Første gang tabellen fylles, får radene `gyldig_fra = null`, fordi mappingen gjaldt før kodeverket fantes.
     */
    fun synkroniser(gjeldende: Set<Tiltakstypekodeverkrad>, clock: Clock, tx: TransactionalSession) {
        tx.run(sqlQuery("lock table statistikk_kodeverk_tiltakstype in exclusive mode").asExecute)
        val erTom = tx.run(sqlQuery("select not exists (select 1 from statistikk_kodeverk_tiltakstype) as tom").map { it.boolean("tom") }.asSingle)!!
        val lagrede = tx.run(
            sqlQuery(
                "select id, ekstern_kildesystem, ekstern_tiltakskode, ekstern_stonadstype from statistikk_kodeverk_tiltakstype where gyldig_til is null",
            ).map {
                it.long("id") to Tiltakstypekodeverkrad(
                    eksternKildesystem = it.string("ekstern_kildesystem"),
                    eksternTiltakskode = it.string("ekstern_tiltakskode"),
                    eksternStønadstype = it.string("ekstern_stonadstype"),
                )
            }.asList,
        )
        val nå = Instant.now(clock)

        lagrede.filter { (_, rad) -> rad !in gjeldende }.forEach { (id, _) ->
            tx.run(sqlQuery("update statistikk_kodeverk_tiltakstype set gyldig_til = :na, sist_endret = :na where id = :id", "na" to nå, "id" to id).asUpdate)
        }
        (gjeldende - lagrede.map { it.second }.toSet()).forEach {
            tx.run(
                sqlQuery(
                    """
                    insert into statistikk_kodeverk_tiltakstype (ekstern_kildesystem, ekstern_tiltakskode, ekstern_stonadstype, gyldig_fra, opprettet, sist_endret)
                    values (:ekstern_kildesystem, :ekstern_tiltakskode, :ekstern_stonadstype, :gyldig_fra, :opprettet, :opprettet)
                    """.trimIndent(),
                    "ekstern_kildesystem" to it.eksternKildesystem,
                    "ekstern_tiltakskode" to it.eksternTiltakskode,
                    "ekstern_stonadstype" to it.eksternStønadstype,
                    "gyldig_fra" to if (erTom) null else nå,
                    "opprettet" to nå,
                ).asUpdate,
            )
        }
    }
}
