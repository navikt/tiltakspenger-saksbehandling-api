package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskForlengelse

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.Deltakelsestilstand
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.harSammeDeltakelsesmengde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.tilDeltakelsestilstand
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Det systemet har utledet for å kunne fylle ut en automatisk opprettet forlengelse (revurdering til innvilgelse) uten saksbehandler.
 *
 * @param innvilgelsesperiode Fra dagen etter siste innvilgede dag i det gjeldende vedtaket, til ny sluttdato for deltakelsen.
 * @param antallDagerPerMeldeperiode Videreført fra det gjeldende vedtaket, eller utledet fra deltakelsesmengden dersom den er endret.
 * @param antallBarn Videreført fra det gjeldende vedtaket, og gjelder hele [innvilgelsesperiode].
 */
data class AutomatiskForlengelse(
    val innvilgelsesperiode: Periode,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
    val antallBarn: AntallBarn,
    val tiltaksdeltakerId: TiltaksdeltakerId,
    val begrunnelse: Begrunnelse,
) {
    val barnetillegg: Barnetillegg = Barnetillegg(
        periodisering = nonEmptyListOf(PeriodeMedVerdi(antallBarn, innvilgelsesperiode)).tilIkkeTomPeriodisering(),
        begrunnelse = null,
    )
}

/**
 * Utleder verdiene for å fylle ut en forlengelse av [tiltaksdeltakerId] uten saksbehandler.
 * Gir grunnen til at forlengelsen må behandles manuelt dersom verdiene ikke kan utledes med stor sikkerhet.
 *
 * En forlengelse kan automatiseres når tiltaksdeltakelsen er utvidet fremover i tid fra et gjeldende vedtak, og vedtaket ikke er stanset eller opphørt.
 * Det gjeldende vedtaket er det siste vedtaket i den innvilgede tidslinjen som innvilger for [tiltaksdeltakerId].
 * Innvilgelsen må fortsatt gjelde helt frem til vedtakets opprinnelige sluttdato, og den må ha gått frem til deltakelsens tidligere sluttdato.
 * Forlengelsen innvilges fra dagen etter, til ny sluttdato.
 *
 * Deltakelsen slik den er registrert nå, hentes fra [saksopplysninger], slik at det er de samme opplysningene beslutter ser.
 * Endringer som har kommet etter at forlengelsen ble opprettet, fanges dermed opp her.
 * Startdatoen må være uendret fra det gjeldende vedtaket, siden en forlengelse bare utvider perioden.
 *
 * Er deltakelsesmengden uendret, videreføres antall dager per meldeperiode fra det gjeldende vedtaket.
 * Er den endret, utledes antall dager per meldeperiode fra den nye deltakelsesmengden, se [utledDeltakelsesmengde].
 * Kan bruker melde i helg, overlates en endret deltakelsesmengde likevel til saksbehandler.
 * Antall barn med barnetillegg videreføres fra det gjeldende vedtaket.
 *
 * Vurderingen er bevisst konservativ, og alt som ikke er entydig overlates til saksbehandler.
 *
 * @param saksopplysninger Saksopplysningene til revurderingen, hentet på nytt før vurderingen.
 */
