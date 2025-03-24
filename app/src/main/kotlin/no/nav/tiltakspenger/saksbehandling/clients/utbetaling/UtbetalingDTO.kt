package no.nav.tiltakspenger.saksbehandling.clients.utbetaling

import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.felles.Navkontor
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak
import no.nav.utsjekk.kontrakter.felles.Personident
import no.nav.utsjekk.kontrakter.felles.Satstype
import no.nav.utsjekk.kontrakter.felles.StønadTypeTiltakspenger
import no.nav.utsjekk.kontrakter.iverksett.ForrigeIverksettingV2Dto
import no.nav.utsjekk.kontrakter.iverksett.IverksettV2Dto
import no.nav.utsjekk.kontrakter.iverksett.StønadsdataTiltakspengerV2Dto
import no.nav.utsjekk.kontrakter.iverksett.UtbetalingV2Dto
import no.nav.utsjekk.kontrakter.iverksett.VedtaksdetaljerV2Dto
import kotlin.collections.fold

/**
 * @param forrigeUtbetalingJson Forrige utbetaling vi sendte til helved. Siden vi må sende alle utbetalinger på nytt, må vi sende med alle utbetalinger vi har sendt tidligere.
 */
fun Utbetalingsvedtak.toDTO(
    forrigeUtbetalingJson: String?,
): String {
    val vedtak: Utbetalingsvedtak = this

    val utbetalingerStønad = meldekortbehandling.toUtbetalingDto(vedtak.brukerNavkontor, barnetillegg = false)
    val utbetalingerBarnetillegg = meldekortbehandling.toUtbetalingDto(vedtak.brukerNavkontor, barnetillegg = true)

    val nyeUtbetalinger = (utbetalingerStønad + utbetalingerBarnetillegg)

    val førsteNyeUtbetaling = nyeUtbetalinger.minByOrNull { it.fraOgMedDato }!!

    val tidligereUtbetalinger = forrigeUtbetalingJson
        ?.let { deserialize<IverksettV2Dto>(it) }
        ?.utbetalingerFør(førsteNyeUtbetaling) ?: emptyList()

    return IverksettV2Dto(
        sakId = vedtak.saksnummer.toString(),
        // Brukes som dedupliseringsnøkkel av helved dersom iverksettingId er null.
        behandlingId = vedtak.id.uuidPart(),
        // Dersom en vedtaksløsning har behov for å sende flere utbetalinger per behandling/vedtak, kan dette feltet brukes for å skille de. Denne blir brukt som delytelseId mot OS/UR. Se slack tråd: https://nav-it.slack.com/archives/C06SJTR2X3L/p1724136342664969
        iverksettingId = null,
        personident = Personident(verdi = vedtak.fnr.verdi),
        vedtak =
        VedtaksdetaljerV2Dto(
            vedtakstidspunkt = vedtak.opprettet,
            saksbehandlerId = vedtak.saksbehandler,
            beslutterId = vedtak.beslutter,
            utbetalinger = tidligereUtbetalinger + nyeUtbetalinger,
        ),
        forrigeIverksetting =
        vedtak.forrigeUtbetalingsvedtakId?.let { ForrigeIverksettingV2Dto(behandlingId = it.uuidPart()) },
    ).let {
        serialize(it)
    }
}

/** Filtrerer vekk tidligere utbetalinger, og utbetalinger på samme meldeperiodekjede ("meldekortId" her) */
private fun IverksettV2Dto.utbetalingerFør(utbetaling: UtbetalingV2Dto) =
    this.vedtak.utbetalinger.filter {
        val itStønadsdata = it.stønadsdata as StønadsdataTiltakspengerV2Dto
        val utbetalingStønadsdata = utbetaling.stønadsdata as StønadsdataTiltakspengerV2Dto
        utbetaling.fraOgMedDato > it.fraOgMedDato && utbetalingStønadsdata.meldekortId != itStønadsdata.meldekortId
    }

private fun List<MeldeperiodeBeregningDag.Utfylt>.toUtbetalingDto(
    brukersNavKontor: Navkontor,
    barnetillegg: Boolean,
) = this.fold((listOf())) { acc: List<UtbetalingV2Dto>, meldekortdag ->
    val kjedeId = meldekortdag.kjedeId
    when (val sisteUtbetalingsperiode = acc.lastOrNull()) {
        null -> {
            meldekortdag.genererUtbetalingsperiode(
                kjedeId = kjedeId,
                brukersNavKontor = brukersNavKontor,
                barnetillegg = barnetillegg,
            )?.let { acc + it } ?: acc
        }

        else ->
            sisteUtbetalingsperiode.leggTil(
                meldekortdag = meldekortdag,
                kjedeId = kjedeId,
                brukersNavKontor = brukersNavKontor,
                barnetillegg = barnetillegg,
            ).let {
                when (it) {
                    is Resultat.KanIkkeSlåSammen -> acc + it.utbetalingsperiode
                    is Resultat.KanSlåSammen -> acc.dropLast(1) + it.utbetalingsperiode
                    is Resultat.SkalIkkeUtbetales -> acc
                }
            }
    }
}

private fun MeldekortBehandling.MeldekortBehandlet.toUtbetalingDto(
    brukersNavKontor: Navkontor,
    barnetillegg: Boolean,
): List<UtbetalingV2Dto> {
    return this.beregning.dager.toUtbetalingDto(
        brukersNavKontor,
        barnetillegg,
    ) + this.beregning.dagerOmberegnet.toUtbetalingDto(brukersNavKontor, barnetillegg)
}

