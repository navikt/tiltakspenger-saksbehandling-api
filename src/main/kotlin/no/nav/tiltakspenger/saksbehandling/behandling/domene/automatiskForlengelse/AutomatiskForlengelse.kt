package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskForlengelse

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.harSammeDeltakelsesmengde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.tilDeltakelsestilstand
import java.time.LocalDate

/**
 * Det systemet har utledet for å kunne fylle ut en automatisk opprettet forlengelse (revurdering til innvilgelse) uten saksbehandler.
 *
 * @param innvilgelsesperiode Fra dagen etter siste innvilgede dag i det gjeldende vedtaket, til ny sluttdato for deltakelsen.
 * @param antallDagerPerMeldeperiode Videreført fra det gjeldende vedtaket.
 */
data class AutomatiskForlengelse(
    val innvilgelsesperiode: Periode,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
    val tiltaksdeltakerId: TiltaksdeltakerId,
    val begrunnelse: Begrunnelse,
) {
    /** Forlengelser med barnetillegg overlates foreløpig til saksbehandler, se [utledAutomatiskForlengelse]. */
    val barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(nonEmptyListOf(innvilgelsesperiode))
}

/**
 * Utleder verdiene for å fylle ut en forlengelse av [tiltaksdeltakerId] uten saksbehandler.
 * Gir grunnen til at forlengelsen må behandles manuelt dersom verdiene ikke kan utledes med stor sikkerhet.
 *
 * En forlengelse kan automatiseres når tiltaksdeltakelsen er utvidet fremover i tid fra et gjeldende vedtak, og vedtaket ikke er stanset eller opphørt.
 * Det gjeldende vedtaket er det siste vedtaket i den innvilgede tidslinjen som innvilger for [tiltaksdeltakerId].
 * Innvilgelsen må fortsatt gjelde helt frem til vedtakets opprinnelige sluttdato, og den må ha gått frem til deltakelsens tidligere sluttdato.
 * Forlengelsen innvilges fra dagen etter, til ny sluttdato, med samme antall dager per meldeperiode som i det gjeldende vedtaket.
 *
 * Deltakelsen slik den er registrert nå, hentes fra [saksopplysninger], slik at det er de samme opplysningene beslutter ser.
 * Endringer som har kommet etter at forlengelsen ble opprettet, fanges dermed opp her.
 * Startdato og deltakelsesmengde må være uendret fra det gjeldende vedtaket, siden en forlengelse bare utvider perioden.
 *
 * Vurderingen er bevisst konservativ, og alt som ikke er entydig overlates til saksbehandler.
 * Blant annet overlates forlengelser med barnetillegg, siden vi ikke vurderer barnas alder i den nye perioden ennå.
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

    if (!tidligereDeltakelse.tilDeltakelsestilstand().harSammeDeltakelsesmengde(nåtilstand.tilDeltakelsestilstand())) {
        return ManueltBehandlesGrunn.FORLENGELSE_ENDRET_DELTAKELSESMENGDE.left()
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
    if (antallBarn != AntallBarn.ZERO) {
        return ManueltBehandlesGrunn.FORLENGELSE_HAR_BARNETILLEGG.left()
    }

    val antallDagerPerMeldeperiode = gjeldendeVedtak.antallDagerPerMeldeperiode!!.hentVerdiForDag(sisteInnvilgedeDag)!!

    return AutomatiskForlengelse(
        innvilgelsesperiode = innvilgelsesperiode,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        tiltaksdeltakerId = tiltaksdeltakerId,
        begrunnelse = lagBegrunnelse(innvilgelsesperiode, nySluttdato, antallDagerPerMeldeperiode),
    ).right()
}

private fun lagBegrunnelse(
    innvilgelsesperiode: Periode,
    nySluttdato: LocalDate,
    antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
): Begrunnelse {
    return Begrunnelse.create(
        "Automatisk behandlet.\n" +
            "Tiltaksdeltakelsen er forlenget til ${nySluttdato.format(norskDatoFormatter)}.\n" +
            "Tiltakspengene innvilges fra og med ${innvilgelsesperiode.fraOgMed.format(norskDatoFormatter)} " +
            "til og med ${innvilgelsesperiode.tilOgMed.format(norskDatoFormatter)}, " +
            "med ${antallDagerPerMeldeperiode.value} dager per meldeperiode som i det gjeldende vedtaket.",
    )!!
}
