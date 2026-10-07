package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingType

/**
 * En endring i tiltaksdeltakelsen, vurdert mot de gjeldende vedtakene på saken.
 * @param automatiskRevurdering Revurderingen som kan opprettes automatisk som følge av endringen.
 * Er den null, må endringen følges opp manuelt.
 */
data class VurdertTiltaksdeltakerEndring(
    val endring: TiltaksdeltakerEndring,
    val automatiskRevurdering: AutomatiskRevurderingAvEndring?,
)

/**
 * Revurderingen som kan opprettes automatisk for en endret tiltaksdeltakelse, med én implementasjon per revurderingstype.
 * Hver type får på sikt egne attributter når mer av saksbehandlingen automatiseres.
 */
sealed interface AutomatiskRevurderingAvEndring {
    val type: StartRevurderingType

    data object Stans : AutomatiskRevurderingAvEndring {
        override val type = StartRevurderingType.STANS
    }

    data object Innvilgelse : AutomatiskRevurderingAvEndring {
        override val type = StartRevurderingType.INNVILGELSE
    }

    data class Omgjøring(val vedtakIdSomOmgjøres: VedtakId) : AutomatiskRevurderingAvEndring {
        override val type = StartRevurderingType.OMGJØRING
    }
}
