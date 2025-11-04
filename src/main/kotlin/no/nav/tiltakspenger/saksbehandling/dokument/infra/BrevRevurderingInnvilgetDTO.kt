package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

private data class BrevRevurderingInnvilgetDTO(
    override val personalia: BrevPersonaliaDTO,
    override val saksnummer: String,
    override val saksbehandlerNavn: String,
    override val beslutterNavn: String?,
    override val datoForUtsending: String,
    override val tilleggstekst: String?,
    override val forhandsvisning: Boolean,
    val kontor: String,
    val fraDato: String,
    val tilDato: String,
    val harBarnetillegg: Boolean,
    /**
     Intro teksten som vises dersom [harBarnetillegg] er true - ellers vil brevet ha sin egen introtekst uten barnetillegg.
     */
    val introTekstMedBarnetillegg: String?,
    val satser: List<Any>,
) : BrevRammevedtakBaseDTO

suspend fun Rammevedtak.tilRevurderingInnvilgetBrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
    tilleggstekst: FritekstTilVedtaksbrev? = null,
): String {
    return genererRevurderingInnvilgetBrev(
        hentBrukersNavn = hentBrukersNavn,
        hentSaksbehandlersNavn = hentSaksbehandlersNavn,
        vedtaksdato = vedtaksdato,
        tilleggstekst = tilleggstekst,
        fnr = fnr,
        saksbehandlerNavIdent = saksbehandler,
        beslutterNavIdent = beslutter,
        innvilgelsesperiode = innvilgelsesperiode!!,
        saksnummer = saksnummer,
        forhåndsvisning = false,
        barnetilleggsPerioder = barnetillegg?.periodisering,
    )
}

internal suspend fun genererRevurderingInnvilgetBrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    tilleggstekst: FritekstTilVedtaksbrev?,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    saksnummer: Saksnummer,
    innvilgelsesperiode: Periode,
    vedtaksdato: LocalDate,
    barnetilleggsPerioder: Periodisering<AntallBarn>?,
    forhåndsvisning: Boolean,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }

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
        fraDato = innvilgelsesperiode.fraOgMed.format(norskDatoFormatter),
        tilDato = innvilgelsesperiode.tilOgMed.format(norskDatoFormatter),
        satser = Satser.satser.filter { it.periode.overlapperMed(innvilgelsesperiode) }.map {
            @Suppress("unused")
            object {
                val år = it.periode.fraOgMed.year
                val ordinær = it.sats
                val barnetillegg = it.satsBarnetillegg
            }
        },
        tilleggstekst = tilleggstekst?.verdi,
        forhandsvisning = forhåndsvisning,
        harBarnetillegg = barnetilleggsPerioder != null && barnetilleggsPerioder.any { it.verdi.value > 0 },
        introTekstMedBarnetillegg = barnetilleggsPerioder?.tilIntroTekst(innvilgelsesperiode),
        datoForUtsending = vedtaksdato.format(norskDatoFormatter),
    ).let { serialize(it) }
}

private fun Periodisering<AntallBarn>.tilIntroTekst(vurderingsperiode: Periode): String? {
    val perioderMedBarnetillegg = perioderMedVerdi
        .filter { it.verdi.value > 0 }

    if (perioderMedBarnetillegg.isEmpty()) {
        return null
    }

    val harBarnetilleggOverHeleInnvilgelsesperiode = perioder.all { periode -> vurderingsperiode == periode }

    val perioderMedBarnetilleggString = perioderMedBarnetillegg
        .map { periodeMedVerdi ->
            val antallBarn = periodeMedVerdi.verdi.toTekst()
            "for $antallBarn barn fra og med ${periodeMedVerdi.periode.fraOgMed.format(norskDatoFormatter)} til og med ${
                periodeMedVerdi.periode.tilOgMed.format(norskDatoFormatter)
            }"
        }.let {
            when (it.size) {
                0 -> throw IllegalStateException("Skal ikke være mulig å ha 0 perioder med barnetillegg her!")
                1 -> it.first()
                2 -> "${it.first()} og ${it.last()}"
                else -> it.dropLast(1).joinToString(", ").plus(" og ${it.last()}")
            }
        }

    return if (harBarnetilleggOverHeleInnvilgelsesperiode) {
        "Du får tiltakspenger og barnetillegg $perioderMedBarnetilleggString."
    } else {
        """
            Du får tiltakspenger fra og med ${vurderingsperiode.fraOgMed.format(norskDatoFormatter)} til og med ${
            vurderingsperiode.tilOgMed.format(norskDatoFormatter)
        }.
        
            Du får barnetillegg $perioderMedBarnetilleggString.
        """.trimIndent()
    }
}
