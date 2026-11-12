package no.nav.tiltakspenger.saksbehandling.barnetillegg

import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.toDbJson
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext

/**
 * Skriver om det lagrede barnetillegget til [behandlingId] til [BarnetilleggV1], slik behandlinger fra før [BarnetilleggV2] er lagret.
 * Rutene lager bare [BarnetilleggV2], så dette er eneste måte å teste eldre data på gjennom rutene.
 */
fun TestApplicationContext.lagreBarnetilleggSomV1(behandlingId: RammebehandlingId) {
    val behandling = behandlingContext.rammebehandlingRepo.hent(behandlingId)
    val v1 = BarnetilleggV1(periodisering = behandling.barnetilleggsperioder!!, begrunnelse = behandling.barnetillegg!!.begrunnelse)
    (sessionFactory as PostgresSessionFactory).withSession { session ->
        session.run(
            queryOf(
                "update behandling set barnetillegg = :barnetillegg::jsonb where id = :id",
                mapOf("barnetillegg" to v1.toDbJson(), "id" to behandlingId.toString()),
            ).asUpdate,
        )
    }
}
