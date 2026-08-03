package no.nav.tiltakspenger.saksbehandling.person.identhendelser.infra.repo

import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory

/**
 * Oppslag mot `identhendelse` som kun testene trenger.
 * Prodkoden henter per id og id-ene som ikke er behandlet; denne spørringen finnes for å se hva som faktisk ble skrevet for et fnr.
 * Den hører derfor i testlaget, ikke som `@TestOnly` på [IdenthendelseRepository].
 *
 * Mappingen gjenbrukes fra repoet — den brukes av prodspørringene og skal ikke dupliseres her.
 */
fun PostgresSessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr: Fnr): List<IdenthendelseDb> =
    withSession { session ->
        session.run(
            queryOf("select * from identhendelse where gammelt_fnr = ?", gammeltFnr.verdi)
                .map { row -> row.toIdenthendelseDb() }
                .asList,
        )
    }
