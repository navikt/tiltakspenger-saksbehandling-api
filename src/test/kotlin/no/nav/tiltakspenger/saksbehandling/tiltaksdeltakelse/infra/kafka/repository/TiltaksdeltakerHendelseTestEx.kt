package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository

import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelseId

/**
 * Oppslag mot `tiltaksdeltaker_kafka` som kun testene trenger.
 * Prodkoden henter deltakere med ubehandlede hendelser og hendelser per deltaker; disse spørringene finnes for å se hva som faktisk ble skrevet.
 * De hører derfor i testlaget, ikke som `@TestOnly` på [TiltaksdeltakerHendelsePostgresRepo].
 *
 * Mappingen gjenbrukes fra repoet — den brukes av prodspørringene og skal ikke dupliseres her.
 */
fun PostgresSessionFactory.hentUbehandledeTiltaksdeltakerHendelser(): List<TiltaksdeltakerHendelse> =
    withSession { session ->
        session.run(
            sqlQuery(
                """
                    select *
                    from tiltaksdeltaker_kafka
                    where behandlet_tidspunkt is null
                    order by sist_oppdatert asc
                """.trimIndent(),
            ).map { row -> row.tilTiltaksdeltakerHendelse() }.asList,
        )
    }

fun PostgresSessionFactory.hentTiltaksdeltakerHendelse(
    id: TiltaksdeltakerHendelseId,
): TiltaksdeltakerHendelse? = withSession { session ->
    session.run(
        sqlQuery(
            """
                select *
                from tiltaksdeltaker_kafka
                where hendelse_id = :hendelse_id
            """.trimIndent(),
            "hendelse_id" to id.toString(),
        ).map { row -> row.tilTiltaksdeltakerHendelse() }.asSingle,
    )
}

fun PostgresSessionFactory.hentTiltaksdeltakerHendelserForEksternId(
    eksternDeltakerId: String,
): List<TiltaksdeltakerHendelse> = withSession { session ->
    session.run(
        sqlQuery(
            """
                select *
                from tiltaksdeltaker_kafka
                where deltaker_id = :deltaker_id
            """.trimIndent(),
            "deltaker_id" to eksternDeltakerId,
        ).map { row -> row.tilTiltaksdeltakerHendelse() }.asList,
    )
}
