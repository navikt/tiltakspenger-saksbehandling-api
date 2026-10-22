package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import no.nav.tiltakspenger.libs.common.VedtakId

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
 * Revurderingen som kan opprettes automatisk for en endret tiltaksdeltakelse.
 * Hver type får på sikt egne attributter når mer av saksbehandlingen automatiseres.
 */
sealed interface AutomatiskRevurderingAvEndring {
    /**
     * Avbruddet gir en stans eller et opphør, som avgjøres ut fra simuleringen når revurderingen opprettes.
     * En stans opprettes under automatisk behandling, og verdiene for å fylle den ut utledes når den behandles.
     * Et opphør fylles ut når det opprettes, og overlates til en saksbehandler.
     * Se [no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.DelautomatiskStansService.opprettStansEllerOpphør].
     */
    data object StansEllerOpphør : AutomatiskRevurderingAvEndring

    data object Innvilgelse : AutomatiskRevurderingAvEndring

    data class Omgjøring(val vedtakIdSomOmgjøres: VedtakId) : AutomatiskRevurderingAvEndring

    /**
     * Sluttdatoen er avkortet, se [TiltaksdeltakerEndring.Avkortet].
     * Vedtaket omgjøres slik at deltakelsen er innvilget til og med ny sluttdato, og resten av vedtaksperioden opphøres.
     * Omgjøringen opprettes under automatisk behandling, og verdiene for å fylle den ut utledes når den behandles.
     * Se [no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.DelautomatiskOmgjøringService].
     */
    data class OmgjøringAvAvkortetSluttdato(val vedtakIdSomOmgjøres: VedtakId) : AutomatiskRevurderingAvEndring
}
