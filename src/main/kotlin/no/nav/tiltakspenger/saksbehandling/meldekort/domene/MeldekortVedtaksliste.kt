package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling

data class MeldekortVedtaksliste(val verdi: List<MeldekortVedtak>) : List<MeldekortVedtak> by verdi {

    val utbetalinger: List<VedtattUtbetaling> by lazy {
        verdi.map { it.utbetaling }
    }

    init {
        if (verdi.isNotEmpty()) {
            require(
                verdi.map { it.fnr }
                    .distinct().size == 1,
            ) { "Alle meldekortvedtakene må være for samme person. ${verdi.map { it.id }}" }

            require(
                verdi.map { it.saksnummer }
                    .distinct().size == 1,
            ) { "Alle meldekortvedtakene må være for samme saksnummer. ${verdi.map { it.id to it.saksnummer }}" }

            require(
                verdi.map { it.sakId }
                    .distinct().size == 1,
            ) { "Alle meldekortvedtakene må være for samme sak-id. ${verdi.map { it.id to it.sakId }}" }

            verdi.mapNotNull { it.journalpostId }.let { journalpostIds ->
                require(journalpostIds.size == journalpostIds.distinct().size) {
                    "Alle meldekortvedtakene må ha unik journalpostId. ${verdi.map { it.id to it.journalpostId }}"
                }
            }

            require(
                verdi.zipWithNext()
                    .all { (a, b) -> a.opprettet < b.opprettet },
            ) { "Meldekortvedtakene må være sortert på opprettet, men var ${verdi.map { it.id to it.opprettet }}" }

            require(
                verdi.map { it.id }
                    .distinct() == verdi.map { it.id },
            ) { "Alle meldekortvedtakene må ha unik id. ${verdi.map { it.id }}" }
        }
    }

    fun hentForMeldekortBehandling(id: MeldekortId): MeldekortVedtak? {
        return verdi.find { it.meldekortId == id }
    }

    fun leggTil(meldekortVedtak: MeldekortVedtak): MeldekortVedtaksliste {
        return MeldekortVedtaksliste(verdi + meldekortVedtak)
    }

    companion object {
        fun empty(): MeldekortVedtaksliste {
            return MeldekortVedtaksliste(emptyList())
        }
    }
}
