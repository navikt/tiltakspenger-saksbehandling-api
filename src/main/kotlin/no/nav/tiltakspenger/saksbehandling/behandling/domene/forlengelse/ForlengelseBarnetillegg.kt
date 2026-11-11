package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV1
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV2
import no.nav.tiltakspenger.saksbehandling.barnetillegg.harBarnSomFårRettI
import no.nav.tiltakspenger.saksbehandling.barnetillegg.periodiserAntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.tilBarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.LocalDate

/**
 * Utleder barnetillegget i forlengelsen [innvilgelsesperiode] fra det gjeldende barnetillegget på [sisteInnvilgedeDag].
 *
 * Er det gjeldende barnetillegget [BarnetilleggV2], videreføres barna som har barnetillegg på [sisteInnvilgedeDag], for hele forlengelsen.
 * Barnetillegget avkortes automatisk for barn som fyller 16 år i forlengelsen, og barn som allerede har fylt 16 år, tas ikke med.
 * Barn saksbehandler har lagt til selv, videreføres også, siden vi vet hvilke barn som er innvilget.
 * Blir et barn i søknaden født i forlengelsen uten å være valgt, overlates forlengelsen til saksbehandler, siden et nytt barn må vurderes.
 *
 * Er det [BarnetilleggV1], vet vi bare antallet, og det utledes fra søknaden som før, se [utledAntallBarn].
 * Uten barnetillegg på [sisteInnvilgedeDag] får heller ikke forlengelsen barnetillegg.
 *
 * Begrunnelsen lages med [lagBegrunnelseBarnetillegg].
 */
fun Sak.utledBarnetilleggForForlengelse(
    tiltaksdeltakerId: TiltaksdeltakerId,
    sisteInnvilgedeDag: LocalDate,
    innvilgelsesperiode: Periode,
): Either<ManueltBehandlesGrunn, Barnetillegg> {
    val antallBarnIVedtaket = rammevedtaksliste.barnetilleggsperioder.hentVerdiForDag(sisteInnvilgedeDag) ?: AntallBarn.ZERO
    if (antallBarnIVedtaket == AntallBarn.ZERO) {
        return BarnetilleggV2.utenBarnetillegg(nonEmptyListOf(innvilgelsesperiode)).right()
    }

    val gjeldendeBarnetillegg = rammevedtaksliste.innvilgetTidslinje.hentVerdiForDag(sisteInnvilgedeDag)?.barnetillegg
    if (gjeldendeBarnetillegg is BarnetilleggV2) {
        val søknaden = søknadenSomErInnvilget(tiltaksdeltakerId)
        val ikkeValgteBarnFraSøknaden = søknaden?.barnetillegg.orEmpty()
            .filter { barn -> gjeldendeBarnetillegg.barn.none { it.erSammeBarn(barn.tilBarnMedBarnetillegg(søknaden!!.id)) } }
        if (ikkeValgteBarnFraSøknaden.harBarnSomFårRettI(innvilgelsesperiode)) {
            return ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_BARNETILLEGG.left()
        }

        val barn = gjeldendeBarnetillegg.barn
            .filter { barn -> gjeldendeBarnetillegg.perioderMedBarnetillegg(barn).any { it.inneholder(sisteInnvilgedeDag) } }
            .filter { it.periodeMedRett.overlapperMed(innvilgelsesperiode) }
            .map { it.copy(perioder = null) }
        val barnetillegg = BarnetilleggV2(
            barn = barn,
            innvilgelsesperioder = nonEmptyListOf(innvilgelsesperiode),
            begrunnelse = null,
        )
        return barnetillegg.copy(
            begrunnelse = lagBegrunnelseBarnetillegg(
                antallBarnIVedtaket = antallBarnIVedtaket,
                antallBarn = barnetillegg.periodisering as SammenhengendePeriodisering,
            ),
        ).right()
    }

    return utledAntallBarn(
        tiltaksdeltakerId = tiltaksdeltakerId,
        sisteInnvilgedeDag = sisteInnvilgedeDag,
        antallBarnIVedtaket = antallBarnIVedtaket,
        innvilgelsesperiode = innvilgelsesperiode,
    ).map { antallBarn ->
        BarnetilleggV1(
            periodisering = antallBarn,
            begrunnelse = lagBegrunnelseBarnetillegg(antallBarnIVedtaket = antallBarnIVedtaket, antallBarn = antallBarn),
        )
    }
}

/**
 * Utleder antall barn med barnetillegg i [innvilgelsesperiode], når det gjeldende barnetillegget er [BarnetilleggV1].
 *
 * Hadde det gjeldende vedtaket ikke barnetillegg på [sisteInnvilgedeDag] ([antallBarnIVedtaket] er 0), får heller ikke forlengelsen barnetillegg.
 * Nye barn krever en ny søknad, så det er ikke noe å vurdere.
 *
 * Ellers periodiseres barnetillegget ut fra barna i søknaden som ligger til grunn for innvilgelsen av [tiltaksdeltakerId], se [periodiserAntallBarn].
 * Slik faller barnetillegget bort for barn som fyller 16 år i forlengelsen, på samme måte som når saksbehandler forhåndsutfyller fra søknaden.
 * Søknaden må gi like mange barn som det gjeldende vedtaket på [sisteInnvilgedeDag], slik at vi vet at det er de samme barna som er innvilget.
 * Har saksbehandler for eksempel innvilget barnetillegg for et barn utenfor EØS, eller lagt til et barn, overlates forlengelsen til saksbehandler.
 * Det samme gjelder om et barn i søknaden blir født i forlengelsen, siden et nytt barn må vurderes av saksbehandler.
 * Det gjelder også når et annet barn fyller 16 år samtidig, slik at antallet barn ikke øker.
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

    val barnFraSøknaden = søknadenSomErInnvilget(tiltaksdeltakerId)?.barnetillegg
        ?: return ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_BARNETILLEGG.left()

    val antallBarnFraSøknadenISisteInnvilgedeDag = barnFraSøknaden
        .periodiserAntallBarn(sisteInnvilgedeDag til sisteInnvilgedeDag)
        .verdier
        .single()
    if (antallBarnFraSøknadenISisteInnvilgedeDag != antallBarnIVedtaket) {
        return ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_BARNETILLEGG.left()
    }

    if (barnFraSøknaden.harBarnSomFårRettI(innvilgelsesperiode)) {
        return ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_BARNETILLEGG.left()
    }

    return barnFraSøknaden.periodiserAntallBarn(innvilgelsesperiode).right()
}

/** Søknaden fra den siste innvilgede søknadsbehandlingen av [tiltaksdeltakerId], eller null om den ikke finnes. */
private fun Sak.søknadenSomErInnvilget(tiltaksdeltakerId: TiltaksdeltakerId): Søknad? = rammevedtaksliste
    .filter { it.rammebehandling is Søknadsbehandling && it.rammebehandlingsresultat is Søknadsbehandlingsresultat.Innvilgelse }
    .filter { vedtak -> vedtak.valgteTiltaksdeltakelser!!.verdier.flatten().any { it.internDeltakelseId == tiltaksdeltakerId } }
    .maxByOrNull { it.opprettet }
    ?.let { (it.rammebehandling as Søknadsbehandling).søknad }

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
