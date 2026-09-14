package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory

/**
 * Leser `ytelser`-objektet ut av jsonb-kolonnen `behandling.saksopplysninger` med egen SQL.
 *
 * Domenet viser bare [Ytelser], ikke formatet på disk.
 * Feltnavnene i [no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.YtelserDbJson] er kontrakten mot rader som allerede er lagret, og den kontrakten er usynlig gjennom en rundtur.
 * Behandlingsresponsen tar med selve ytelsene, men verken `oppslagsperiode` eller `type`, så kolonnen er eneste vei til dem.
 */
fun PostgresSessionFactory.hentYtelserJson(behandlingId: RammebehandlingId): String = withSession { session ->
    session.run(
        queryOf(
            "select saksopplysninger -> 'ytelser' as ytelser from behandling where id = :id",
            mapOf("id" to behandlingId.toString()),
        ).map { row -> row.string("ytelser") }.asSingle,
    )
}!!
