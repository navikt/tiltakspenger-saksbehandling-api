package no.nav.tiltakspenger.vedtak.repository.søknad

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.ports.SøknadRepo

internal class PostgresSøknadRepo(
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
}
