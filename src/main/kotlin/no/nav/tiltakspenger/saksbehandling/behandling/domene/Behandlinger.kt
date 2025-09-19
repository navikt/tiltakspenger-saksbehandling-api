package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow

/**
 * En samling av alle behandlinger innenfor en gitt sak. Listen er tom når vi oppretter saken og før vi oppretter den
 * første behandlingen.
 * Garanterer at første elementet er en søknadsbehandling og de resterende revurderinger.
 */
data class Behandlinger(
    val behandlinger: List<Rammebehandling>,
) : List<Rammebehandling> by behandlinger {

    constructor(behandling: Rammebehandling) : this(listOf(behandling))

    val revurderinger: Revurderinger = Revurderinger(behandlinger.filterIsInstance<Revurdering>())
    val søknadsbehandlinger: List<Søknadsbehandling> = behandlinger.filterIsInstance<Søknadsbehandling>()

    fun leggTilSøknadsbehandling(
        søknadsbehandling: Søknadsbehandling,
    ): Behandlinger {
        return this.copy(behandlinger = this.behandlinger + søknadsbehandling)
    }

    fun leggTilRevurdering(
        revurdering: Revurdering,
    ): Behandlinger {
        return this.copy(behandlinger = this.behandlinger + revurdering)
    }

    fun hentBehandling(behandlingId: BehandlingId): Rammebehandling? {
        return behandlinger.singleOrNullOrThrow { it.id == behandlingId }
    }

    fun hentÅpneBehandlinger(): List<Rammebehandling> {
        return behandlinger.filterNot { it.erAvsluttet }
    }

    fun oppdaterBehandling(behandling: Rammebehandling): Behandlinger {
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

    companion object {
        fun empty(): Behandlinger {
            return Behandlinger(emptyList())
        }
    }
}
