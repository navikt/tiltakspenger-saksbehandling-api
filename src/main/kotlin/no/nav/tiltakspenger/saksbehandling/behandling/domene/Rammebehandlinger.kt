package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

/**
 * En samling av alle behandlinger innenfor en gitt sak. Listen er tom når vi oppretter saken og før vi oppretter den
 * første behandlingen.
 * Garanterer at første elementet er en søknadsbehandling og de resterende revurderinger.
 */
data class Rammebehandlinger(
    val behandlinger: List<Rammebehandling>,
) : List<Rammebehandling> by behandlinger {

    constructor(behandling: Rammebehandling) : this(listOf(behandling))

    val revurderinger: Revurderinger = Revurderinger(behandlinger.filterIsInstance<Revurdering>())
    val søknadsbehandlinger: List<Søknadsbehandling> = behandlinger.filterIsInstance<Søknadsbehandling>()

    val fnr: Fnr? by lazy { behandlinger.distinctBy { it.fnr }.map { it.fnr }.singleOrNullOrThrow() }
    val sakId: SakId? by lazy { behandlinger.distinctBy { it.sakId }.map { it.sakId }.singleOrNullOrThrow() }
    val saksnummer: Saksnummer? by lazy {
        behandlinger.distinctBy { it.saksnummer }.map { it.saksnummer }.singleOrNullOrThrow()
    }

    val harÅpenBehandling: Boolean by lazy { åpneBehandlinger.isNotEmpty() }

    val åpneBehandlinger: List<Rammebehandling> by lazy { behandlinger.filterNot { it.erAvsluttet } }

    fun leggTilSøknadsbehandling(
        søknadsbehandling: Søknadsbehandling,
    ): Rammebehandlinger {
        return this.copy(behandlinger = this.behandlinger + søknadsbehandling)
    }

    fun leggTilRevurdering(
        revurdering: Revurdering,
    ): Rammebehandlinger {
        return this.copy(behandlinger = this.behandlinger + revurdering)
    }

    fun hentRammebehandling(rammebehandlingId: BehandlingId): Rammebehandling? {
        return behandlinger.singleOrNullOrThrow { it.id == rammebehandlingId }
    }

    fun oppdaterRammebehandling(behandling: Rammebehandling): Rammebehandlinger {
        behandlinger.single { it.id == behandling.id }
        val behandlinger = this.behandlinger.map { if (it.id == behandling.id) behandling else it }
        return this.copy(behandlinger = behandlinger)
    }

    fun oppdaterKlagebehandling(klagebehandling: Klagebehandling): Rammebehandlinger {
        val behandlinger = this.behandlinger.map {
            if (it.klagebehandling?.id == klagebehandling.id) {
                it.oppdaterKlagebehandling(klagebehandling = klagebehandling)
            } else {
                it
            }
        }
        return this.copy(behandlinger = behandlinger)
    }

    fun åpneRammebehandlingerMedKlagebehandlingId(klagebehandlingId: KlagebehandlingId): List<Rammebehandling> {
        return behandlinger
            .filter { !it.erAvsluttet && it.klagebehandling?.id == klagebehandlingId }
    }

    init {
        require(behandlinger.distinctBy { it.id }.size == behandlinger.size) { "Behandlinger inneholder duplikate behandlinger: ${behandlinger.map { it.id.toString() }}" }
        behandlinger.map { it.opprettet }
            .zipWithNext { a, b -> require(a < b) { "Behandlinger er ikke sortert på opprettet-tidspunkt" } }
    }

    companion object {
        fun empty(): Rammebehandlinger {
            return Rammebehandlinger(emptyList())
        }
    }
}
