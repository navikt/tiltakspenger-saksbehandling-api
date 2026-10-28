package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskForlengelse

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.periodiserAntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.medPaddingForAndreYtelser
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
 * @param antallBarn Dekker hele [innvilgelsesperiode], se [utledAntallBarn].
 * @param begrunnelse Begrunnelsen for vilkårsvurderingen.
 * @param begrunnelseBarnetillegg Begrunnelsen for barnetillegget, eller null dersom forlengelsen ikke har barnetillegg.
 * @param fritekstTilVedtaksbrev Forklarer forlengelsen for bruker i vedtaksbrevet, se [lagFritekstTilVedtaksbrev].
 */
data class AutomatiskForlengelse(
    val innvilgelsesperiode: Periode,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
    val antallBarn: SammenhengendePeriodisering<AntallBarn>,
    val tiltaksdeltakerId: TiltaksdeltakerId,
    val begrunnelse: Begrunnelse,
    val begrunnelseBarnetillegg: Begrunnelse?,
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev,
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
 * Gir alle grunnene til at forlengelsen må behandles manuelt dersom verdiene ikke kan utledes med stor sikkerhet.
 * Sjekker som bygger på en verdi som ikke kunne utledes, hoppes over, slik at saksbehandler ikke får følgefeil.
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
 * Deltakelsesmengden må også være uendret, og antall dager per meldeperiode videreføres fra det gjeldende vedtaket.
 * En endret deltakelsesmengde overlates til saksbehandler, siden vi ikke vet fra hvilken dato den gjelder.
 * Den kan derfor også gjelde deler av perioden som allerede er innvilget, og da må den perioden omgjøres.
 * Barnetillegget videreføres fra det gjeldende vedtaket, og periodiseres ut fra barna i søknaden, se [utledAntallBarn].
 *
 * Vurderingen er bevisst konservativ, og alt som ikke er entydig overlates til saksbehandler.
 *
 * @param saksopplysninger Saksopplysningene til revurderingen, hentet på nytt før vurderingen.
 */
fun Sak.utledAutomatiskForlengelse(
    tiltaksdeltakerId: TiltaksdeltakerId,
    saksopplysninger: Saksopplysninger,
): Either<NonEmptyList<ManueltBehandlesGrunn>, AutomatiskForlengelse> {
    val grunner = mutableListOf<ManueltBehandlesGrunn>()

    val gjeldende = rammevedtaksliste.innvilgetTidslinje.perioderMedVerdi
        .filter { (vedtak, periode) ->
            vedtak.valgteTiltaksdeltakelser?.hentVerdiForDag(periode.tilOgMed)?.internDeltakelseId == tiltaksdeltakerId
        }
        .maxByOrNull { it.periode.tilOgMed }
    if (gjeldende == null) {
        grunner.add(ManueltBehandlesGrunn.FORLENGELSE_FANT_IKKE_GJELDENDE_VEDTAK)
    }
    val tidligereDeltakelse = gjeldende?.let { (vedtak, periode) -> vedtak.valgteTiltaksdeltakelser!!.hentVerdiForDag(periode.tilOgMed)!! }

    // Er slutten av innvilgelsen erstattet av et senere vedtak, er vedtaket stanset, opphørt eller omgjort.
    val erStansetEllerOpphørt = gjeldende != null && gjeldende.periode.tilOgMed != gjeldende.verdi.innvilgelsesperioder!!.tilOgMed
    if (erStansetEllerOpphørt) {
        grunner.add(ManueltBehandlesGrunn.FORLENGELSE_VEDTAKET_ER_STANSET_ELLER_OPPHORT)
    }
    // Er vedtaket stanset eller opphørt, slutter innvilgelsen alltid før deltakelsen, og det er ikke en egen grunn.
    val slutterFørDeltakelsen = gjeldende != null && !erStansetEllerOpphørt && tidligereDeltakelse!!.periode.tilOgMed != gjeldende.periode.tilOgMed
    if (slutterFørDeltakelsen) {
        grunner.add(ManueltBehandlesGrunn.FORLENGELSE_INNVILGELSEN_SLUTTER_FOR_DELTAKELSEN)
    }
    // Forlengelsen innvilges fra dagen etter, så den kan bare utledes når innvilgelsen gikk helt frem til deltakelsens tidligere sluttdato.
    val sisteInnvilgedeDag = gjeldende?.periode?.tilOgMed?.takeUnless { erStansetEllerOpphørt || slutterFørDeltakelsen }

    val nåtilstand = saksopplysninger.getTiltaksdeltakelse(tiltaksdeltakerId)
    if (nåtilstand == null) {
        grunner.add(ManueltBehandlesGrunn.FORLENGELSE_FANT_IKKE_TILTAKSDELTAKELSE)
    }

    if (nåtilstand != null && !nåtilstand.deltakelseStatus.deltarEllerHarDeltatt()) {
        grunner.add(ManueltBehandlesGrunn.FORLENGELSE_DELTAKELSEN_HAR_UGYLDIG_STATUS)
    }

    if (nåtilstand != null && !nåtilstand.rettPåTiltakspenger) {
        grunner.add(ManueltBehandlesGrunn.FORLENGELSE_TILTAKET_GIR_IKKE_RETT)
    }

    val nySluttdato = nåtilstand?.periode?.tilOgMed
    if (nåtilstand != null && nySluttdato == null) {
        grunner.add(ManueltBehandlesGrunn.FORLENGELSE_DELTAKELSEN_MANGLER_SLUTTDATO)
    }

    if (sisteInnvilgedeDag != null && nySluttdato != null && !nySluttdato.isAfter(sisteInnvilgedeDag)) {
        grunner.add(ManueltBehandlesGrunn.FORLENGELSE_SLUTTDATO_ER_IKKE_ETTER_INNVILGELSEN)
    }

    if (tidligereDeltakelse != null && nåtilstand != null && nåtilstand.periode.fraOgMed != tidligereDeltakelse.periode.fraOgMed) {
        grunner.add(ManueltBehandlesGrunn.FORLENGELSE_ENDRET_STARTDATO)
    }

    if (tidligereDeltakelse != null && nåtilstand != null &&
        !tidligereDeltakelse.tilDeltakelsestilstand().harSammeDeltakelsesmengde(nåtilstand.tilDeltakelsestilstand())
    ) {
        grunner.add(ManueltBehandlesGrunn.FORLENGELSE_ENDRET_DELTAKELSESMENGDE)
    }

    val innvilgelsesperiode = if (sisteInnvilgedeDag != null && nySluttdato != null && nySluttdato.isAfter(sisteInnvilgedeDag)) {
        sisteInnvilgedeDag.plusDays(1) til nySluttdato
    } else {
        null
    }

    if (innvilgelsesperiode != null && rammevedtaksliste.tidslinje.overlapper(innvilgelsesperiode)) {
        grunner.add(ManueltBehandlesGrunn.FORLENGELSE_OVERLAPPER_ANDRE_VEDTAK)
    }

    if (innvilgelsesperiode != null && saksopplysninger.harOverlappendeTiltaksdeltakelse(tiltaksdeltakerId, innvilgelsesperiode)) {
        grunner.add(ManueltBehandlesGrunn.FORLENGELSE_OVERLAPPENDE_TILTAK)
    }

    if (innvilgelsesperiode != null && saksopplysninger.harAndreYtelserEnnTiltakspengerSomOverlapper(innvilgelsesperiode.medPaddingForAndreYtelser())) {
        grunner.add(ManueltBehandlesGrunn.FORLENGELSE_ANDRE_YTELSER)
    }

    if (innvilgelsesperiode != null && saksopplysninger.harTiltakspengevedtakFraArenaSomOverlapper(innvilgelsesperiode.medPaddingForAndreYtelser())) {
        grunner.add(ManueltBehandlesGrunn.FORLENGELSE_VEDTAK_I_ARENA)
    }

    val antallBarnIVedtaket = sisteInnvilgedeDag?.let { rammevedtaksliste.barnetilleggsperioder.hentVerdiForDag(it) } ?: AntallBarn.ZERO
    val antallBarn = innvilgelsesperiode?.let {
        utledAntallBarn(
            tiltaksdeltakerId = tiltaksdeltakerId,
            sisteInnvilgedeDag = sisteInnvilgedeDag!!,
            antallBarnIVedtaket = antallBarnIVedtaket,
            innvilgelsesperiode = it,
        ).onLeft { grunn -> grunner.add(grunn) }.getOrNull()
    }

    grunner.toNonEmptyListOrNull()?.let { return it.left() }

    // Uten grunner til manuell behandling er alle verdiene utledet.
    return AutomatiskForlengelse(
        innvilgelsesperiode = innvilgelsesperiode!!,
        antallDagerPerMeldeperiode = gjeldende!!.verdi.antallDagerPerMeldeperiode!!.hentVerdiForDag(sisteInnvilgedeDag!!)!!,
        antallBarn = antallBarn!!,
        tiltaksdeltakerId = tiltaksdeltakerId,
        begrunnelse = lagBegrunnelse(
            nySluttdato = nySluttdato!!,
            antallBarnIVedtaket = antallBarnIVedtaket,
            antallBarn = antallBarn,
        ),
        begrunnelseBarnetillegg = lagBegrunnelseBarnetillegg(
            antallBarnIVedtaket = antallBarnIVedtaket,
            antallBarn = antallBarn,
        ),
        fritekstTilVedtaksbrev = lagFritekstTilVedtaksbrev(
            sisteInnvilgedeDag = sisteInnvilgedeDag,
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
 * Begrunnelsen for vilkårsvurderingen.
 * Barnetillegget begrunnes i eget felt, se [lagBegrunnelseBarnetillegg], bortsett fra når det faller helt bort.
 */
private fun lagBegrunnelse(
    nySluttdato: LocalDate,
    antallBarnIVedtaket: AntallBarn,
    antallBarn: SammenhengendePeriodisering<AntallBarn>,
): Begrunnelse {
    val barnetilleggFallerBort = if (antallBarnIVedtaket != AntallBarn.ZERO && antallBarn.inneholderKun(AntallBarn.ZERO)) {
        "\nBarnetillegget for ${antallBarnIVedtaket.value} barn i det gjeldende vedtaket faller bort, fordi barna har fylt 16 år."
    } else {
        ""
    }
    return Begrunnelse.create(
        "Automatisk behandlet.\n" +
            "Tiltaksdeltakelsen er forlenget til ${nySluttdato.format(norskDatoFormatter)}." +
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

/**
 * Teksten til bruker i vedtaksbrevet.
 * Nevner barnetillegg når forlengelsen har barnetillegg.
 *
 * Fyller barn 16 år i forlengelsen, forklares det at barnetillegget stopper for dem.
 * Hadde vedtaket barnetillegg for bare ett barn, oppgis i stedet siste dag med barnetillegg.
 * Det er siste innvilgede dag i det gjeldende vedtaket dersom barnet fyller 16 år dagen etter.
 * Barn kan bare falle bort i forlengelsen, se [utledAntallBarn], så antall barn som fyller 16 år er differansen mellom vedtaket og slutten av forlengelsen.
 */
private fun lagFritekstTilVedtaksbrev(
    sisteInnvilgedeDag: LocalDate,
    antallBarnIVedtaket: AntallBarn,
    antallBarn: SammenhengendePeriodisering<AntallBarn>,
): FritekstTilVedtaksbrev {
    val harBarnetillegg = !antallBarn.inneholderKun(AntallBarn.ZERO)
    val antallBarnSomFyller16 = antallBarnIVedtaket.value - antallBarn.perioderMedVerdi.last().verdi.value
    val forlengelse = "Tiltaket ditt er forlenget. " +
        "Vi forlenger derfor vedtaket ditt om tiltakspenger${if (harBarnetillegg) " og barnetillegg" else ""}."
    val barnFyller16 = when {
        antallBarnSomFyller16 == 0 -> null

        antallBarnIVedtaket.value == 1 -> {
            val sisteDagMedBarnetillegg = antallBarn.perioderMedVerdi.lastOrNull { it.verdi != AntallBarn.ZERO }?.periode?.tilOgMed
                ?: sisteInnvilgedeDag
            "Du er innvilget barnetillegg til og med ${sisteDagMedBarnetillegg.format(norskDatoFormatter)} " +
                "fordi du ikke har rett til barnetillegg for barn over 16 år. " +
                "Vi viser til forskriften om tiltakspenger § 3."
        }

        antallBarnSomFyller16 == 1 ->
            "Barnetillegg gis for hvert barn under 16 år. " +
                "Ett av dine barn fyller 16 år i tiltaksperioden. " +
                "Barnetillegget for dette barnet vil derfor stoppe fra den dagen barnet ditt fyller 16 år. " +
                "Vi viser til forskriften om tiltakspenger § 3."

        else ->
            "Barnetillegg gis for hvert barn under 16 år. " +
                "${antallBarnSomFyller16.tilTallord()} av dine barn fyller 16 år i tiltaksperioden. " +
                "Barnetillegget for disse barna vil derfor stoppe fra den dagen hvert av barna fyller 16 år. " +
                "Vi viser til forskriften om tiltakspenger § 3."
    }
    return FritekstTilVedtaksbrev.create(listOfNotNull(forlengelse, barnFyller16).joinToString("\n\n"))!!
}

private fun Int.tilTallord(): String = listOf("Null", "Ett", "To", "Tre", "Fire", "Fem", "Seks", "Sju", "Åtte", "Ni", "Ti").getOrNull(this) ?: toString()
