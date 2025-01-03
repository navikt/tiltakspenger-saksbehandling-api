package no.nav.tiltakspenger.saksbehandling.domene.behandling

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.libs.common.BehandlingId

/**
 * En samling av alle behandlinger innenfor en gitt sak.
 * Garanterer at første elementet er en førstegangsbehandling og de resterende revurderinger.
 */
data class Behandlinger(
    val behandlinger: NonEmptyList<Behandling>,
) : List<Behandling> by behandlinger {

    constructor(behandling: Behandling) : this(nonEmptyListOf(behandling))

    val sakId = behandlinger.distinctBy { it.sakId }.single()
    val revurderinger: Revurderinger = Revurderinger(behandlinger.drop(1))
    val førstegangsbehandling: Behandling = behandlinger.first().also {
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

    init {
        require(behandlinger.distinctBy { it.id }.size == behandlinger.size) { "Behandlinger inneholder duplikate behandlinger: ${behandlinger.map { it.id.toString() }}" }
        require(behandlinger.distinctBy { it.sakId }.size == 1) { "Behandlinger inneholder behandlinger for ulike saker: ${behandlinger.map { it.sakId.toString() }}" }
        require(behandlinger.distinctBy { it.fnr }.size == 1) { "Behandlinger inneholder behandlinger for ulike personer: ${behandlinger.map { it.fnr.toString() }}" }
        require(behandlinger.distinctBy { it.saksnummer }.size == 1) { "Behandlinger inneholder behandlinger for ulike saksnummer: ${behandlinger.map { it.saksnummer.toString() }}" }
        behandlinger.map { it.opprettet }
            .zipWithNext { a, b -> require(a < b) { "Behandlinger er ikke sortert på opprettet-tidspunkt" } }
    }
}
