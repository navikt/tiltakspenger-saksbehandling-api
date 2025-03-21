package no.nav.tiltakspenger.saksbehandling.saksbehandling.service.statistikk.stønad

import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vedtak.Rammevedtak
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Dette må sees på som en rammesak for stønadstatistikk der vi fyller på med utbetalingsinformasjon i tillegg.
 * TODO statistikk jah: Hør med stønadsstatistikk om vi kun skal sende denne for førstegangsbehandlingen, eller om de trenger noe nytt ved revurdering.
 */
fun genererStønadsstatistikkForRammevedtak(
    vedtak: Rammevedtak,
): StatistikkStønadDTO {
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    return StatistikkStønadDTO(
        id = UUID.randomUUID(),
        brukerId = vedtak.fnr.verdi,

        sakId = vedtak.sakId.toString(),
        saksnummer = vedtak.saksnummer.toString(),
        // vår sak har ikke resultat, så bruker vedtak sin resultat
        resultat = vedtak.vedtaksType.navn,
        sakDato = vedtak.saksnummer.dato,
        // sak har ikke periode lengre, så bruker vedtak sin periode
        sakFraDato = vedtak.periode.fraOgMed,
        sakTilDato = vedtak.periode.tilOgMed,
        ytelse = "IND",

        søknadId = vedtak.behandling.søknad?.id?.toString(),
        opplysning = "",
        søknadDato = vedtak.behandling.søknad?.opprettet?.toLocalDate(),
        søknadFraDato = vedtak.behandling.søknad?.tiltak?.deltakelseFom,
        søknadTilDato = vedtak.behandling.søknad?.tiltak?.deltakelseTom,

        vedtakId = vedtak.id.toString(),
        vedtaksType = "Ny Rettighet",
        // TODO post-mvp: Denne skal kanskje egentlig være datoen fra brevet. Ta en prat med statistikk.
        vedtakDato = vedtak.opprettet.toLocalDate(),
        vedtakFom = vedtak.periode.fraOgMed,
        vedtakTom = vedtak.periode.tilOgMed,
    )
}
