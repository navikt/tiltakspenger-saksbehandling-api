package no.nav.tiltakspenger.saksbehandling.klage.domene

import arrow.core.NonEmptyCollection
import arrow.core.NonEmptySet
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Klagehjemmel

/**
 * https://github.com/navikt/klage-kodeverk/blob/main/src/main/kotlin/no/nav/klage/kodeverk/hjemmel/YtelseToRegistreringshjemlerV2.kt#L2938
 */
data class Klagehjemler(
    val verdi: NonEmptySet<Klagehjemmel>,
) : NonEmptyCollection<Klagehjemmel> by verdi,
    Set<Klagehjemmel>
