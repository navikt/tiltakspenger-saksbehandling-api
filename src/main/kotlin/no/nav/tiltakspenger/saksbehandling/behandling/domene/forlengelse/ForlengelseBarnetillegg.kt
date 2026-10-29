package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.periodiserAntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.LocalDate

/**
 * Utleder antall barn med barnetillegg i [innvilgelsesperiode].
 *
 * Hadde det gjeldende vedtaket ikke barnetillegg på [sisteInnvilgedeDag] ([antallBarnIVedtaket] er 0), får heller ikke forlengelsen barnetillegg.
 * Nye barn krever en ny søknad, så det er ikke noe å vurdere.
 *
 * Ellers periodiseres barnetillegget ut fra barna i søknaden som ligger til grunn for innvilgelsen av [tiltaksdeltakerId], se [periodiserAntallBarn].
 * Slik faller barnetillegget bort for barn som fyller 16 år i forlengelsen, på samme måte som når saksbehandler forhåndsutfyller fra søknaden.
 * Søknaden må gi like mange barn som det gjeldende vedtaket på [sisteInnvilgedeDag], slik at vi vet at det er de samme barna som er innvilget.
 * Har saksbehandler for eksempel innvilget barnetillegg for et barn utenfor EØS, eller lagt til et barn, overlates forlengelsen til saksbehandler.
 * Det samme gjelder om søknaden gir flere barn i forlengelsen enn i vedtaket, siden et nytt barn må vurderes av saksbehandler.
 */
fun Sak.utledAntallBarn(
    tiltaksdeltakerId: TiltaksdeltakerId,
    sisteInnvilgedeDag: LocalDate,
    antallBarnIVedtaket: AntallBarn,
    innvilgelsesperiode: Periode,
): Either<ManueltBehandlesGrunn, SammenhengendePeriodisering<AntallBarn>> {
    if (antallBarnIVedtaket == AntallBarn.ZERO) {
        return SammenhengendePeriodisering(AntallBarn.ZERO, innvilgelsesperiode).right()
    }

    val barnFraSøknaden = rammevedtaksliste
        .filter { it.rammebehandling is Søknadsbehandling && it.rammebehandlingsresultat is Søknadsbehandlingsresultat.Innvilgelse }
        .filter { vedtak -> vedtak.valgteTiltaksdeltakelser!!.verdier.any { it.internDeltakelseId == tiltaksdeltakerId } }
        .maxByOrNull { it.opprettet }
        ?.let { (it.rammebehandling as Søknadsbehandling).søknad.barnetillegg }
        ?: return ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_BARNETILLEGG.left()

    val antallBarnFraSøknadenISisteInnvilgedeDag = barnFraSøknaden
        .periodiserAntallBarn(sisteInnvilgedeDag til sisteInnvilgedeDag)
        .verdier
        .single()
    if (antallBarnFraSøknadenISisteInnvilgedeDag != antallBarnIVedtaket) {
        return ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_BARNETILLEGG.left()
    }

    val antallBarn = barnFraSøknaden.periodiserAntallBarn(innvilgelsesperiode)
    if (antallBarn.verdier.any { it.value > antallBarnIVedtaket.value }) {
        return ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_BARNETILLEGG.left()
    }

    return antallBarn.right()
}

/**
 * Begrunnelsen for barnetillegget.
 * Er null når forlengelsen ikke har barnetillegg, enten fordi det gjeldende vedtaket ikke hadde det, eller fordi det faller helt bort.
 */
fun lagBegrunnelseBarnetillegg(
    antallBarnIVedtaket: AntallBarn,
    antallBarn: SammenhengendePeriodisering<AntallBarn>,
): Begrunnelse? = when {
    antallBarn.inneholderKun(AntallBarn.ZERO) -> null

    antallBarn.inneholderKun(antallBarnIVedtaket) -> Begrunnelse.create(
        "Barnetillegg for ${antallBarnIVedtaket.value} barn videreføres fra det gjeldende vedtaket.",
    )

    else -> Begrunnelse.create(
        "Barnetillegget for ${antallBarnIVedtaket.value} barn i det gjeldende vedtaket reduseres når barn fyller 16 år:" +
            antallBarn.perioderMedVerdi.joinToString("") { (antall, periode) ->
                "\n${antall.value} barn fra og med ${periode.fraOgMed.format(norskDatoFormatter)} " +
                    "til og med ${periode.tilOgMed.format(norskDatoFormatter)}."
            },
    )
}
