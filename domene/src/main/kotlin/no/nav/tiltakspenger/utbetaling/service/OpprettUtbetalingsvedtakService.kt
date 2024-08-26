package no.nav.tiltakspenger.utbetaling.service

import arrow.core.Either
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.domene.Meldekort.UtfyltMeldekort
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.utbetaling.domene.tilUtbetalingsperiode
import no.nav.tiltakspenger.utbetaling.ports.UtbetalingsvedtakRepo

/**
 * Forventet å kalles fra en jobb.
 * Henter alle godkjente meldekort, beregner og lagrer en utbetaling.
 * Grunnen til at vi ønsker å gjøre dette i et eget steg er at vi må kunne resende utbetaling til helved dersom noe feiler og da bør vi prøve den samme behandlingId på nytt, som de deduper på.
 *
 * TODO pre-mvp jah: Diskuter med teamet: Siden saksbehandler sender inn meldekort, kan vi flytte dette inn i transaksjonen som oppretter meldekortet.
 */
class OpprettUtbetalingsvedtakService(
    private val utbetalingsvedtakRepo: UtbetalingsvedtakRepo,
    private val rammevedtakRepo: RammevedtakRepo,
) {
    val logger = KotlinLogging.logger { }

    fun opprettUtbetalingsvedtak() {
        utbetalingsvedtakRepo.hentGodkjenteMeldekortUtenUtbetalingsvedtak().forEach { meldekort: UtfyltMeldekort ->
            // Vi ønsker ikke at trøblete meldekort skal spenne ben for andre meldekort.
            Either
                .catch {
                    val rammevedtak: Rammevedtak = rammevedtakRepo.hent(meldekort.rammevedtakId)!!
                    require(rammevedtak.sakId == meldekort.sakId)
                    // TODO jah: Lag en type som passer på at disse er sortert riktig. Dette vil være en del av arbeidet å sentralisere Sak.kt
                    val eksisterendeUtbetalingsvedtak = utbetalingsvedtakRepo.hentForSakId(meldekort.sakId)
                    meldekort.tilUtbetalingsperiode(rammevedtak, eksisterendeUtbetalingsvedtak.lastOrNull()?.id).also {
                        utbetalingsvedtakRepo.lagre(it)
                    }
                }.onLeft {
                    logger.error(it) { "Feil ved opprettelse av utbetalingsvedtak for meldekortId=${meldekort.id}" }
                }
        }
    }
}
