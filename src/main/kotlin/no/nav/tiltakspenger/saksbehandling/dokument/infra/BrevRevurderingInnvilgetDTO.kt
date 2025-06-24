package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Satser

private data class BrevRevurderingInnvilgetDTO(
    val personalia: BrevPersonaliaDTO,
    val saksnummer: String,
    val saksbehandlerNavn: String,
    val beslutterNavn: String?,
    val kontor: String,
    val fraDato: String,
    val tilDato: String,
    val harBarnetillegg: Boolean,
    /**
     Intro teksten som vises dersom [harBarnetillegg] er true - ellers vil brevet ha sin egen introtekst uten barnetillegg.
     */
    val introTekstMedBarnetillegg: String?,
    val satser: List<Any>,
    val saksbehandlerVurdering: String?,
    val forhåndsvisning: Boolean,
)

internal suspend fun genererRevurderingInnvilgetBrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    saksbehandlersVurdering: FritekstTilVedtaksbrev,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    saksnummer: Saksnummer,
    vurderingsperiode: Periode,
    barnetillegg: Periodisering<AntallBarn>?,
    forhåndsvisning: Boolean,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }

    val introTekstMedBarnetillegg = if (barnetillegg != null && barnetillegg.any { it.verdi.value > 0 }) {
        val harBarnetilleggOverHeleInnvilgelsesperiode =
            barnetillegg.perioder.map { periode -> vurderingsperiode == periode }.all { it }

        if (harBarnetilleggOverHeleInnvilgelsesperiode) {
            val antallBarn = barnetillegg.perioderMedVerdi.sumOf { it.verdi.value }.toTekst()
            """
                Du får tiltakspenger og barnetillegg for $antallBarn barn fra og med ${
                vurderingsperiode.fraOgMed.format(
                    norskDatoFormatter,
                )
            } til og med ${vurderingsperiode.tilOgMed.format(norskDatoFormatter)} fordi deltakelsen på arbeidsmarkedstiltaket er blitt forlenget.
            """.trimIndent()
        } else {
            val perioderMedBarnetillegg = barnetillegg.perioderMedVerdi.joinToString(" og ") { periodeMedVerdi ->
                val antallBarn = periodeMedVerdi.verdi.toTekst()
                "$antallBarn barn fra og med ${periodeMedVerdi.periode.fraOgMed.format(norskDatoFormatter)} til og med ${
                    periodeMedVerdi.periode.tilOgMed.format(norskDatoFormatter)
                }"
            }

            """
                Du får tiltakspenger fra og med ${vurderingsperiode.fraOgMed.format(norskDatoFormatter)} til og med ${
                vurderingsperiode.tilOgMed.format(
                    norskDatoFormatter,
                )
            } fordi deltakelsen på arbeidsmarkedstiltaket er blitt forlenget.
                Du får barnetillegg for $perioderMedBarnetillegg.
            """.trimIndent()
        }
    } else {
        null
    }

    return BrevRevurderingInnvilgetDTO(
        personalia = BrevPersonaliaDTO(
            ident = fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
        ),
        saksnummer = saksnummer.verdi,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        kontor = "Nav Tiltakspenger",
        fraDato = vurderingsperiode.fraOgMed.format(norskDatoFormatter),
        tilDato = vurderingsperiode.tilOgMed.format(norskDatoFormatter),
        satser = Satser.satser.filter { it.periode.overlapperMed(vurderingsperiode) }.map {
            @Suppress("unused")
            object {
                val år = it.periode.fraOgMed.year
                val ordinær = it.sats
                val barnetillegg = it.satsBarnetillegg
            }
        },
        saksbehandlerVurdering = saksbehandlersVurdering.verdi,
        forhåndsvisning = forhåndsvisning,
        harBarnetillegg = barnetillegg != null && barnetillegg.any { it.verdi.value > 0 },
        introTekstMedBarnetillegg = introTekstMedBarnetillegg,
    ).let { serialize(it) }
}
