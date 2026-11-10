package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV1
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgetTiltaksdeltakelseKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.KanIkkeForlenge
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
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
 * Utleder innvilgelsesperioden og barnetillegget til forlengelsen [revurdering] når saksbehandler har valgt [tilOgMed] og [tiltaksdeltakerId].
 *
 * Innvilgelsesperioden går fra dagen etter vedtaket som forlenges, til og med [tilOgMed].
 * Tiltaksdeltakelsen må være en av [tiltaksdeltakelserSomKanForlenges], og antall dager per meldeperiode videreføres fra den siste perioden vedtaket innvilget for den.
 * Forlengelsen kan ikke overlappe andre vedtak, og tiltaksdeltakelsen i saksopplysningene må dekke hele forlengelsen.
 *
 * @param barnetillegg Barnetillegget saksbehandler har valgt.
 * Er det null, utledes det fra vedtaket som forlenges, se [utledBarnetilleggForForlengelse].
 * Kan det ikke utledes, videreføres antall barn i vedtaket uendret, og saksbehandler må vurdere barnetillegget selv.
 */
fun Sak.utledForlengelse(
    revurdering: Revurdering,
    tilOgMed: LocalDate,
    tiltaksdeltakerId: TiltaksdeltakerId,
    barnetillegg: ValgtBarnetillegg?,
): Either<KanIkkeForlenge, UtledetForlengelse> {
    val resultat = revurdering.resultat
    require(resultat is Revurderingsresultat.Forlengelse) {
        "Behandlingen ${revurdering.id} er ikke en forlengelse, men ${resultat.javaClass.simpleName}"
    }
    val forlenger = resultat.forlenger

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

    val valgtTiltaksdeltakelse = tiltaksdeltakelserSomKanForlenges(forlenger, revurdering.saksopplysninger)
        .find { it.tiltaksdeltakelse.internDeltakelseId == tiltaksdeltakerId }
        ?: return KanIkkeForlenge.TiltaksdeltakelsenKanIkkeForlenges(tiltaksdeltakerId).left()

    val deltakelsesperiode = valgtTiltaksdeltakelse.tiltaksdeltakelse.lukketPeriode
    if (deltakelsesperiode == null || !deltakelsesperiode.inneholderHele(forlengelsesperiode)) {
        return KanIkkeForlenge.TiltaksdeltakelsenDekkerIkkeForlengelsen(forlengelsesperiode).left()
    }

    val valgtBarnetillegg = barnetillegg?.let {
        it.tilBarnetillegg(nonEmptyListOf(forlengelsesperiode)).getOrElse { feil -> return KanIkkeForlenge.UgyldigBarnetillegg(feil).left() }
    }

    val utledetBarnetillegg = utledBarnetilleggForForlengelse(
        tiltaksdeltakerId = tiltaksdeltakerId,
        sisteInnvilgedeDag = forlenger.sisteInnvilgedeDag,
        innvilgelsesperiode = forlengelsesperiode,
    )

    return UtledetForlengelse(
        innvilgelsesperioder = SammenhengendePeriodisering(
            InnvilgelsesperiodeKommando(
                periode = forlengelsesperiode,
                tiltaksdeltakelser = nonEmptyListOf(
                    InnvilgetTiltaksdeltakelseKommando(
                        internDeltakelseId = tiltaksdeltakerId,
                        antallDagerPerMeldeperiode = valgtTiltaksdeltakelse.antallDagerPerMeldeperiode,
                    ),
                ),
            ),
            forlengelsesperiode,
        ),
        barnetillegg = valgtBarnetillegg ?: utledetBarnetillegg.getOrElse {
            val antallBarnIVedtaket = rammevedtaksliste.barnetilleggsperioder.hentVerdiForDag(forlenger.sisteInnvilgedeDag) ?: AntallBarn.ZERO
            val antallBarn = SammenhengendePeriodisering(antallBarnIVedtaket, forlengelsesperiode)
            BarnetilleggV1(
                periodisering = antallBarn,
                begrunnelse = lagBegrunnelseBarnetillegg(antallBarnIVedtaket = antallBarnIVedtaket, antallBarn = antallBarn),
            )
        },
        kanIkkeUtledeBarnetillegg = utledetBarnetillegg.isLeft(),
    ).right()
}
