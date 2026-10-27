package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.KanIkkeForlenge
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDate

/**
 * Verdiene en forlengelse lagres og beregnes med, utledet fra ny til og med-dato og vedtaket som forlenges.
 */
data class UtledetForlengelse(
    val innvilgelsesperioder: IkkeTomPeriodisering<InnvilgelsesperiodeKommando>,
    val barnetillegg: Barnetillegg,
    val kanIkkeUtledeBarnetillegg: Boolean,
) {
    val forlengelsesperiode: Periode = innvilgelsesperioder.totalPeriode
}

/**
 * Utleder innvilgelsesperioden og barnetillegget til forlengelsen [revurdering] når saksbehandler har valgt [tilOgMed].
 *
 * Innvilgelsesperioden går fra dagen etter vedtaket som forlenges, til og med [tilOgMed].
 * Tiltaksdeltakelsen og antall dager per meldeperiode videreføres fra siste innvilgede dag i vedtaket.
 * Forlengelsen kan ikke overlappe andre vedtak, og tiltaksdeltakelsen i saksopplysningene må dekke hele forlengelsen.
 *
 * @param barnetillegg Barnetillegget saksbehandler har valgt.
 * Er det null, utledes det fra vedtaket som forlenges, se [utledAntallBarn].
 * Kan det ikke utledes, videreføres antall barn i vedtaket uendret, og saksbehandler må vurdere barnetillegget selv.
 */
fun Sak.utledForlengelse(
    revurdering: Revurdering,
    tilOgMed: LocalDate,
    barnetillegg: ValgtBarnetillegg?,
): Either<KanIkkeForlenge, UtledetForlengelse> {
    val resultat = revurdering.resultat
    require(resultat is Revurderingsresultat.Forlengelse) {
        "Behandlingen ${revurdering.id} er ikke en forlengelse, men ${resultat.javaClass.simpleName}"
    }
    val forlenger = resultat.forlenger
    val vedtak = hentRammevedtakForId(forlenger.rammevedtakId)

    if (!rammevedtaksliste.kanFortsattForlenges(forlenger)) {
        return KanIkkeForlenge.VedtaketKanIkkeLengerForlenges.left()
    }

    if (!tilOgMed.isAfter(forlenger.sisteInnvilgedeDag)) {
        return KanIkkeForlenge.TilOgMedMåVæreEtterVedtaket(forlenger.sisteInnvilgedeDag).left()
    }

    val forlengelsesperiode = forlenger.forlengesFraOgMed til tilOgMed
    if (rammevedtaksliste.tidslinje.overlapper(forlengelsesperiode)) {
        return KanIkkeForlenge.OverlapperAndreVedtak(forlengelsesperiode).left()
    }

    val tiltaksdeltakerId = vedtak.valgteTiltaksdeltakelser!!.hentVerdiForDag(forlenger.sisteInnvilgedeDag)!!.internDeltakelseId
    val deltakelsesperiode = revurdering.saksopplysninger.getTiltaksdeltakelse(tiltaksdeltakerId)?.periode
    if (deltakelsesperiode == null || !deltakelsesperiode.inneholderHele(forlengelsesperiode)) {
        return KanIkkeForlenge.TiltaksdeltakelsenDekkerIkkeForlengelsen(forlengelsesperiode).left()
    }

    if (barnetillegg != null && !barnetillegg.erInnenfor(forlengelsesperiode)) {
        return KanIkkeForlenge.BarnetilleggetGårUtoverForlengelsen(forlengelsesperiode).left()
    }

    val antallBarnIVedtaket = rammevedtaksliste.barnetilleggsperioder.hentVerdiForDag(forlenger.sisteInnvilgedeDag) ?: AntallBarn.ZERO
    val utledetAntallBarn = utledAntallBarn(
        tiltaksdeltakerId = tiltaksdeltakerId,
        sisteInnvilgedeDag = forlenger.sisteInnvilgedeDag,
        antallBarnIVedtaket = antallBarnIVedtaket,
        innvilgelsesperiode = forlengelsesperiode,
    )

    return UtledetForlengelse(
        innvilgelsesperioder = SammenhengendePeriodisering(
            InnvilgelsesperiodeKommando(
                periode = forlengelsesperiode,
                antallDagerPerMeldeperiode = vedtak.antallDagerPerMeldeperiode!!.hentVerdiForDag(forlenger.sisteInnvilgedeDag)!!,
                internDeltakelseId = tiltaksdeltakerId,
            ),
            forlengelsesperiode,
        ),
        barnetillegg = barnetillegg?.tilBarnetillegg(forlengelsesperiode) ?: utledetAntallBarn
            .getOrElse { SammenhengendePeriodisering(antallBarnIVedtaket, forlengelsesperiode) }
            .let { antallBarn ->
                Barnetillegg(
                    periodisering = antallBarn,
                    begrunnelse = lagBegrunnelseBarnetillegg(antallBarnIVedtaket = antallBarnIVedtaket, antallBarn = antallBarn),
                )
            },
        kanIkkeUtledeBarnetillegg = utledetAntallBarn.isLeft(),
    ).right()
}
