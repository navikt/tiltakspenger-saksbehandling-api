package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad

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
        søknad.avbrutt?.let { avbrutt ->
            sessionFactory.withTransaction(txContext) { session ->
                SøknadDAO.lagreAvbruttSøknad(søknad.id, avbrutt, session)
            }
        } ?: throw IllegalArgumentException("Kan ikke lagre en søknad som ikke er avbrutt")
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

    override fun hentAlleUbehandledeSoknader(limit: Int): List<InnvilgbarSøknad> {
        return sessionFactory.withTransaction {
            SøknadDAO.hentAlleUbehandledeSoknader(
                limit = limit,
                session = it,
            )
        }
    }
}
