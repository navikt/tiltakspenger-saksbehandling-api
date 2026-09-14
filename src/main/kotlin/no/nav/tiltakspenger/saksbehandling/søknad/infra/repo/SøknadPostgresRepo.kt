package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

class SøknadPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : SøknadRepo {
    override fun lagre(
        søknad: Søknad,
        txContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(txContext) {
            SøknadDAO.lagreHeleSøknaden(søknad, it)
        }
    }

    override fun finnSakIdForTiltaksdeltakelse(tiltaksdeltakerId: TiltaksdeltakerId): SakId? =
        sessionFactory.withSession {
            SøknadDAO.finnSakIdForTiltaksdeltakelse(tiltaksdeltakerId, it)
        }

    override fun lagreAvbruttSøknad(søknad: Søknad, txContext: TransactionContext?) {
        require(søknad.erAvbrutt) { "Kan ikke lagre en søknad som ikke er avbrutt" }
        sessionFactory.withTransaction(txContext) { session ->
            SøknadDAO.lagreAvbruttSøknad(søknad.id, søknad.avbrutt, session)
        }
    }

    override fun lagreGjenopprettetSøknad(søknad: Søknad, txContext: TransactionContext) {
        require(søknad.erGjenopprettet) { "Kan ikke lagre en gjenopprettet søknad som fortsatt er avbrutt" }
        sessionFactory.withTransaction(txContext) { session ->
            SøknadDAO.lagreGjenopprettetSøknad(søknad.id, søknad.avbrutt, session)
        }
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, context: TransactionContext?) {
        sessionFactory.withTransaction(context) {
            SøknadDAO.oppdaterFnr(
                gammeltFnr = gammeltFnr,
                nyttFnr = nyttFnr,
                session = it,
            )
        }
    }

    override fun hentUbehandledeSøknadIder(limit: Int): List<SøknadId> {
        return sessionFactory.withTransaction {
            SøknadDAO.hentUbehandledeSøknadIder(
                limit = limit,
                session = it,
            )
        }
    }

    override fun hentUbehandletSøknad(søknadId: SøknadId): InnvilgbarSøknad? {
        return sessionFactory.withTransaction {
            SøknadDAO.hentUbehandletSøknad(
                søknadId = søknadId,
                session = it,
            )
        }
    }
}
