package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste

data class Meldekortvedtaksliste(
    val verdi: List<Meldekortvedtak>,
) : List<Meldekortvedtak> by verdi {
    @Suppress("unused")
    constructor(value: Meldekortvedtak) : this(listOf(value))

    val fnr = this.distinctBy { it.fnr }.map { it.fnr }.singleOrNullOrThrow()
    val sakId = this.distinctBy { it.sakId }.map { it.sakId }.singleOrNullOrThrow()
    val saksnummer = this.distinctBy { it.saksnummer }.map { it.saksnummer }.singleOrNullOrThrow()

    val utbetalinger: List<VedtattUtbetaling> by lazy {
        verdi.map { it.utbetaling }
    }

    init {
        if (verdi.isNotEmpty()) {
            (this.map { it.id.toString() } + this.map { it.meldekortBehandling.id.toString() }).also {
                require(it.size == it.distinct().size) { "Klagebehandlingene og klagevedtakene kan ikke ha overlapp i IDer. sakId=$sakId, saksnummer=$saksnummer" }
            }
            verdi.mapNotNull { it.journalpostId }.let { journalpostIds ->
                require(journalpostIds.size == journalpostIds.distinct().size) {
                    "Alle meldekortvedtakene må ha unik journalpostId. ${verdi.map { it.id to it.journalpostId }}"
                }
            }
            require(
                verdi.zipWithNext()
                    .all { (a, b) -> a.opprettet < b.opprettet },
            ) { "Meldekortvedtakene må være sortert på opprettet, men var ${verdi.map { it.id to it.opprettet }}" }
        }
    }

    fun hentForMeldekortBehandling(id: MeldekortId): Meldekortvedtak? {
        return verdi.find { it.meldekortId == id }
    }

    fun leggTil(meldekortvedtak: Meldekortvedtak): Meldekortvedtaksliste {
        return Meldekortvedtaksliste(verdi + meldekortvedtak)
    }

    companion object {
        fun empty(): Meldekortvedtaksliste {
            return Meldekortvedtaksliste(emptyList())
        }
    }
}
