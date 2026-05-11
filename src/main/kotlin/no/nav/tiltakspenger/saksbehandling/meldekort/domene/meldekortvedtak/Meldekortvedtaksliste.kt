package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinje
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import java.time.LocalDateTime

data class Meldekortvedtaksliste(
    private val verdi: List<Meldekortvedtak>,
) : List<Meldekortvedtak> by verdi {
    @Suppress("unused")
    constructor(value: Meldekortvedtak) : this(listOf(value))

    val fnr = this.distinctBy { it.fnr }.map { it.fnr }.singleOrNullOrThrow()
    val sakId = this.distinctBy { it.sakId }.map { it.sakId }.singleOrNullOrThrow()
    val saksnummer = this.distinctBy { it.saksnummer }.map { it.saksnummer }.singleOrNullOrThrow()

    val utbetalinger: List<VedtattUtbetaling> by lazy {
        verdi.map { it.utbetaling }
    }

    val tidslinje: Periodisering<Meldekortvedtak> by lazy {
        verdi.toTidslinje()
    }

    /**
     * Tidspunktet for det sist opprettede meldekortvedtaket på saken, eller `null` om lista er tom.
     * Init-blokken garanterer at lista er sortert stigende på `opprettet`.
     */
    val sisteVedtakOpprettet: LocalDateTime? = verdi.lastOrNull()?.opprettet

    init {
        if (verdi.isNotEmpty()) {
            (this.map { it.id.toString() } + this.map { it.meldekortbehandling.id.toString() }).also {
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

    fun hentForMeldekortbehandling(id: MeldekortId): Meldekortvedtak? {
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
