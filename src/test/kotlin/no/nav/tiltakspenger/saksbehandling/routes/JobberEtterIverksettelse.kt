package no.nav.tiltakspenger.saksbehandling.routes

/**
 * Styrer hvilke av jobbene som følger etter en iverksettelse route-byggeren skal kjøre.
 *
 * I prod går jobbene på et cron-intervall en stund etter iverksettelsen, så vedtaket ligger i den aktuelle køen i mellomtiden.
 * Byggerne kjører dem med én gang som en bekvemmelighet, slik at de fleste tester slipper å tenke på dem.
 * En test som skal observere en av køene må slå av jobben som tømmer den — ellers er køen alltid tom når testen ser på den.
 */
data class JobberEtterIverksettelse(
    val sendUtbetalinger: Boolean = true,
    val oppdaterUtbetalingsstatus: Boolean = true,
    val journalførVedtaksbrev: Boolean = true,
    /**
     * Kun rammevedtaksbrev distribueres av en egen jobb.
     * Meldekortvedtak har ingen distribusjonsjobb, så byggeren for meldekortbehandling ser bort fra feltet.
     */
    val distribuerVedtaksbrev: Boolean = true,
) {
    companion object {
        /**
         * Ingen jobber kjøres.
         * Tilstanden blir liggende slik iverksettelsen etterlot den.
         */
        val ingen = JobberEtterIverksettelse(
            sendUtbetalinger = false,
            oppdaterUtbetalingsstatus = false,
            journalførVedtaksbrev = false,
            distribuerVedtaksbrev = false,
        )
    }
}
