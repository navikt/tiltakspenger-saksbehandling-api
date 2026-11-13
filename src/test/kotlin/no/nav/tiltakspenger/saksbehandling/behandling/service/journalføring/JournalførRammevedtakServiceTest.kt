package no.nav.tiltakspenger.saksbehandling.behandling.service.journalføring

import org.junit.jupiter.api.Test

/**
 * GenererFakeVedtaksbrevKlient svarer alltid "{}" og husker ikke hva den ble kalt med.
 * For å kunne sjekke tidligereInnvilgelse må faken ta vare på argumentet, slik den gjør for forlengelsesbrev.
 */
class JournalførRammevedtakServiceTest {
    @Test
    fun `avslag med grunn InnvilgetTiltakspenger får den tidligere innvilgelsen i brevet`() {
        // TODO: Bruk withTestApplicationContextAndPostgres.
        // Iverksett en innvilgelse, deretter et avslag med grunn InnvilgetTiltakspenger for samme periode.
        // Kjør journalføringsjobben.
        // Forvent at faken fikk innvilgelsesvedtaket og den innvilgede perioden som tidligereInnvilgelse.
        TODO("Ikke implementert")
    }

    @Test
    fun `revurdering etter avslaget påvirker ikke brevet`() {
        // TODO: Innvilgelse, avslag, og så en revurdering som endrer innvilgelsen, før journalføringsjobben kjører.
        // Slå av journalføringen etter iverksettelse med JobberEtterIverksettelse, så avslaget venter.
        // Forvent at tidligereInnvilgelse fortsatt er den opprinnelige innvilgelsen med hele perioden.
        TODO("Ikke implementert")
    }
}
