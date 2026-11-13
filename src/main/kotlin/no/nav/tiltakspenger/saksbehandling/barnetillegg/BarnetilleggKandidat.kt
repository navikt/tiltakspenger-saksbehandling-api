package no.nav.tiltakspenger.saksbehandling.barnetillegg

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/**
 * Et barn saksbehandler kan velge å gi barnetillegg for i en behandling, se [barnetilleggKandidater].
 *
 * @param barn Barnet med barnetillegg for hele innvilgelsesperioden.
 * @param søknadsopplysninger Opplysningene om barnet fra den nyeste søknaden barnet finnes i, eller null om barnet ikke finnes i en søknad.
 * @param tiltaksdeltakerIder Barnet er bare aktuelt når det innvilges for en av disse tiltaksdeltakelsene.
 * Null når barnet er aktuelt uansett hvilke tiltaksdeltakelser det innvilges for.
 * Det gjelder barn som er lagret på behandlingen, barn fra gjeldende vedtak og barn fra søknaden i en søknadsbehandling.
 * @param fraGjeldendeVedtak Barnet gir barnetillegg i et gjeldende vedtak.
 */
data class BarnetilleggKandidat(
    val barn: BarnMedBarnetillegg,
    val søknadsopplysninger: BarnetilleggFraSøknad?,
    val tiltaksdeltakerIder: Set<TiltaksdeltakerId>?,
    val fraGjeldendeVedtak: Boolean,
) {
    fun erSammeBarn(other: BarnetilleggKandidat): Boolean = barn.erSammeBarn(other.barn)

    /** Slår sammen to kandidater for samme barn, der barnet fra denne kandidaten beholdes. */
    fun slåSammen(other: BarnetilleggKandidat): BarnetilleggKandidat = copy(
        søknadsopplysninger = søknadsopplysninger ?: other.søknadsopplysninger,
        tiltaksdeltakerIder = if (tiltaksdeltakerIder == null || other.tiltaksdeltakerIder == null) {
            null
        } else {
            tiltaksdeltakerIder + other.tiltaksdeltakerIder
        },
        fraGjeldendeVedtak = fraGjeldendeVedtak || other.fraGjeldendeVedtak,
    )
}

/**
 * Barna saksbehandler kan velge å gi barnetillegg for i [behandling], sortert med eldste først.
 * Barn som finnes i flere kilder tas med én gang, og opplysningene slås sammen.
 * Barnet fra den første kilden beholdes, i denne rekkefølgen:
 * 1. Barn fra søknader som er lagret på behandlingen, slik at de kan kjennes igjen.
 * 2. Barn fra søknaden(e), nyeste søknad først.
 * For søknadsbehandlinger er det søknaden som behandles.
 * Ellers er det søknadene fra innvilgede søknadsbehandlinger, med tiltaksdeltakelsene de ble innvilget for.
 * 3. Barn fra gjeldende vedtak med barn, nyeste vedtak først, for revurderinger.
 * Barn lagt til av saksbehandler er ikke med, siden de bare er aktuelle i behandlingen de er lagt til i.
 * Søknadene og vedtakene hentes fra saken slik den er nå, også for avsluttede behandlinger.
 */
fun Sak.barnetilleggKandidater(behandling: Rammebehandling): List<BarnetilleggKandidat> {
    val lagredeBarn = (behandling.barnetillegg as? BarnetilleggV2)?.barn.orEmpty()
        .filter { it.kilde is BarnMedBarnetillegg.Kilde.Søknad }
        .map { BarnetilleggKandidat(barn = it.copy(perioder = null), søknadsopplysninger = null, tiltaksdeltakerIder = null, fraGjeldendeVedtak = false) }

    val barnFraSøknader = when (behandling) {
        is Søknadsbehandling -> behandling.søknad.barnetillegg.map {
            BarnetilleggKandidat(
                barn = it.tilBarnMedBarnetillegg(behandling.søknad.id),
                søknadsopplysninger = it,
                tiltaksdeltakerIder = null,
                fraGjeldendeVedtak = false,
            )
        }

        is Revurdering ->
            rammevedtaksliste
                .filter { it.rammebehandling is Søknadsbehandling && it.rammebehandlingsresultat is Søknadsbehandlingsresultat.Innvilgelse }
                .sortedByDescending { it.opprettet }
                .flatMap { vedtak ->
                    val søknad = (vedtak.rammebehandling as Søknadsbehandling).søknad
                    val tiltaksdeltakerIder = vedtak.valgteTiltaksdeltakelser!!.verdier.flatten().map { it.internDeltakelseId }.toSet()
                    søknad.barnetillegg.map {
                        BarnetilleggKandidat(
                            barn = it.tilBarnMedBarnetillegg(søknad.id),
                            søknadsopplysninger = it,
                            tiltaksdeltakerIder = tiltaksdeltakerIder,
                            fraGjeldendeVedtak = false,
                        )
                    }
                }
    }

    val barnFraGjeldendeVedtak = when (behandling) {
        is Søknadsbehandling -> emptyList()

        is Revurdering ->
            rammevedtaksliste.innvilgetTidslinje.verdier
                .distinctBy { it.id }
                .sortedByDescending { it.opprettet }
                .flatMap { (it.barnetillegg as? BarnetilleggV2)?.barn.orEmpty() }
                .map { BarnetilleggKandidat(barn = it.copy(perioder = null), søknadsopplysninger = null, tiltaksdeltakerIder = null, fraGjeldendeVedtak = true) }
    }

    return (lagredeBarn + barnFraSøknader + barnFraGjeldendeVedtak)
        .fold(emptyList<BarnetilleggKandidat>()) { kandidater, kandidat ->
            val index = kandidater.indexOfFirst { it.erSammeBarn(kandidat) }
            if (index == -1) {
                kandidater + kandidat
            } else {
                kandidater.toMutableList().also { it[index] = it[index].slåSammen(kandidat) }
            }
        }
        .sortedBy { it.barn.fødselsdato }
}
