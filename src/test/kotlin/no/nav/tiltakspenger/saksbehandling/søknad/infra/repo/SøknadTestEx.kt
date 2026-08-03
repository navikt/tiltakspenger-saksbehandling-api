package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadDAO.toSøknad

/**
 * Oppslag mot søknadstabellene som kun testlaget trenger.
 * Prodkoden leser søknader gjennom sak-aggregatet; dette oppslaget finnes for testinfra som slår opp på fnr uten en sak i hånda.
 * Session-telleren skrus av fordi søknadsfallbacken i [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeKlient] kalles med en åpen sesjon i lokal kjøring.
 *
 * Mappingen gjenbrukes fra [SøknadDAO] — den brukes av prodspørringene og skal ikke dupliseres her.
 */
fun PostgresSessionFactory.hentSøknadForSøknadId(søknadId: SøknadId): Søknad? =
    withSession(disableSessionCounter = true) { session ->
        SøknadDAO.hentForSøknadId(søknadId, session)
    }

fun PostgresSessionFactory.hentSøknaderForFnr(fnr: Fnr): List<Søknad> =
    withSession(disableSessionCounter = true) { session ->
        session.run(
            sqlQuery(
                """
                    select *
                    from søknad s
                    join sak on sak.id = s.sak_id where sak.fnr = :fnr
                """.trimIndent(),
                "fnr" to fnr.verdi,
            ).map { row -> row.toSøknad(session) }.asList,
        )
    }
