package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskForlengelse

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.periodiserAntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.Deltakelsestilstand
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.harSammeDeltakelsesmengde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.tilDeltakelsestilstand
import java.time.LocalDate

/**
 * Det systemet har utledet for å kunne fylle ut en automatisk opprettet forlengelse (revurdering til innvilgelse) uten saksbehandler.
 *
 * @param innvilgelsesperiode Fra dagen etter siste innvilgede dag i det gjeldende vedtaket, til ny sluttdato for deltakelsen.
 * @param antallDagerPerMeldeperiode Videreført fra det gjeldende vedtaket, eller utledet fra deltakelsesmengden dersom den er endret.
 * @param antallBarn Dekker hele [innvilgelsesperiode], se [utledAntallBarn].
 * @param begrunnelse Begrunnelsen for vilkårsvurderingen.
 * @param begrunnelseBarnetillegg Begrunnelsen for barnetillegget, eller null dersom forlengelsen ikke har barnetillegg.
 */
data class AutomatiskForlengelse(
    val innvilgelsesperiode: Periode,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
    val antallBarn: SammenhengendePeriodisering<AntallBarn>,
    val tiltaksdeltakerId: TiltaksdeltakerId,
    val begrunnelse: Begrunnelse,
    val begrunnelseBarnetillegg: Begrunnelse?,
) {
    init {
        require(antallBarn.totalPeriode == innvilgelsesperiode) {
            "Antall barn må dekke hele innvilgelsesperioden $innvilgelsesperiode, men dekket ${antallBarn.totalPeriode}"
        }
    }

    val barnetillegg: Barnetillegg = Barnetillegg(periodisering = antallBarn, begrunnelse = begrunnelseBarnetillegg)
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
 * Barnetillegget videreføres fra det gjeldende vedtaket, og periodiseres ut fra barna i søknaden, se [utledAntallBarn].
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
        nyDeltakelsesmengde.utledDeltakelsesmengde().getOrElse { return it.left() }
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

    val antallBarnIVedtaket = rammevedtaksliste.barnetilleggsperioder.hentVerdiForDag(sisteInnvilgedeDag) ?: AntallBarn.ZERO
    val antallBarn = utledAntallBarn(
        tiltaksdeltakerId = tiltaksdeltakerId,
        sisteInnvilgedeDag = sisteInnvilgedeDag,
        antallBarnIVedtaket = antallBarnIVedtaket,
        innvilgelsesperiode = innvilgelsesperiode,
    ).getOrElse { return it.left() }

    val antallDagerPerMeldeperiode = endretDeltakelsesmengde?.antallDagerPerMeldeperiode
        ?: gjeldendeVedtak.antallDagerPerMeldeperiode!!.hentVerdiForDag(sisteInnvilgedeDag)!!

    return AutomatiskForlengelse(
        innvilgelsesperiode = innvilgelsesperiode,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        antallBarn = antallBarn,
        tiltaksdeltakerId = tiltaksdeltakerId,
        begrunnelse = lagBegrunnelse(
            nySluttdato = nySluttdato,
            endretDeltakelsesmengde = endretDeltakelsesmengde,
            antallBarnIVedtaket = antallBarnIVedtaket,
            antallBarn = antallBarn,
        ),
        begrunnelseBarnetillegg = lagBegrunnelseBarnetillegg(
            antallBarnIVedtaket = antallBarnIVedtaket,
            antallBarn = antallBarn,
        ),
    ).right()
}

