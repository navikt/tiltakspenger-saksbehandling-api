package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV1
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV2
import no.nav.tiltakspenger.saksbehandling.barnetillegg.UgyldigBarnetillegg
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse

/**
 * Barnetillegget saksbehandler har valgt, før det er validert mot innvilgelsesperiodene.
 */
sealed interface ValgtBarnetillegg {
    val begrunnelse: Begrunnelse?

    fun tilBarnetillegg(innvilgelsesperioder: NonEmptyList<Periode>): Either<UgyldigBarnetillegg, Barnetillegg>

    /**
     * Barna det skal gis barnetillegg for.
     * Tom liste betyr ingen barnetillegg.
     */
    data class Barn(
        val barn: List<BarnMedBarnetillegg>,
        override val begrunnelse: Begrunnelse?,
    ) : ValgtBarnetillegg {
        override fun tilBarnetillegg(innvilgelsesperioder: NonEmptyList<Periode>): Either<UgyldigBarnetillegg, BarnetilleggV2> =
            BarnetilleggV2.create(
                barn = barn,
                innvilgelsesperioder = innvilgelsesperioder,
                begrunnelse = begrunnelse,
            )
    }

    /**
     * Antall barn per periode, slik eldre versjoner av frontenden sender det.
     * Gir [BarnetilleggV1].
     * Tom liste betyr ingen barnetillegg, og gir et [BarnetilleggV2] uten barn, som frontenden som sender barna.
     *
     * TODO: Fjernes når alle saksbehandlere har frontenden som sender barna.
     */
    data class PerioderMedAntallBarn(
        val perioderMedBarn: List<Pair<Periode, AntallBarn>>,
        override val begrunnelse: Begrunnelse?,
    ) : ValgtBarnetillegg {
        override fun tilBarnetillegg(innvilgelsesperioder: NonEmptyList<Periode>): Either<UgyldigBarnetillegg, Barnetillegg> {
            val perioderMedBarn = perioderMedBarn.toNonEmptyListOrNull()
                ?: return Barn(barn = emptyList(), begrunnelse = begrunnelse).tilBarnetillegg(innvilgelsesperioder)

            return BarnetilleggV1.create(
                perioderMedBarn = perioderMedBarn,
                innvilgelsesperioder = innvilgelsesperioder,
                begrunnelse = begrunnelse,
            )
        }
    }
}
