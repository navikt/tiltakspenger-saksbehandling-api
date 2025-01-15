package no.nav.tiltakspenger.saksbehandling.domene.behandling

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.norskDatoMedPunktumFormatter

data class TilleggstekstBrev(
    val subsumsjon: Subsumsjon,
    val tekst: String,
) {
    enum class Subsumsjon {
        TILTAKSDELTAGELSE,
    }

    companion object {
        fun hentStandardTekstForBegrunnelse(
            subsumsjon: Subsumsjon,
            søknadsperiode: Periode,
            vurderingsperiode: Periode,
        ): String {
            when (subsumsjon) {
                Subsumsjon.TILTAKSDELTAGELSE -> return tiltaksdeltagelse(søknadsperiode, vurderingsperiode)
            }
        }

        fun tiltaksdeltagelse(søknadsperiode: Periode, vedtaksperiode: Periode): String {
            val vedtakFraOgMed = vedtaksperiode.fraOgMed.format(norskDatoMedPunktumFormatter)
            val vedtakTilOgMed = vedtaksperiode.tilOgMed.format(norskDatoMedPunktumFormatter)

            return """
            Du har søkt tiltakspenger ${søknadsperiode.tilNorskFormat()}. Din første dag på tiltak er $vedtakFraOgMed.
            Du har derfor rett til tiltakspenger fra og med $vedtakFraOgMed til og med $vedtakTilOgMed.
            """.trimIndent()
        }
    }
}
