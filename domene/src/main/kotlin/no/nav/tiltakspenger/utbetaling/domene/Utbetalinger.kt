package no.nav.tiltakspenger.utbetaling.domene

data class Utbetalinger(
    val verdi: List<Utbetalingsvedtak>,
) : List<Utbetalingsvedtak> by verdi {
    init {
        if (verdi.isNotEmpty()) {
            require(
                verdi.map { it.fnr }
                    .distinct().size == 1,
            ) { "Alle utbetalingsvedtakene må være for samme person. ${verdi.map { it.id }}" }
            require(
                verdi.map { it.saksnummer }
                    .distinct().size == 1,
            ) { "Alle utbetalingsvedtakene må være for samme sak.  ${verdi.map { it.id to it.saksnummer }}" }
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
        }
    }
}
