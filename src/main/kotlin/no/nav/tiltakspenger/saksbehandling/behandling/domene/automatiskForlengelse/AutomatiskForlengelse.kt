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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
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

sealed interface KanIkkeForlengeAutomatisk : Loggbar {
    data object ErIkkeAutomatiskOpprettetInnvilgelse : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("revurderingen er ikke en automatisk opprettet innvilgelse")
    }

    data class BehandlingenErIkkeKlarTilAutomatiskBehandling(val status: Rammebehandlingsstatus) : KanIkkeForlengeAutomatisk {
        override val loggkontekst get() = Loggkontekst("revurderingen har status $status, er tildelt en saksbehandler, står på vent eller er knyttet til en klage")
    }

    data object ErIkkeEnRenForlengelse : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("endringene i tiltaksdeltakelsen er ikke en ren forlengelse")
    }

    data object HarAndreÅpneBehandlinger : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("saken har andre åpne behandlinger")
    }

    data object FantIkkeGjeldendeVedtak : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("fant ikke et gjeldende vedtak som innvilger for tiltaksdeltakelsen")
    }

    data object VedtaketErStansetEllerOpphørt : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("det gjeldende vedtaket er helt eller delvis stanset, opphørt eller omgjort i slutten av innvilgelsesperioden")
    }

    data object InnvilgelsenSlutterFørDeltakelsen : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("det gjeldende vedtaket innvilget ikke frem til den tidligere sluttdatoen for deltakelsen")
    }

    data object ForlengerIkkeGjeldendeVedtak : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("ny sluttdato er ikke etter siste innvilgede dag i det gjeldende vedtaket")
    }

    data object FantIkkeTiltaksdeltakelse : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("fant ikke tiltaksdeltakelsen i saksopplysningene til revurderingen")
    }

    data class DeltakelsenHarUgyldigStatus(val status: TiltakDeltakerstatus) : KanIkkeForlengeAutomatisk {
        override val loggkontekst get() = Loggkontekst("deltakelsen har status $status")
    }

    data object TiltaketGirIkkeRett : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("tiltaksdeltakelsen kan ikke innvilges")
    }

    data object SaksopplysningeneAvvikerFraEndringen : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("periode eller deltakelsesmengde i saksopplysningene samsvarer ikke med forlengelsen og det gjeldende vedtaket")
    }

    data object OverlapperAndreVedtak : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("forlengelsesperioden overlapper andre vedtak på saken")
    }

    data object OverlapperAndreTiltaksdeltakelser : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("forlengelsesperioden overlapper andre tiltaksdeltakelser")
    }

    data object HarAndreYtelser : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("saksopplysningene har andre ytelser enn tiltakspenger")
    }

    data object HarVedtakIArena : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("saksopplysningene har tiltakspengevedtak fra Arena")
    }

    data object HarBarnetillegg : KanIkkeForlengeAutomatisk {
        override val loggkontekst = Loggkontekst("det gjeldende vedtaket har barnetillegg ved slutten av innvilgelsesperioden")
    }

    /**
     * Vurderingen gikk gjennom, men et av stegene for å ta, fylle ut eller sende revurderingen til beslutning feilet.
     * [årsak] skal være PII-fri, f.eks. klassenavnet til den underliggende feilen.
     */
    data class FeilVedAutomatiskBehandling(val steg: String, val årsak: String) : KanIkkeForlengeAutomatisk {
        override val loggkontekst get() = Loggkontekst("feil ved $steg: $årsak")
    }
}

/**
 * Vurderer om systemet har nok informasjon til å fylle ut en automatisk opprettet forlengelse selv.
 *
 * En forlengelse kan automatiseres når tiltaksdeltakelsen er utvidet fremover i tid fra et gjeldende vedtak, og vedtaket ikke er stanset eller opphørt.
 * Det gjeldende vedtaket er det siste vedtaket i den innvilgede tidslinjen som innvilger for [tiltaksdeltakerId].
 * Innvilgelsen må fortsatt gjelde helt frem til vedtakets opprinnelige sluttdato, og den må ha gått frem til deltakelsens tidligere sluttdato.
 * Forlengelsen innvilges fra dagen etter, til ny sluttdato, med samme antall dager per meldeperiode som i det gjeldende vedtaket.
 *
 * Vurderingen er bevisst konservativ, og alt som ikke er entydig overlates til saksbehandler.
 * Blant annet overlates forlengelser med barnetillegg, siden vi ikke vurderer barnas alder i den nye perioden ennå.
 * Periode, status og deltakelsesmengde kontrolleres mot saksopplysningene på revurderingen, slik at det er de samme opplysningene beslutter ser.
 */