fun Sak.utledAutomatiskForlengelse(
    tiltaksdeltakerId: TiltaksdeltakerId,
    saksopplysninger: Saksopplysninger,
): Either<ManueltBehandlesGrunn, AutomatiskForlengelse> {
    val (gjeldendeVedtak, gjeldendePeriode) = rammevedtaksliste.innvilgetTidslinje.perioderMedVerdi
        .filter { (vedtak, periode) ->
            vedtak.valgteTiltaksdeltakelser?.hentVerdiForDag(periode.tilOgMed)?.internDeltakelseId == tiltaksdeltakerId
        }
        .maxByOrNull { it.periode.tilOgMed }
        ?: return ManueltBehandlesGrunn.FORLENGELSE_FANT_IKKE_GJELDENDE_VEDTAK.left()

    val sisteInnvilgedeDag = gjeldendePeriode.tilOgMed

    // Er slutten av innvilgelsen erstattet av et senere vedtak, er vedtaket stanset, opphørt eller omgjort.
    if (sisteInnvilgedeDag != gjeldendeVedtak.innvilgelsesperioder!!.tilOgMed) {
        return ManueltBehandlesGrunn.FORLENGELSE_VEDTAKET_ER_STANSET_ELLER_OPPHORT.left()
    }

    val tidligereDeltakelse = gjeldendeVedtak.valgteTiltaksdeltakelser!!.hentVerdiForDag(sisteInnvilgedeDag)!!
    if (tidligereDeltakelse.deltakelseTilOgMed != sisteInnvilgedeDag) {
        return ManueltBehandlesGrunn.FORLENGELSE_INNVILGELSEN_SLUTTER_FOR_DELTAKELSEN.left()
    }

    val nåtilstand = saksopplysninger.getTiltaksdeltakelse(tiltaksdeltakerId)
        ?: return ManueltBehandlesGrunn.FORLENGELSE_FANT_IKKE_TILTAKSDELTAKELSE.left()

    if (!nåtilstand.deltakelseStatus.deltarEllerHarDeltatt()) {
        return ManueltBehandlesGrunn.FORLENGELSE_DELTAKELSEN_HAR_UGYLDIG_STATUS.left()
    }

    if (!nåtilstand.rettPåTiltakspenger) {
        return ManueltBehandlesGrunn.FORLENGELSE_TILTAKET_GIR_IKKE_RETT.left()
    }

    val nySluttdato = nåtilstand.deltakelseTilOgMed
        ?: return ManueltBehandlesGrunn.FORLENGELSE_DELTAKELSEN_MANGLER_SLUTTDATO.left()

    if (!nySluttdato.isAfter(sisteInnvilgedeDag)) {
        return ManueltBehandlesGrunn.FORLENGELSE_SLUTTDATO_ER_IKKE_ETTER_INNVILGELSEN.left()
    }

    if (nåtilstand.deltakelseFraOgMed != tidligereDeltakelse.deltakelseFraOgMed) {
        return ManueltBehandlesGrunn.FORLENGELSE_ENDRET_STARTDATO.left()
    }

    val nyDeltakelsesmengde = nåtilstand.tilDeltakelsestilstand()
    val endretDeltakelsesmengde = if (tidligereDeltakelse.tilDeltakelsestilstand().harSammeDeltakelsesmengde(nyDeltakelsesmengde)) {
        null
    } else {
        if (kanSendeInnHelgForMeldekort) {
            return ManueltBehandlesGrunn.FORLENGELSE_ENDRET_DELTAKELSESMENGDE_OG_KAN_MELDE_I_HELG.left()
        }
        nyDeltakelsesmengde.utledDeltakelsesmengde()
            ?: return ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_DELTAKELSESMENGDE.left()
    }

    val innvilgelsesperiode = sisteInnvilgedeDag.plusDays(1) til nySluttdato

    if (rammevedtaksliste.tidslinje.overlapper(innvilgelsesperiode)) {
        return ManueltBehandlesGrunn.FORLENGELSE_OVERLAPPER_ANDRE_VEDTAK.left()
    }

    if (saksopplysninger.harOverlappendeTiltaksdeltakelse(tiltaksdeltakerId, innvilgelsesperiode)) {
        return ManueltBehandlesGrunn.FORLENGELSE_OVERLAPPENDE_TILTAK.left()
    }

    if (saksopplysninger.harAndreYtelserEnnTiltakspenger()) {
        return ManueltBehandlesGrunn.FORLENGELSE_ANDRE_YTELSER.left()
    }

    if (saksopplysninger.harTiltakspengevedtakFraArena()) {
        return ManueltBehandlesGrunn.FORLENGELSE_VEDTAK_I_ARENA.left()
    }

    val antallBarn = rammevedtaksliste.barnetilleggsperioder.hentVerdiForDag(sisteInnvilgedeDag) ?: AntallBarn.ZERO

    val antallDagerPerMeldeperiode = endretDeltakelsesmengde?.antallDagerPerMeldeperiode
        ?: gjeldendeVedtak.antallDagerPerMeldeperiode!!.hentVerdiForDag(sisteInnvilgedeDag)!!

    return AutomatiskForlengelse(
        innvilgelsesperiode = innvilgelsesperiode,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        antallBarn = antallBarn,
        tiltaksdeltakerId = tiltaksdeltakerId,
        begrunnelse = lagBegrunnelse(
            innvilgelsesperiode = innvilgelsesperiode,
            nySluttdato = nySluttdato,
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            endretDeltakelsesmengde = endretDeltakelsesmengde,
            antallBarn = antallBarn,
        ),
    ).right()
}