/**
 * Utleder antall barn med barnetillegg i [innvilgelsesperiode].
 *
 * Hadde det gjeldende vedtaket ikke barnetillegg på [sisteInnvilgedeDag] ([antallBarnIVedtaket] er 0), får heller ikke forlengelsen barnetillegg.
 * Nye barn krever en ny søknad, så det er ikke noe å vurdere.
 *
 * Ellers periodiseres barnetillegget ut fra barna i søknaden som ligger til grunn for innvilgelsen av [tiltaksdeltakerId], se [periodiserAntallBarn].
 * Slik faller barnetillegget bort for barn som fyller 16 år i forlengelsen, på samme måte som når saksbehandler forhåndsutfyller fra søknaden.
 * Søknaden må gi like mange barn som det gjeldende vedtaket på [sisteInnvilgedeDag], slik at vi vet at det er de samme barna som er innvilget.
 * Har saksbehandler for eksempel innvilget barnetillegg for et barn utenfor EØS, eller lagt til et barn, overlates forlengelsen til saksbehandler.
 * Det samme gjelder om søknaden gir flere barn i forlengelsen enn i vedtaket, siden et nytt barn må vurderes av saksbehandler.
 */
private fun Sak.utledAntallBarn(
    tiltaksdeltakerId: TiltaksdeltakerId,
    sisteInnvilgedeDag: LocalDate,
    antallBarnIVedtaket: AntallBarn,
    innvilgelsesperiode: Periode,
): Either<ManueltBehandlesGrunn, SammenhengendePeriodisering<AntallBarn>> {
    if (antallBarnIVedtaket == AntallBarn.ZERO) {
        return SammenhengendePeriodisering(AntallBarn.ZERO, innvilgelsesperiode).right()
    }

    val barnFraSøknaden = rammevedtaksliste
        .filter { it.rammebehandling is Søknadsbehandling && it.rammebehandlingsresultat is Søknadsbehandlingsresultat.Innvilgelse }
        .filter { vedtak -> vedtak.valgteTiltaksdeltakelser!!.verdier.any { it.internDeltakelseId == tiltaksdeltakerId } }
        .maxByOrNull { it.opprettet }
        ?.let { (it.rammebehandling as Søknadsbehandling).søknad.barnetillegg }
        ?: return ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_BARNETILLEGG.left()

    val antallBarnFraSøknadenISisteInnvilgedeDag = barnFraSøknaden
        .periodiserAntallBarn(sisteInnvilgedeDag til sisteInnvilgedeDag)
        .verdier
        .single()
    if (antallBarnFraSøknadenISisteInnvilgedeDag != antallBarnIVedtaket) {
        return ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_BARNETILLEGG.left()
    }

    val antallBarn = barnFraSøknaden.periodiserAntallBarn(innvilgelsesperiode)
    if (antallBarn.verdier.any { it.value > antallBarnIVedtaket.value }) {
        return ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_BARNETILLEGG.left()
    }

    return antallBarn.right()
}

/**
 * Antall dager per meldeperiode utledet fra en deltakelsesmengde, med en beskrivelse av hva det er utledet fra.
 */
data class UtledetDeltakelsesmengde(
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
    val beskrivelse: String,
)

private const val DAGER_PER_MELDEPERIODE_VED_FULLTID = 10F
private const val PROSENT_VED_FULLTID = 100F
private const val UKER_PER_MELDEPERIODE = 2F

/**
 * Utleder antall dager per meldeperiode fra antall dager per uke dersom det er satt, ellers fra deltakelsesprosenten.
 * Dager per uke ganges med antall uker i en meldeperiode, så 2,5 dager per uke blir 5 dager per meldeperiode.
 * Prosenten regnes om der 100 prosent er 5 dager per uke, så 80 prosent blir 4 dager per uke og 8 dager per meldeperiode.
 * En verdi på 0 regnes som ikke satt, på samme måte som i sammenligningen av deltakelsesmengder.
 * Gir grunnen til manuell behandling dersom ingen av verdiene er satt, deltakelsesmengden er mer enn fulltid, eller den ikke gir et helt antall dager per meldeperiode.
 * Vi runder ikke av, siden det ikke er entydig hvilken vei det skal rundes.
 */
