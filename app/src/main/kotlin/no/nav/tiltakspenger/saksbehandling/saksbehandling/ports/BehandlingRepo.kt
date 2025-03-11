package no.nav.tiltakspenger.saksbehandling.saksbehandling.ports

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandling
import java.time.LocalDateTime

interface BehandlingRepo {
    fun lagre(
        behandling: Behandling,
        transactionContext: TransactionContext? = null,
    )

    fun hentOrNull(
        behandlingId: BehandlingId,
        sessionContext: SessionContext? = null,
    ): Behandling?

    fun hent(
        behandlingId: BehandlingId,
        sessionContext: SessionContext? = null,
    ): Behandling

    fun hentAlleForFnr(fnr: Fnr): List<Behandling>

    fun hentForSøknadId(søknadId: SøknadId): Behandling?

    fun hentFørstegangsbehandlingerTilDatadeling(limit: Int = 10): List<Behandling>

    fun markerSendtTilDatadeling(id: BehandlingId, tidspunkt: LocalDateTime)
}
