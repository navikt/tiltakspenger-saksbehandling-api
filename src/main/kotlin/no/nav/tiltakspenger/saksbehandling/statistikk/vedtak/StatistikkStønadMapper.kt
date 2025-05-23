package no.nav.tiltakspenger.saksbehandling.statistikk.vedtak

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.VedtakStatistikkResultat.Companion.toVedtakStatistikkResultat
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.util.UUID

/**
 * Dette må sees på som en rammesak for stønadstatistikk der vi fyller på med utbetalingsinformasjon i tillegg.
 * TODO statistikk jah: Hør med stønadsstatistikk om vi kun skal sende denne for søknadsbehandlingen, eller om de trenger noe nytt ved revurdering.
 */
fun genererStønadsstatistikkForRammevedtak(
    vedtak: Rammevedtak,
): StatistikkStønadDTO {
    val erSøknadsbehandling = vedtak.behandling is Søknadsbehandling

    val søknad = if (erSøknadsbehandling) vedtak.behandling.søknad else null
    val tiltaksdeltakelser =
        if (erSøknadsbehandling) {
            vedtak.behandling.valgteTiltaksdeltakelser?.let { valgteTiltaksdeltakelser ->
                valgteTiltaksdeltakelser.getTiltaksdeltakelser().map { it.eksternDeltagelseId }
            }
        } else {
            null
        }

    val barnetillegg = vedtak.barnetillegg?.periodisering?.mapNotNull { bt ->
        if (bt.verdi.value > 0) {
            StatistikkStønadDTO.Barnetillegg(
                fraOgMed = bt.periode.fraOgMed,
                tilOgMed = bt.periode.tilOgMed,
                antallBarn = bt.verdi.value,
            )
        } else {
            null
        }
    } ?: emptyList()
    return StatistikkStønadDTO(
        id = UUID.randomUUID(),
        brukerId = vedtak.fnr.verdi,

        sakId = vedtak.sakId.toString(),
        saksnummer = vedtak.saksnummer.toString(),
        // vår sak har ikke resultat, så bruker vedtak sin resultat
        resultat = vedtak.vedtaksType.toVedtakStatistikkResultat(),
        sakDato = vedtak.saksnummer.dato,
        // sak har ikke periode lengre, så bruker vedtak sin periode
        sakFraDato = vedtak.periode.fraOgMed,
        sakTilDato = vedtak.periode.tilOgMed,
        ytelse = "IND",

        søknadId = søknad?.id?.toString(),
        søknadDato = søknad?.opprettet?.toLocalDate(),
        søknadFraDato = søknad?.tiltak?.deltakelseFom,
        søknadTilDato = søknad?.tiltak?.deltakelseTom,

        vedtakId = vedtak.id.toString(),
        vedtaksType = "Ny Rettighet",
        // TODO post-mvp: Denne skal kanskje egentlig være datoen fra brevet. Ta en prat med statistikk.
        vedtakDato = vedtak.opprettet.toLocalDate(),
        vedtakFom = vedtak.periode.fraOgMed,
        vedtakTom = vedtak.periode.tilOgMed,
        tiltaksdeltakelser = tiltaksdeltakelser ?: emptyList(),
        barnetillegg = barnetillegg,
        harBarnetillegg = barnetillegg.isNotEmpty(),
    )
}
