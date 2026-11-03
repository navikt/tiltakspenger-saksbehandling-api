package no.nav.tiltakspenger.saksbehandling.dagpenger.domene

import no.nav.tiltakspenger.libs.periode.Overlapp
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.ÅpenPeriode
import java.time.LocalDate

/**
 * En periode bruker har rett til dagpenger, slik dp-datadeling rapporterer den.
 *
 * @param tilOgMed null betyr at rettigheten fortsatt er løpende.
 */
data class Dagpengeperiode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val ytelsetype: Ytelsetype,
    val kilde: Kilde,
) {
    /**
     * En periode uten sluttdato kan overlappe med alt etter at den startet, og gir [Overlapp.Kanskje].
     * Se [ÅpenPeriode.overlapperMed].
     */
    fun overlapperMed(periode: Periode): Overlapp = ÅpenPeriode(fraOgMed, tilOgMed).overlapperMed(periode)

    enum class Ytelsetype {
        ORDINÆRE_DAGPENGER,
        PERMITTERING,
        PERMITTERING_FISKEINDUSTRI,
    }

    enum class Kilde {
        ARENA,
        DP_SAK,
    }
}
