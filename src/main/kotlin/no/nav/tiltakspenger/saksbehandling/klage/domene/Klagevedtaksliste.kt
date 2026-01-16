package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow

data class Klagevedtaksliste(
    val verdi: List<Klagevedtak>,
) : List<Klagevedtak> by verdi {

    @Suppress("unused")
    constructor(value: Klagevedtak) : this(listOf(value))

    val fnr = this.distinctBy { it.fnr }.map { it.fnr }.singleOrNullOrThrow()
    val sakId = this.distinctBy { it.sakId }.map { it.sakId }.singleOrNullOrThrow()
    val saksnummer = this.distinctBy { it.saksnummer }.map { it.saksnummer }.singleOrNullOrThrow()

    fun leggTil(klagevedtak: Klagevedtak): Klagevedtaksliste {
        return Klagevedtaksliste(verdi + klagevedtak)
    }

    fun hentForKlagevedtakId(klagevedtakId: VedtakId): Klagevedtak {
        return verdi.single { it.id == klagevedtakId }
    }

    fun hentForKlagebehandlingId(klagebehandlingId: KlagebehandlingId): Klagevedtak {
        return verdi.single { it.behandling.id == klagebehandlingId }
    }

    companion object {
        fun empty(): Klagevedtaksliste {
            return Klagevedtaksliste(emptyList())
        }
    }

    init {
        if (verdi.isNotEmpty()) {
            (this.map { it.id.toString() } + this.map { it.behandling.id.toString() }).also {
                require(it.size == it.distinct().size) { "Klagebehandlingene og klagevedtakene kan ikke ha overlapp i IDer. sakId=$sakId, saksnummer=$saksnummer" }
            }
            this.map { it.opprettet }.also {
                require(
                    it.zipWithNext()
                        .all { (a, b) -> a < b },
                ) { "Klagevedtakene må være sortert på opprettet-tidspunktet, men var ${this.map { it.id to it.opprettet }}. sakId=$sakId, saksnummer=$saksnummer" }
            }
            verdi.mapNotNull { it.journalpostId }.let { journalpostIds ->
                require(journalpostIds.size == journalpostIds.distinct().size) {
                    "Alle meldekortvedtakene må ha unik journalpostId. ${verdi.map { it.id to it.journalpostId }}"
                }
            }
        }
    }
}
