package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import kotliquery.Row
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.åpenPeriodeOrNull
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId

/**
 * Oppslag mot `tiltaksdeltaker_kafka` som kun testene trenger.
 * Prodkoden skriver hendelseshistorikk uten å lese den; disse spørringene finnes for å se hva som faktisk ble skrevet.
 * De hører derfor i testlaget, ikke som `@TestOnly` på [TiltaksdeltakerHendelsePostgresRepo].
 */
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

private fun Row.tilTiltaksdeltakerHendelse(): TiltaksdeltakerHendelse {
    val deltakelse = åpenPeriodeOrNull("deltakelse_periode")
    return TiltaksdeltakerHendelse(
        id = TiltaksdeltakerHendelseId.fromString(string("hendelse_id")),
        eksternDeltakerId = string("deltaker_id"),
        deltakelseFraOgMed = deltakelse?.fraOgMed,
        deltakelseTilOgMed = deltakelse?.tilOgMed,
        dagerPerUke = floatOrNull("dager_per_uke"),
        deltakelsesprosent = floatOrNull("deltakelsesprosent"),
        deltakerstatus = TiltakDeltakerstatus.valueOf(string("deltakerstatus")),
        sakId = SakId.fromString(string("sak_id")),
        internDeltakerId = TiltaksdeltakerId.fromString(string("tiltaksdeltaker_id")),
    )
}
