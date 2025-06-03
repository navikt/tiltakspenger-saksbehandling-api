package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow

data class Utbetalinger(
    val verdi: List<Utbetalingsvedtak>,
) : List<Utbetalingsvedtak> by verdi {

    fun hentUtbetalingForBehandlingId(id: MeldekortId): Utbetalingsvedtak? {
        return verdi.singleOrNullOrThrow { it.meldekortbehandling.id == id }
    }

    fun leggTil(utbetalingsvedtak: Utbetalingsvedtak): Utbetalinger {
        return Utbetalinger(verdi + utbetalingsvedtak)
    }

    fun hentUtbetalingerFraPeriode(periode: Periode): List<Utbetalingsvedtak> {
        return verdi.filter { periode.overlapperMed(it.periode) }
    }

    init {
        if (verdi.isNotEmpty()) {
            require(
                verdi.map { it.fnr }
                    .distinct().size == 1,
            ) { "Alle utbetalingsvedtakene må være for samme person. ${verdi.map { it.id }}" }
            require(
                verdi.map { it.saksnummer }
                    .distinct().size == 1,
            ) { "Alle utbetalingsvedtakene må være for samme sak. ${verdi.map { it.id to it.saksnummer }}" }
            require(
                verdi.map { it.sakId }
                    .distinct().size == 1,
            ) { "Alle utbetalingsvedtakene må være for samme sak. ${verdi.map { it.id to it.sakId }}" }
            verdi.mapNotNull { it.journalpostId }.let { journalpostIds ->
                require(journalpostIds.size == journalpostIds.distinct().size) { "Alle utbetalingsvedtakene må ha unik journalpostId. ${verdi.map { it.id to it.journalpostId }}" }
            }
            require(
                verdi.zipWithNext()
                    .all { (a, b) -> a.opprettet < b.opprettet },
            ) { "Utbetalingsvedtakene må være sortert på opprettet, men var ${verdi.map { it.id to it.opprettet }}" }
            require(
                verdi.zipWithNext()
                    .all { (a, b) -> a.id == b.forrigeUtbetalingsvedtakId },
            ) { "Utbetalingsvedtakene må være lenket, men var ${verdi.map { it.id to it.forrigeUtbetalingsvedtakId }}" }
            require(verdi.first().forrigeUtbetalingsvedtakId == null) { "Første utbetalingsvedtak.forrigeUtbetalingsvedtakId må være null, men var ${verdi.first().forrigeUtbetalingsvedtakId}" }
            require(verdi.map { it.id }.distinct() == verdi.map { it.id }) { "Alle utbetalingsvedtakene må ha unik id. ${verdi.map { it.id }}" }
        }
    }
}
