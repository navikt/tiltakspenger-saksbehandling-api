package no.nav.tiltakspenger.saksbehandling.lokal.dagpenger.infra.http

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.dagpenger.domene.Dagpengeperiode
import no.nav.tiltakspenger.saksbehandling.dagpenger.infra.http.DagpengerKlient
import no.nav.tiltakspenger.saksbehandling.lokal.dagpenger.infra.repo.LokalDagpengerPostgresRepo
import java.time.Clock

/**
 * Fake for lokal kjøring (LocalApplicationContext).
 * Svarer med dagpengeperiodene som er satt via dev-endepunktet og lagret i `lokal.dagpengeperioder`, slik at de overlever omstart.
 * Personer uten satte perioder får tom liste.
 * Faken filtrerer ikke på perioden den blir spurt om.
 * Testene bruker [no.nav.tiltakspenger.saksbehandling.dagpenger.infra.http.DagpengerFakeKlient].
 */
class DagpengerFakeLokalKlient(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) : DagpengerKlient {

    /**
     * Session-telleren skrus av fordi klienten kan kalles med en åpen sesjon.
     */
    override suspend fun hentDagpengeperioder(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<Dagpengeperiode>> =
        sessionFactory.withSession(disableSessionCounter = true) { session ->
            LokalDagpengerPostgresRepo.hentForFnr(fnr, session)
        }.right()

    /** Erstatter dagpengeperiodene som er satt for [fnr]. */
    fun settDagpengeperioder(fnr: Fnr, perioder: List<Dagpengeperiode>) {
        sessionFactory.withSession { session ->
            LokalDagpengerPostgresRepo.lagre(fnr, perioder, nå(clock), session)
        }
    }
}