/**
 * Antall dager per meldeperiode utledet fra en deltakelsesmengde, med en beskrivelse av hva det er utledet fra.
 */
data class UtledetDeltakelsesmengde(
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
    val beskrivelse: String,
)

private const val DAGER_PER_UKE_VED_FULLTID = 5F
private const val PROSENT_VED_FULLTID = 100F
private const val UKER_PER_MELDEPERIODE = 2

/**
 * Utleder antall dager per meldeperiode fra antall dager per uke dersom det er satt, ellers fra deltakelsesprosenten.
 * Prosenten regnes om til dager per uke der 100 prosent er 5 dager per uke, så 80 prosent blir 4 dager per uke.
 * Dager per uke ganges med antall uker i en meldeperiode og rundes til nærmeste hele dag, men aldri lavere enn 1.
 * En verdi på 0 regnes som ikke satt, på samme måte som i sammenligningen av deltakelsesmengder.
 * Gir null dersom ingen av verdiene er satt, eller deltakelsesmengden er mer enn fulltid.
 */
fun Deltakelsestilstand.utledDeltakelsesmengde(): UtledetDeltakelsesmengde? {
    val dagerPerUke = antallDagerPerUke?.takeIf { it > 0F }
    val prosent = deltakelseProsent?.takeIf { it > 0F }

    val (utledetDagerPerUke, beskrivelse) = when {
        dagerPerUke != null -> dagerPerUke to "${dagerPerUke.formater()} dager per uke"
        prosent != null -> prosent / PROSENT_VED_FULLTID * DAGER_PER_UKE_VED_FULLTID to "${prosent.formater()} prosent"
        else -> return null
    }

    if (utledetDagerPerUke > DAGER_PER_UKE_VED_FULLTID) {
        return null
    }

    return UtledetDeltakelsesmengde(
        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(
            (utledetDagerPerUke * UKER_PER_MELDEPERIODE).roundToInt().coerceAtLeast(1),
        ),
        beskrivelse = beskrivelse,
    )
}

private fun Float.formater(): String =
    if (this % 1F == 0F) toInt().toString() else toString().replace('.', ',')

private fun lagBegrunnelse(
    innvilgelsesperiode: Periode,
    nySluttdato: LocalDate,
    antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
    endretDeltakelsesmengde: UtledetDeltakelsesmengde?,
    antallBarn: AntallBarn,
): Begrunnelse {
    val antallDager = if (endretDeltakelsesmengde == null) {
        "Antall dager per meldeperiode er ${antallDagerPerMeldeperiode.value}, som i det gjeldende vedtaket."
    } else {
        "Deltakelsesmengden er endret til ${endretDeltakelsesmengde.beskrivelse}, " +
            "som gir ${antallDagerPerMeldeperiode.value} dager per meldeperiode."
    }
    val barnetillegg = if (antallBarn == AntallBarn.ZERO) {
        ""
    } else {
        "\nBarnetillegg for ${antallBarn.value} barn videreføres fra det gjeldende vedtaket."
    }
    return Begrunnelse.create(
        "Automatisk behandlet.\n" +
            "Tiltaksdeltakelsen er forlenget til ${nySluttdato.format(norskDatoFormatter)}.\n" +
            "Tiltakspengene innvilges fra og med ${innvilgelsesperiode.fraOgMed.format(norskDatoFormatter)} " +
            "til og med ${innvilgelsesperiode.tilOgMed.format(norskDatoFormatter)}.\n" +
            antallDager +
            barnetillegg,
    )!!
}
