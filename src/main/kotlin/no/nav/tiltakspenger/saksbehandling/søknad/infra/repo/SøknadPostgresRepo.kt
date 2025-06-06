package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad

internal class SøknadPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : SøknadRepo {
    override fun hentForSøknadId(søknadId: SøknadId): Søknad? =
        sessionFactory.withSession {
            SøknadDAO.hentForSøknadId(søknadId, it)
        }

    override fun lagre(
        søknad: Søknad,
        txContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(txContext) {
            SøknadDAO.lagreHeleSøknaden(søknad, it)
        }
    }

    override fun hentSakIdForSoknad(søknadId: SøknadId): SakId? =
        sessionFactory.withSession {
            SøknadDAO.finnSakId(søknadId, it)
        }

    override fun hentSøknaderForFnr(fnr: Fnr): List<Søknad> {
        return sessionFactory.withSession {
            SøknadDAO.hentForFnr(fnr, it)
        }
    }

    override fun finnSakIdForTiltaksdeltakelse(eksternId: String): SakId? =
        sessionFactory.withSession {
            SøknadDAO.finnSakIdForTiltaksdeltakelse(eksternId, it)
        }

    override fun lagreAvbruttSøknad(søknad: Søknad, txContext: TransactionContext?) {
        if (søknad.avbrutt == null) {
            throw IllegalArgumentException("Kan ikke lagre en søknad som ikke er avbrutt")
        }
        sessionFactory.withTransaction(txContext) {
            SøknadDAO.lagreAvbruttSøknad(søknad.id, søknad.avbrutt!!, it)
        }
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr) {
        sessionFactory.withSession {
            SøknadDAO.oppdaterFnr(
                gammeltFnr = gammeltFnr,
                nyttFnr = nyttFnr,
                session = it,
            )
        }
    }
}
