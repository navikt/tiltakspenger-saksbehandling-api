package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse

/**
 * Barnetillegget saksbehandler har valgt for en forlengelse.
 * Dager i forlengelsen uten en valgt periode får 0 barn.
 */
data class ValgtBarnetillegg(
    val perioderMedBarn: List<Pair<Periode, AntallBarn>>,
    val begrunnelse: Begrunnelse?,
) {
    fun erInnenfor(forlengelsesperiode: Periode): Boolean {
        return perioderMedBarn.all { forlengelsesperiode.inneholderHele(it.first) }
    }

    fun tilBarnetillegg(forlengelsesperiode: Periode): Barnetillegg {
        val perioderMedBarn = perioderMedBarn.toNonEmptyListOrNull()
            ?: return Barnetillegg(
                periodisering = Barnetillegg.utenBarnetillegg(nonEmptyListOf(forlengelsesperiode)).periodisering,
                begrunnelse = begrunnelse,
            )

        return Barnetillegg.periodiserOgFyllUtHullMed0(
            perioderMedBarn = perioderMedBarn,
            begrunnelse = begrunnelse,
            innvilgelsesperioder = nonEmptyListOf(forlengelsesperiode),
        )
    }
}
