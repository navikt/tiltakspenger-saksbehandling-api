package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow

/**
 * En samling av alle behandlinger innenfor en gitt sak. Listen er tom når vi oppretter saken og før vi oppretter den
 * første behandlingen.
 * Garanterer at første elementet er en førstegangsbehandling og de resterende revurderinger.
 */
data class Behandlinger(
    val behandlinger: List<Behandling>,
) : List<Behandling> by behandlinger {

    constructor(behandling: Behandling) : this(listOf(behandling))

    val revurderinger: Revurderinger = Revurderinger(behandlinger.filter { it.erRevurdering })
    val førstegangsBehandlinger = this.behandlinger.filter { it.erFørstegangsbehandling }
    val ikkeAvbrutteFørstegangsbehandlinger: List<Behandling> =
        førstegangsBehandlinger.filterNot { it.status == Behandlingsstatus.AVBRUTT }

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

    fun hentÅpneBehandlinger(): List<Behandling> {
        return behandlinger.filterNot { it.erAvsluttet }
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

        /**
         * En antagelse er at førstegangsbehandlinger ikke kan ha tilstøtende perioder. Ved utvidelse av perioden vil lage en revurdering
         * Det vil si at det 'alltid' skal være hull mmellom periodene til alle førstegangsbehandlingene
         */
        behandlinger.filter { it.erFørstegangsbehandling }.map { it.virkningsperiode }
            .zipWithNext { a, b ->
                if (a != null && b != null) {
                    require(!a.overlapperMed(b)) { "Førstegangsbehandlinger kan ikke ha overlappende virkningsperiode" }
                    require(!a.tilstøter(b)) { "Førstegangsbehandlinger kan ikke tilstøte hverandre (må ha hull i mellom)" }
                }
            }
    }
}
