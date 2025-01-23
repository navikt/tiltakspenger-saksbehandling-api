package no.nav.tiltakspenger.vedtak.clients.utbetaling

import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.utbetaling.domene.Utbetalingsvedtak
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
    val forrigeUtbetaling = forrigeUtbetalingJson?.let { deserialize<IverksettV2Dto>(it) }
    val vedtak: Utbetalingsvedtak = this
    val nyeUtbetalinger = meldekortbehandling.toUtbetalingDto(vedtak.brukerNavkontor)

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
            utbetalinger = (forrigeUtbetaling?.vedtak?.utbetalinger ?: emptyList()) + nyeUtbetalinger,
        ),
        forrigeIverksetting =
        vedtak.forrigeUtbetalingsvedtakId?.let { ForrigeIverksettingV2Dto(behandlingId = it.uuidPart()) },
    ).let {
        serialize(it)
    }
}

private fun MeldekortBehandling.UtfyltMeldekort.toUtbetalingDto(
    brukersNavKontor: Navkontor,
): List<UtbetalingV2Dto> {
    return this.beregning.fold((listOf())) { acc: List<UtbetalingV2Dto>, meldekortdag ->
        meldekortdag as MeldeperiodeBeregningDag.Utfylt
        val meldeperiodeKjedeId = this.meldeperiodeKjedeId
        when (val sisteUtbetalingsperiode = acc.lastOrNull()) {
            null -> {
                meldekortdag.genererUtbetalingsperiode(
                    meldeperiodeKjedeId = meldeperiodeKjedeId,
                    brukersNavKontor = brukersNavKontor,
                )?.let { acc + it } ?: acc
            }

            else ->
                sisteUtbetalingsperiode.leggTil(meldekortdag, this.meldeperiodeKjedeId, brukersNavKontor).let {
                    when (it) {
                        is Resultat.KanIkkeSlåSammen -> acc + it.utbetalingsperiode
                        is Resultat.KanSlåSammen -> acc.dropLast(1) + it.utbetalingsperiode
                        is Resultat.SkalIkkeUtbetales -> acc
                    }
                }
        }
    }
}

private fun MeldeperiodeBeregningDag.Utfylt.genererUtbetalingsperiode(
    meldeperiodeKjedeId: MeldeperiodeKjedeId,
    brukersNavKontor: Navkontor,
): UtbetalingV2Dto? {
    return when (this.reduksjon) {
        ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort -> null
        ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon, ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon ->
            UtbetalingV2Dto(
                beløp = this.beløp.toUInt(),
                satstype = Satstype.DAGLIG_INKL_HELG,
                fraOgMedDato = this.dato,
                tilOgMedDato = this.dato,
                stønadsdata =
                StønadsdataTiltakspengerV2Dto(
                    stønadstype = this.tiltakstype.mapStønadstype(),
                    // TODO barnetillegg: Legg til støtte for barnetillegg
                    barnetillegg = false,
                    brukersNavKontor = brukersNavKontor.kontornummer,
                    meldekortId = meldeperiodeKjedeId.verdi,
                ),
            )
    }
}

private fun UtbetalingV2Dto.leggTil(
    meldekortdag: MeldeperiodeBeregningDag.Utfylt,
    meldeperiodeKjedeId: MeldeperiodeKjedeId,
    brukersNavKontor: Navkontor,
): Resultat {
    val neste = meldekortdag.genererUtbetalingsperiode(meldeperiodeKjedeId, brukersNavKontor)
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
