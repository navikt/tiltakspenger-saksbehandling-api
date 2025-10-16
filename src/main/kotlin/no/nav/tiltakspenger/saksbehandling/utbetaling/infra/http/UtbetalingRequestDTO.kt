package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.felles.erHelg
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.utsjekk.kontrakter.felles.Personident
import no.nav.utsjekk.kontrakter.felles.Satstype
import no.nav.utsjekk.kontrakter.felles.StønadTypeTiltakspenger
import no.nav.utsjekk.kontrakter.iverksett.ForrigeIverksettingV2Dto
import no.nav.utsjekk.kontrakter.iverksett.IverksettV2Dto
import no.nav.utsjekk.kontrakter.iverksett.StønadsdataTiltakspengerV2Dto
import no.nav.utsjekk.kontrakter.iverksett.UtbetalingV2Dto
import no.nav.utsjekk.kontrakter.iverksett.VedtaksdetaljerV2Dto
import java.time.LocalDate
import kotlin.collections.fold

/**
 * @param forrigeUtbetalingJson Forrige utbetaling vi sendte til helved. Siden vi må sende alle utbetalinger på nytt, må vi sende med alle utbetalinger vi har sendt tidligere.
 */
fun VedtattUtbetaling.toUtbetalingRequestDTO(
    forrigeUtbetalingJson: String?,
): String {
    return IverksettV2Dto(
        sakId = saksnummer.toString(),
        // Brukes som dedupliseringsnøkkel av helved dersom iverksettingId er null.
        behandlingId = id.uuidPart(),
        // Dersom en vedtaksløsning har behov for å sende flere utbetalinger per behandling/vedtak, kan dette feltet brukes for å skille de. Denne blir brukt som delytelseId mot OS/UR. Se slack tråd: https://nav-it.slack.com/archives/C06SJTR2X3L/p1724136342664969
        iverksettingId = null,
        personident = Personident(verdi = fnr.verdi),
        vedtak =
        VedtaksdetaljerV2Dto(
            vedtakstidspunkt = opprettet,
            saksbehandlerId = saksbehandler,
            beslutterId = beslutter,
            utbetalinger = beregning.tilUtbetalingerDTO(
                brukersNavkontor = brukerNavkontor,
                kanUtbetaleHelgPåFredag = kanUtbetaleHelgPåFredag,
                forrigeUtbetalingJson = forrigeUtbetalingJson,
            ),
        ),
        forrigeIverksetting = forrigeUtbetalingId?.let { ForrigeIverksettingV2Dto(behandlingId = it.uuidPart()) },
    ).let { serialize(it) }
}

/**
 * Brukes både av simulering og iverksetting.
 */
fun Beregning.tilUtbetalingerDTO(
    brukersNavkontor: Navkontor,
    kanUtbetaleHelgPåFredag: Boolean,
    forrigeUtbetalingJson: String?,
): List<UtbetalingV2Dto> {
    val meldeperioderTilOppdrag = this.beregninger.map {
        MeldeperiodeTilOppdrag(it, kanUtbetaleHelgPåFredag)
    }

    val utbetalingerStønad = meldeperioderTilOppdrag.toUtbetalingDto(brukersNavkontor, barnetillegg = false)
    val utbetalingerBarnetillegg = meldeperioderTilOppdrag.toUtbetalingDto(brukersNavkontor, barnetillegg = true)

    val nyeOgOppdaterteUtbetalinger = utbetalingerStønad.plus(utbetalingerBarnetillegg)

    val tidligereUtbetalinger = forrigeUtbetalingJson
        ?.let { deserialize<IverksettV2Dto>(it) }
        ?.hentIkkeOppdaterteUtbetalinger(this) ?: emptyList()

    val utbetalinger = tidligereUtbetalinger.plus(nyeOgOppdaterteUtbetalinger).sortedBy { it.fraOgMedDato }

    utbetalinger.valider()
    return utbetalinger
}

private fun List<MeldeperiodeTilOppdrag>.toUtbetalingDto(
    brukersNavKontor: Navkontor,
    barnetillegg: Boolean,
): List<UtbetalingV2Dto> {
    return this.map {
        it.dager.toUtbetalingDto(
            brukersNavKontor,
            barnetillegg,
            it.kjedeId,
        )
    }.flatten()
}

private fun List<UtbetalingV2Dto>.valider() {
    this.groupBy {
        val stønadsdata = it.stønadsdata as StønadsdataTiltakspengerV2Dto
        stønadsdata.barnetillegg
    }.values.forEach {
        it.zipWithNext().forEach { (a, b) ->
            require(a.tilOgMedDato < b.fraOgMedDato) {
                "Utbetalingsperiodene kan ikke ha overlap - $a $b"
            }
        }
    }
}

