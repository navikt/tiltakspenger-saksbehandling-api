package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.DigitalsøknadRepo
import no.nav.tiltakspenger.saksbehandling.søknad.Digitalsøknad

internal class DigitalsøknadPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : DigitalsøknadRepo {
    override fun hentForSøknadId(søknadId: SøknadId): Digitalsøknad? =
        sessionFactory.withSession {
            DigitalsøknadDAO.hentForSøknadId(søknadId, it)
        }

    override fun lagre(
        søknad: Digitalsøknad,
        txContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(txContext) {
            DigitalsøknadDAO.lagreHeleSøknaden(søknad, it)
        }
    }

    override fun hentSakIdForSoknad(søknadId: SøknadId): SakId? =
        sessionFactory.withSession {
            DigitalsøknadDAO.finnSakId(søknadId, it)
        }

    override fun hentSøknaderForFnr(fnr: Fnr): List<Digitalsøknad> {
        return sessionFactory.withSession {
            DigitalsøknadDAO.hentForFnr(fnr, it)
        }
    }

    override fun finnSakIdForTiltaksdeltakelse(eksternId: String): SakId? =
        sessionFactory.withSession {
            DigitalsøknadDAO.finnSakIdForTiltaksdeltakelse(eksternId, it)
        }

    override fun lagreAvbruttSøknad(søknad: Digitalsøknad, txContext: TransactionContext?) {
        if (søknad.avbrutt == null) {
            throw IllegalArgumentException("Kan ikke lagre en søknad som ikke er avbrutt")
        }
        sessionFactory.withTransaction(txContext) {
            DigitalsøknadDAO.lagreAvbruttSøknad(søknad.id, søknad.avbrutt, it)
        }
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, context: TransactionContext?) {
        sessionFactory.withTransaction(context) {
            DigitalsøknadDAO.oppdaterFnr(
                gammeltFnr = gammeltFnr,
                nyttFnr = nyttFnr,
                session = it,
            )
        }
    }

    override fun hentAlleUbehandledeSoknader(limit: Int): List<Digitalsøknad> {
        return sessionFactory.withTransaction {
            DigitalsøknadDAO.hentAlleUbehandledeSoknader(
                limit = limit,
                session = it,
            )
        }
    }
}
