package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

/**
 * En liste med klagebehandlinger tilhørende samme sak.
 * Sortert på opprettet-tidspunkt i stigende rekkefølge.
 */
data class Klagebehandlinger(
    val klagebehandlinger: List<Klagebehandling>,
) : List<Klagebehandling> by klagebehandlinger {

    val fnr: Fnr? = klagebehandlinger.map { it.fnr }.distinct().singleOrNullOrThrow()
    val sakId: SakId? = klagebehandlinger.map { it.sakId }.distinct().singleOrNullOrThrow()
    val saksnummer: Saksnummer? = klagebehandlinger.map { it.saksnummer }.distinct().singleOrNullOrThrow()

    fun oppdaterKlagebehandling(klagebehandling: Klagebehandling): Klagebehandlinger {
        require(klagebehandlinger.map { it.id }.contains(klagebehandling.id)) {
            "Klagebehandling med id ${klagebehandling.id} finnes ikke og kan derfor ikke oppdateres. SakId: ${klagebehandling.sakId}"
        }
        val oppdaterteKlagebehandlinger = klagebehandlinger.map {
            if (it.id == klagebehandling.id) {
                klagebehandling
            } else {
                it
            }
        }
        return Klagebehandlinger(oppdaterteKlagebehandlinger)
    }

    fun leggTilKlagebehandling(klagebehandling: Klagebehandling): Klagebehandlinger {
        return Klagebehandlinger((klagebehandlinger + klagebehandling).sortedBy { it.opprettet })
    }

    fun hentKlagebehandling(klagebehandlingId: KlagebehandlingId): Klagebehandling {
        return klagebehandlinger.single { it.id == klagebehandlingId }
    }

    init {
        klagebehandlinger.map { it.id }.also {
            require(it.distinct().size == it.size) {
                "Klagebehandlingene kan ikke ha samme id. SakId: $sakId, saksnummer: $saksnummer, IDer: $it"
            }
        }
        klagebehandlinger.zipWithNext { a, b ->
            require(a.opprettet < b.opprettet) {
                "Klagebehandlingene må være sortert på opprettet-tidspunkt i stigende rekkefølge. SakId: $sakId, saksnummer: $saksnummer. }}"
            }
        }
    }

    companion object {
        fun empty(): Klagebehandlinger {
            return Klagebehandlinger(emptyList())
        }
    }
}
