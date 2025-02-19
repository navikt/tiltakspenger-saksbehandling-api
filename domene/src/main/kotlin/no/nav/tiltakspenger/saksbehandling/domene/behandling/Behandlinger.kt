package no.nav.tiltakspenger.saksbehandling.domene.behandling

import no.nav.tiltakspenger.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.libs.common.BehandlingId

/**
 * En samling av alle behandlinger innenfor en gitt sak. Listen er tom når vi oppretter saken og før vi oppretter den
 * første behandlingen.
 * Garanterer at første elementet er en førstegangsbehandling og de resterende revurderinger.
 */
data class Behandlinger(
    val behandlinger: List<Behandling>,
) : List<Behandling> by behandlinger {

    constructor(behandling: Behandling) : this(listOf(behandling))

    val revurderinger: Revurderinger = Revurderinger(behandlinger.drop(1))
    val førstegangsbehandling: Behandling? = behandlinger.firstOrNull()?.also {
        require(it.erFørstegangsbehandling)
    }

    fun leggTilRevurdering(
        revurdering: Behandling,
    ): Behandlinger {
        require(revurdering.behandlingstype == Behandlingstype.REVURDERING) { "Må være revurdering." }
        val behandlinger = this.behandlinger + revurdering
        return this.copy(behandlinger = behandlinger)
    }

    fun hentBehandling(behandlingId: BehandlingId): Behandling? {
        return behandlinger.singleOrNullOrThrow { it.id == behandlingId }
    }

    fun oppdaterBehandling(behandling: Behandling): Behandlinger {
        behandlinger.single { it.id == behandling.id }
        val behandlinger = this.behandlinger.map { if (it.id == behandling.id) behandling else it }
        return this.copy(behandlinger = behandlinger)
    }

    init {
        require(behandlinger.distinctBy { it.id }.size == behandlinger.size) { "Behandlinger inneholder duplikate behandlinger: ${behandlinger.map { it.id.toString() }}" }
        require(behandlinger.distinctBy { it.sakId }.size <= 1) { "Behandlinger inneholder behandlinger for ulike saker: ${behandlinger.map { it.sakId.toString() }}" }
        require(behandlinger.distinctBy { it.fnr }.size <= 1) { "Behandlinger inneholder behandlinger for ulike personer: ${behandlinger.map { it.fnr.toString() }}" }
        require(behandlinger.distinctBy { it.saksnummer }.size <= 1) { "Behandlinger inneholder behandlinger for ulike saksnummer: ${behandlinger.map { it.saksnummer.toString() }}" }
        behandlinger.map { it.opprettet }
            .zipWithNext { a, b -> require(a < b) { "Behandlinger er ikke sortert på opprettet-tidspunkt" } }
    }
}