fun Deltakelsestilstand.utledDeltakelsesmengde(): Either<ManueltBehandlesGrunn, UtledetDeltakelsesmengde> {
    val dagerPerUke = antallDagerPerUke?.takeIf { it > 0F }
    val prosent = deltakelseProsent?.takeIf { it > 0F }

    // Prosenten ganges før den deles, slik at hele prosenter gir eksakte flyttall.
    val (dagerPerMeldeperiode, beskrivelse) = when {
        dagerPerUke != null -> dagerPerUke * UKER_PER_MELDEPERIODE to "${dagerPerUke.formater()} dager per uke"
        prosent != null -> prosent * DAGER_PER_MELDEPERIODE_VED_FULLTID / PROSENT_VED_FULLTID to "${prosent.formater()} prosent"
        else -> return ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_DELTAKELSESMENGDE.left()
    }

    if (dagerPerMeldeperiode > DAGER_PER_MELDEPERIODE_VED_FULLTID) {
        return ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_DELTAKELSESMENGDE.left()
    }

    if (dagerPerMeldeperiode % 1F != 0F) {
        return ManueltBehandlesGrunn.FORLENGELSE_DELTAKELSESMENGDE_GIR_IKKE_HELE_DAGER.left()
    }

    return UtledetDeltakelsesmengde(
        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(dagerPerMeldeperiode.toInt()),
        beskrivelse = beskrivelse,
    ).right()
}

private fun Float.formater(): String =
    if (this % 1F == 0F) toInt().toString() else toString().replace('.', ',')

/**
 * Begrunnelsen for vilkårsvurderingen.
 * Antall dager per meldeperiode nevnes kun når deltakelsesmengden er endret.
 * Barnetillegget begrunnes i eget felt, se [lagBegrunnelseBarnetillegg], bortsett fra når det faller helt bort.
 */
private fun lagBegrunnelse(
    nySluttdato: LocalDate,
    endretDeltakelsesmengde: UtledetDeltakelsesmengde?,
    antallBarnIVedtaket: AntallBarn,
    antallBarn: SammenhengendePeriodisering<AntallBarn>,
): Begrunnelse {
    val antallDager = endretDeltakelsesmengde?.let {
        "\nDeltakelsesmengden er endret til ${it.beskrivelse}, som gir ${it.antallDagerPerMeldeperiode.value} dager per meldeperiode."
    } ?: ""
    val barnetilleggFallerBort = if (antallBarnIVedtaket != AntallBarn.ZERO && antallBarn.inneholderKun(AntallBarn.ZERO)) {
        "\nBarnetillegget for ${antallBarnIVedtaket.value} barn i det gjeldende vedtaket faller bort, fordi barna har fylt 16 år."
    } else {
        ""
    }
    return Begrunnelse.create(
        "Automatisk behandlet.\n" +
            "Tiltaksdeltakelsen er forlenget til ${nySluttdato.format(norskDatoFormatter)}." +
            antallDager +
            barnetilleggFallerBort,
    )!!
}

/**
 * Begrunnelsen for barnetillegget.
 * Er null når forlengelsen ikke har barnetillegg, enten fordi det gjeldende vedtaket ikke hadde det, eller fordi det faller helt bort.
 */
private fun lagBegrunnelseBarnetillegg(
    antallBarnIVedtaket: AntallBarn,
    antallBarn: SammenhengendePeriodisering<AntallBarn>,
): Begrunnelse? = when {
    antallBarn.inneholderKun(AntallBarn.ZERO) -> null

    antallBarn.inneholderKun(antallBarnIVedtaket) -> Begrunnelse.create(
        "Barnetillegg for ${antallBarnIVedtaket.value} barn videreføres fra det gjeldende vedtaket.",
    )

    else -> Begrunnelse.create(
        "Barnetillegget for ${antallBarnIVedtaket.value} barn i det gjeldende vedtaket reduseres når barn fyller 16 år:" +
            antallBarn.perioderMedVerdi.joinToString("") { (antall, periode) ->
                "\n${antall.value} barn fra og med ${periode.fraOgMed.format(norskDatoFormatter)} " +
                    "til og med ${periode.tilOgMed.format(norskDatoFormatter)}."
            },
    )
}
