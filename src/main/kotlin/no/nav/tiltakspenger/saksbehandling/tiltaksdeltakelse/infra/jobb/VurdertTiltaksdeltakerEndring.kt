package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingType

/**
 * Utfallet av å vurdere nå-tilstanden til en tiltaksdeltakelse mot de gjeldende vedtakene på saken.
 */
sealed interface VurdertTiltaksdeltakerEndring {
    /**
     * Ingenting relevant er endret i forhold til de gjeldende vedtakene, eller endringen er allerede fanget opp av en åpen behandling.
     * Gjelder også når deltakelsen ikke er innvilget i noe gjeldende vedtak og heller ikke er med i en åpen manuell behandling.
     */
    data object IngenEndring : VurdertTiltaksdeltakerEndring

    /**
     * En relevant endring i tiltaksdeltakelsen.
     * @param automatiskRevurdering Revurderingen som kan opprettes automatisk som følge av endringen.
     * Er den null, må endringen følges opp manuelt.
     */
    data class Endret(
        val endring: TiltaksdeltakerEndring,
        val automatiskRevurdering: AutomatiskRevurderingAvEndring?,
    ) : VurdertTiltaksdeltakerEndring
}

/**
 * Revurderingen som kan opprettes automatisk for en endret tiltaksdeltakelse, med én implementasjon per revurderingstype.
 * Hver type får på sikt egne attributter når mer av saksbehandlingen automatiseres.
 */
sealed interface AutomatiskRevurderingAvEndring {
    val type: StartRevurderingType

    /**
     * Stansen opprettes under automatisk behandling, og verdiene for å fylle den ut utledes når den behandles.
     */
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