fun Sak.utledAutomatiskForlengelse(
    revurdering: Revurdering,
    tiltaksdeltakerId: TiltaksdeltakerId,
): Either<KanIkkeForlengeAutomatisk, AutomatiskForlengelse> {
    val automatiskOpprettetGrunn = revurdering.automatiskOpprettetGrunn
    if (revurdering.resultat !is Revurderingsresultat.Innvilgelse || automatiskOpprettetGrunn == null) {
        return KanIkkeForlengeAutomatisk.ErIkkeAutomatiskOpprettetInnvilgelse.left()
    }

    if (revurdering.status != Rammebehandlingsstatus.KLAR_TIL_BEHANDLING ||
        revurdering.saksbehandler != null ||
        revurdering.ventestatus.erSattPåVent ||
        revurdering.klagebehandling != null
    ) {
        return KanIkkeForlengeAutomatisk.BehandlingenErIkkeKlarTilAutomatiskBehandling(revurdering.status).left()
    }

    // Kombinasjoner med andre endringer, f.eks. endret deltakelsesmengde, krever en vurdering fra saksbehandler.
    val forlengelse = automatiskOpprettetGrunn.endringer.forlengelse
    if (forlengelse == null || automatiskOpprettetGrunn.endringer.size != 1) {
        return KanIkkeForlengeAutomatisk.ErIkkeEnRenForlengelse.left()
    }

    if (rammebehandlinger.åpneBehandlinger.any { it.id != revurdering.id }) {
        return KanIkkeForlengeAutomatisk.HarAndreÅpneBehandlinger.left()
    }

    val (gjeldendeVedtak, gjeldendePeriode) = rammevedtaksliste.innvilgetTidslinje.perioderMedVerdi
        .filter { (vedtak, periode) ->
            vedtak.valgteTiltaksdeltakelser?.hentVerdiForDag(periode.tilOgMed)?.internDeltakelseId == tiltaksdeltakerId
        }
        .maxByOrNull { it.periode.tilOgMed }
        ?: return KanIkkeForlengeAutomatisk.FantIkkeGjeldendeVedtak.left()

    val sisteInnvilgedeDag = gjeldendePeriode.tilOgMed

    // Er slutten av innvilgelsen erstattet av et senere vedtak, er vedtaket stanset, opphørt eller omgjort.
    if (sisteInnvilgedeDag != gjeldendeVedtak.innvilgelsesperioder!!.tilOgMed) {
        return KanIkkeForlengeAutomatisk.VedtaketErStansetEllerOpphørt.left()
    }

    val tidligereDeltakelse = gjeldendeVedtak.valgteTiltaksdeltakelser!!.hentVerdiForDag(sisteInnvilgedeDag)!!
    if (tidligereDeltakelse.deltakelseTilOgMed != sisteInnvilgedeDag) {
        return KanIkkeForlengeAutomatisk.InnvilgelsenSlutterFørDeltakelsen.left()
    }

    val nySluttdato = forlengelse.nySluttdato
    if (!nySluttdato.isAfter(sisteInnvilgedeDag)) {
        return KanIkkeForlengeAutomatisk.ForlengerIkkeGjeldendeVedtak.left()
    }

    val deltakelse = revurdering.getTiltaksdeltakelse(tiltaksdeltakerId)
        ?: return KanIkkeForlengeAutomatisk.FantIkkeTiltaksdeltakelse.left()

    if (!deltakelse.deltakelseStatus.deltarEllerHarDeltatt()) {
        return KanIkkeForlengeAutomatisk.DeltakelsenHarUgyldigStatus(deltakelse.deltakelseStatus).left()
    }

    if (!revurdering.saksopplysninger.kanInnvilges(tiltaksdeltakerId)) {
        return KanIkkeForlengeAutomatisk.TiltaketGirIkkeRett.left()
    }

    if (deltakelse.deltakelseFraOgMed != tidligereDeltakelse.deltakelseFraOgMed ||
        deltakelse.deltakelseTilOgMed != nySluttdato ||
        deltakelse.antallDagerPerUke != tidligereDeltakelse.antallDagerPerUke ||
        deltakelse.deltakelseProsent != tidligereDeltakelse.deltakelseProsent
    ) {
        return KanIkkeForlengeAutomatisk.SaksopplysningeneAvvikerFraEndringen.left()
    }

    val innvilgelsesperiode = sisteInnvilgedeDag.plusDays(1) til nySluttdato

    if (rammevedtaksliste.tidslinje.overlapper(innvilgelsesperiode)) {
        return KanIkkeForlengeAutomatisk.OverlapperAndreVedtak.left()
    }

    if (revurdering.saksopplysninger.harOverlappendeTiltaksdeltakelse(tiltaksdeltakerId, innvilgelsesperiode)) {
        return KanIkkeForlengeAutomatisk.OverlapperAndreTiltaksdeltakelser.left()
    }

    if (revurdering.saksopplysninger.harAndreYtelserEnnTiltakspenger()) {
        return KanIkkeForlengeAutomatisk.HarAndreYtelser.left()
    }

    if (revurdering.saksopplysninger.harTiltakspengevedtakFraArena()) {
        return KanIkkeForlengeAutomatisk.HarVedtakIArena.left()
    }

    val antallBarn = rammevedtaksliste.barnetilleggsperioder.hentVerdiForDag(sisteInnvilgedeDag) ?: AntallBarn.ZERO
    if (antallBarn != AntallBarn.ZERO) {
        return KanIkkeForlengeAutomatisk.HarBarnetillegg.left()
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
