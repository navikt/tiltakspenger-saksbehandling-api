package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.sladdet
import no.nav.tiltakspenger.saksbehandling.infra.route.SLADDET_TEKST
import no.nav.tiltakspenger.saksbehandling.infra.route.skalSladdeFor
import no.nav.tiltakspenger.saksbehandling.infra.route.sladdet
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.sladdet

/**
 * Sladding av [RammebehandlingDTO].
 * Fødselsdatoen i saksopplysningene, søknaden og saksbehandlers fritekster erstattes.
 * Navkontoret er stedsinformasjon om personen og sladdes sammen med resten.
 * Identer, ID-er, statuser, beløp og perioder beholdes.
 */

fun RammebehandlingDTO.sladdet(): RammebehandlingDTO = when (this) {
    is SøknadsbehandlingDTO -> this.sladdet()
    is RevurderingDTO -> this.sladdet()
}

fun SøknadsbehandlingDTO.sladdet(): SøknadsbehandlingDTO = this.copy(
    saksopplysninger = saksopplysninger.sladdet(),
    attesteringer = attesteringer.map { it.sladdet() },
    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { SLADDET_TEKST },
    begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.let { SLADDET_TEKST },
    avbrutt = avbrutt?.sladdet(),
    ventestatus = ventestatus.map { it.sladdet() },
    utbetaling = utbetaling?.sladdet(),
    resultatDTO = resultatDTO.sladdet(),
    søknad = søknad.sladdet(),
)

fun RevurderingDTO.sladdet(): RevurderingDTO = this.copy(
    saksopplysninger = saksopplysninger.sladdet(),
    attesteringer = attesteringer.map { it.sladdet() },
    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { SLADDET_TEKST },
    begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.let { SLADDET_TEKST },
    avbrutt = avbrutt?.sladdet(),
    ventestatus = ventestatus.map { it.sladdet() },
    utbetaling = utbetaling?.sladdet(),
    resultatDTO = resultatDTO.sladdet(),
)

fun SaksopplysningerDTO.sladdet(): SaksopplysningerDTO = this.copy(
    fødselsdato = SLADDET_TEKST,
)

fun BehandlingUtbetalingDTO.sladdet(): BehandlingUtbetalingDTO = this.copy(
    navkontor = SLADDET_TEKST,
    navkontorNavn = navkontorNavn?.let { SLADDET_TEKST },
)

/**
 * [SøknadsbehandlingDTO] arver resultatgrensesnittet ved delegering og er derfor en egen variant her.
 * Den sladdes som behandling, ikke som resultat.
 */
fun SøknadsbehandlingResultatDTO.sladdet(): SøknadsbehandlingResultatDTO = when (this) {
    is SøknadsbehandlingResultatDTO.Innvilgelse -> this.copy(barnetillegg = barnetillegg?.sladdet())
    is SøknadsbehandlingResultatDTO.Avslag -> this
    SøknadsbehandlingResultatDTO.IkkeValgt -> this
    is SøknadsbehandlingDTO -> this.sladdet()
}

/**
 * [RevurderingDTO] arver resultatgrensesnittet ved delegering og er derfor en egen variant her.
 * Den sladdes som behandling, ikke som resultat.
 */
fun RevurderingResultatDTO.sladdet(): RevurderingResultatDTO = when (this) {
    is RevurderingResultatDTO.Innvilgelse -> this.copy(barnetillegg = barnetillegg?.sladdet())

    is RevurderingResultatDTO.OmgjøringInnvilgelse -> this.copy(barnetillegg = barnetillegg?.sladdet())

    is RevurderingResultatDTO.Stans,
    is RevurderingResultatDTO.OmgjøringOpphør,
    is RevurderingResultatDTO.OmgjøringIkkeValgt,
    -> this

    is RevurderingDTO -> this.sladdet()
}

fun RammebehandlingDTO.sladdetFor(saksbehandler: Saksbehandler): RammebehandlingDTO =
    if (skalSladdeFor(saksbehandler)) sladdet() else this