private fun IverksettV2Dto.hentIkkeOppdaterteUtbetalinger(beregning: Beregning): List<UtbetalingV2Dto> {
    val oppdaterteKjeder = beregning.beregninger.map { it.kjedeId.verdi }.toSet()
    return this.vedtak.utbetalinger.filterNot { tidligereUtbetaling ->
        val stønadsdata = tidligereUtbetaling.stønadsdata as StønadsdataTiltakspengerV2Dto
        oppdaterteKjeder.contains(stønadsdata.meldekortId)
    }
}

private fun List<MeldeperiodeTilOppdrag.BeregnetDag>.toUtbetalingDto(
    brukersNavKontor: Navkontor,
    barnetillegg: Boolean,
    kjedeId: MeldeperiodeKjedeId,
): List<UtbetalingV2Dto> {
    return this.fold(listOf()) { acc: List<UtbetalingV2Dto>, meldeperiodeDag ->
        when (val sisteUtbetalingsperiode = acc.lastOrNull()) {
            null -> {
                meldeperiodeDag.genererUtbetalingsperiode(
                    kjedeId = kjedeId,
                    brukersNavKontor = brukersNavKontor,
                    barnetillegg = barnetillegg,
                )?.let { acc + it } ?: acc
            }

            else ->
                sisteUtbetalingsperiode.leggTil(
                    meldeperiodeDag = meldeperiodeDag,
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
}

private fun MeldeperiodeTilOppdrag.BeregnetDag.genererUtbetalingsperiode(
    kjedeId: MeldeperiodeKjedeId,
    brukersNavKontor: Navkontor,
    barnetillegg: Boolean,
): UtbetalingV2Dto? {
    val beløp = if (barnetillegg) this.beløpBarnetillegg else this.beløp

    // Vi ønsker ikke lage linjer for 0-beløp (safeguard).
    if (beløp == 0) {
        return null
    }

    // For å utbetale helgedager må vi bruke Satstype.DAGLIG_INKL_HELG. Denne er ikke stabil per nå.
    if (this.dato.erHelg()) {
        throw IllegalArgumentException("Helgedager kan ikke ha et beregnet beløp, ettersom det ikke vil bli utbetalt - dato: ${this.dato}")
    }

    return UtbetalingV2Dto(
        beløp = beløp.toUInt(),
        satstype = Satstype.DAGLIG,
        fraOgMedDato = this.dato,
        tilOgMedDato = this.dato,
        stønadsdata =
        StønadsdataTiltakspengerV2Dto(
            stønadstype = this.tiltakstype.mapStønadstype(),
            barnetillegg = barnetillegg,
            brukersNavKontor = brukersNavKontor.kontornummer,
            meldekortId = kjedeId.verdi,
        ),
    )
}

private fun UtbetalingV2Dto.leggTil(
    meldeperiodeDag: MeldeperiodeTilOppdrag.BeregnetDag,
    kjedeId: MeldeperiodeKjedeId,
    brukersNavKontor: Navkontor,
    barnetillegg: Boolean,
): Resultat {
    val neste = meldeperiodeDag.genererUtbetalingsperiode(kjedeId, brukersNavKontor, barnetillegg)
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

private data class MeldeperiodeTilOppdrag(
    private val meldeperiodeBeregning: MeldeperiodeBeregning,
    private val kanUtbetaleHelgPåFredag: Boolean,
) {
    val kjedeId = meldeperiodeBeregning.kjedeId

    val dager = meldeperiodeBeregning.dager.map {
        BeregnetDag(
            dato = it.dato,
            beløp = it.beløp,
            beløpBarnetillegg = it.beløpBarnetillegg,
            tiltakstype = it.tiltakstype!!,
        )
    }.let {
        if (kanUtbetaleHelgPåFredag) {
            it.chunked(7).flatMap { uke -> tilUkeMedUtbetaltHelgPåFredag(uke) }
        } else {
            it
        }
    }

    private fun tilUkeMedUtbetaltHelgPåFredag(uke: List<BeregnetDag>): List<BeregnetDag> {
        val mandagTilTorsdag = uke.take(4)
        val (fredag, lørdag, søndag) = uke.takeLast(3)

        val superFredag = fredag.copy(
            beløp = fredag.beløp + lørdag.beløp + søndag.beløp,
            beløpBarnetillegg = fredag.beløpBarnetillegg + lørdag.beløpBarnetillegg + søndag.beløpBarnetillegg,
        )

        val nullLørdag = lørdag.copy(beløp = 0, beløpBarnetillegg = 0)
        val nullSøndag = søndag.copy(beløp = 0, beløpBarnetillegg = 0)

        return mandagTilTorsdag.plus(listOf(superFredag, nullLørdag, nullSøndag))
    }

    data class BeregnetDag(
        val dato: LocalDate,
        val beløp: Int,
        val beløpBarnetillegg: Int,
        val tiltakstype: TiltakstypeSomGirRett,
    )
}