private fun MeldeperiodeBeregningDag.Utfylt.genererUtbetalingsperiode(
    kjedeId: MeldeperiodeKjedeId,
    brukersNavKontor: Navkontor,
    barnetillegg: Boolean,
): UtbetalingV2Dto? {
    // Vi ønsker ikke lage linjer for 0-beløp (safeguard).
    if (barnetillegg && this.beløpBarnetillegg == 0) return null
    if (!barnetillegg && this.beløp == 0) return null
    return when (this.reduksjon) {
        ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort -> null
        ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon, ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon ->
            UtbetalingV2Dto(
                beløp = (if (barnetillegg) this.beløpBarnetillegg else this.beløp).toUInt(),
                satstype = Satstype.DAGLIG_INKL_HELG,
                fraOgMedDato = this.dato,
                tilOgMedDato = this.dato,
                stønadsdata =
                StønadsdataTiltakspengerV2Dto(
                    stønadstype = this.tiltakstype!!.mapStønadstype(),
                    barnetillegg = barnetillegg,
                    brukersNavKontor = brukersNavKontor.kontornummer,
                    meldekortId = kjedeId.verdi,
                ),
            )
    }
}

private fun UtbetalingV2Dto.leggTil(
    meldekortdag: MeldeperiodeBeregningDag.Utfylt,
    kjedeId: MeldeperiodeKjedeId,
    brukersNavKontor: Navkontor,
    barnetillegg: Boolean,
): Resultat {
    val neste = meldekortdag.genererUtbetalingsperiode(kjedeId, brukersNavKontor, barnetillegg)
        ?: return Resultat.SkalIkkeUtbetales
    return if (this.kanSlåSammen(neste)) {
        Resultat.KanSlåSammen(this.copy(tilOgMedDato = neste.tilOgMedDato))
    } else {
        Resultat.KanIkkeSlåSammen(neste)
    }
}

private fun UtbetalingV2Dto.kanSlåSammen(neste: UtbetalingV2Dto): Boolean {
    return this::class == neste::class &&
        this.beløp == neste.beløp &&
        this.satstype == neste.satstype &&
        this.tilOgMedDato.plusDays(1) == neste.fraOgMedDato &&
        this.stønadsdata == neste.stønadsdata
}

private sealed interface Resultat {
    data object SkalIkkeUtbetales : Resultat
    data class KanIkkeSlåSammen(
        val utbetalingsperiode: UtbetalingV2Dto,
    ) : Resultat

    data class KanSlåSammen(
        val utbetalingsperiode: UtbetalingV2Dto,
    ) : Resultat
}

private fun TiltakstypeSomGirRett.mapStønadstype(): StønadTypeTiltakspenger =
    when (this) {
        TiltakstypeSomGirRett.ARBEIDSFORBEREDENDE_TRENING -> StønadTypeTiltakspenger.ARBEIDSFORBEREDENDE_TRENING
        TiltakstypeSomGirRett.ARBEIDSRETTET_REHABILITERING -> StønadTypeTiltakspenger.ARBEIDSRETTET_REHABILITERING
        TiltakstypeSomGirRett.ARBEIDSTRENING -> StønadTypeTiltakspenger.ARBEIDSTRENING
        TiltakstypeSomGirRett.AVKLARING -> StønadTypeTiltakspenger.AVKLARING
        TiltakstypeSomGirRett.DIGITAL_JOBBKLUBB -> StønadTypeTiltakspenger.DIGITAL_JOBBKLUBB
        TiltakstypeSomGirRett.ENKELTPLASS_AMO -> StønadTypeTiltakspenger.ENKELTPLASS_AMO
        TiltakstypeSomGirRett.ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG -> StønadTypeTiltakspenger.ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG
        TiltakstypeSomGirRett.GRUPPE_AMO -> StønadTypeTiltakspenger.GRUPPE_AMO
        TiltakstypeSomGirRett.GRUPPE_VGS_OG_HØYERE_YRKESFAG -> StønadTypeTiltakspenger.GRUPPE_VGS_OG_HØYERE_YRKESFAG
        TiltakstypeSomGirRett.HØYERE_UTDANNING -> StønadTypeTiltakspenger.HØYERE_UTDANNING
        TiltakstypeSomGirRett.INDIVIDUELL_JOBBSTØTTE -> StønadTypeTiltakspenger.INDIVIDUELL_JOBBSTØTTE
        TiltakstypeSomGirRett.INDIVIDUELL_KARRIERESTØTTE_UNG -> StønadTypeTiltakspenger.INDIVIDUELL_KARRIERESTØTTE_UNG
        TiltakstypeSomGirRett.JOBBKLUBB -> StønadTypeTiltakspenger.JOBBKLUBB
        TiltakstypeSomGirRett.OPPFØLGING -> StønadTypeTiltakspenger.OPPFØLGING
        TiltakstypeSomGirRett.UTVIDET_OPPFØLGING_I_NAV -> StønadTypeTiltakspenger.UTVIDET_OPPFØLGING_I_NAV
        TiltakstypeSomGirRett.UTVIDET_OPPFØLGING_I_OPPLÆRING -> StønadTypeTiltakspenger.UTVIDET_OPPFØLGING_I_OPPLÆRING
        TiltakstypeSomGirRett.FORSØK_OPPLÆRING_LENGRE_VARIGHET -> StønadTypeTiltakspenger.FORSØK_OPPLÆRING_LENGRE_VARIGHET
    }
