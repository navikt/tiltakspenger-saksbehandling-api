package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskOmgjøring

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.leggSammen
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.harSammeDeltakelsesmengde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.tilDeltakelsestilstand
import java.time.LocalDate

/**
 * Det systemet har utledet for å kunne fylle ut en automatisk opprettet omgjøring når sluttdatoen for en tiltaksdeltakelse er flyttet bakover i tid, uten at deltakelsen er avsluttet.
 *
 * @param vedtaksperiode Hele den gjeldende perioden til vedtaket som omgjøres.
 * Delene av vedtaksperioden som ikke innvilges, opphøres.
 * @param innvilgelsesperioder De gjeldende innvilgelsesperiodene i vedtaket, der deltakelsen ikke lenger innvilges etter ny sluttdato.
 * Antall dager per meldeperiode og andre deltakelser videreføres fra vedtaket.
 * @param barnetillegg Barnetillegget i vedtaket, krympet til [innvilgelsesperioder].
 * @param begrunnelse Begrunnelsen for vilkårsvurderingen.
 * @param fritekstTilVedtaksbrev Forklarer omgjøringen for bruker i vedtaksbrevet.
 */
data class AutomatiskOmgjøringAvSluttdato(
    val vedtaksperiode: Periode,
    val innvilgelsesperioder: IkkeTomPeriodisering<InnvilgelsesperiodeKommando>,
    val barnetillegg: Barnetillegg,
    val begrunnelse: Begrunnelse,
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev,
)

/**
 * Utleder verdiene for å omgjøre vedtaket [omgjørVedtakId] uten saksbehandler, når sluttdatoen for [tiltaksdeltakerId] er flyttet bakover i tid og deltakelsen fortsatt har status [TiltakDeltakerstatus.Deltar].
 * Gir alle grunnene til at omgjøringen må behandles manuelt dersom verdiene ikke kan utledes med stor sikkerhet.
 * Sjekker som bygger på en verdi som ikke kunne utledes, hoppes over, slik at saksbehandler ikke får følgefeil.
 *
 * Hele den gjeldende perioden til vedtaket omgjøres.
 * Deltakelsen innvilges til og med ny sluttdato, og resten av perioden den var innvilget i, opphøres.
 * Innvilgelsene av andre deltakelser i vedtaket, antall dager per meldeperiode og barnetillegget videreføres uendret.
 *
 * Deltakelsen slik den er registrert nå, hentes fra [saksopplysninger], slik at det er de samme opplysningene beslutter ser.
 * Endringer som har kommet etter at omgjøringen ble opprettet, fanges dermed opp her.
 * Er deltakelsen avsluttet i mellomtiden, er en stans eller et opphør riktig, og det overlates til saksbehandler.
 * Startdatoen og deltakelsesmengden må være uendret fra vedtaket, siden omgjøringen bare avkorter innvilgelsen.
 * Gir de ferske saksopplysningene en annen innvilgelse enn den utledede, f.eks. fordi en annen deltakelse i vedtaket er endret, overlates omgjøringen også til saksbehandler.
 *
 * Vurderingen er bevisst konservativ, og alt som ikke er entydig overlates til saksbehandler.
 *
 * @param saksopplysninger Saksopplysningene til omgjøringen, hentet på nytt før vurderingen.
 */
fun Sak.utledAutomatiskOmgjøringAvSluttdato(
    tiltaksdeltakerId: TiltaksdeltakerId,
    omgjørVedtakId: VedtakId,
    saksopplysninger: Saksopplysninger,
    iDag: LocalDate,
): Either<NonEmptyList<ManueltBehandlesGrunn>, AutomatiskOmgjøringAvSluttdato> {
    val grunner = mutableListOf<ManueltBehandlesGrunn>()

    val vedtak = rammevedtaksliste.hentRammevedtakForId(omgjørVedtakId)
    val vedtaksperiode = vedtak.gjeldendeTotalPeriode
    val gjeldendeInnvilgelse = vedtak.innvilgelsesperioder?.krymp(vedtak.gjeldendeInnvilgetPerioder)
    val innvilgetForDeltakelse = gjeldendeInnvilgelse?.periodisering?.perioderMedVerdi
        ?.filter { it.verdi.valgtTiltaksdeltakelse.internDeltakelseId == tiltaksdeltakerId }
        .orEmpty()
    if (vedtaksperiode == null || innvilgetForDeltakelse.isEmpty()) {
        grunner.add(ManueltBehandlesGrunn.OMGJORING_VEDTAKET_INNVILGER_IKKE_DELTAKELSEN)
    }

    if (vedtaksperiode != null && vedtaksliste.finnRammevedtakSomOmgjøres(vedtaksperiode).rammevedtakIDer != listOf(omgjørVedtakId)) {
        grunner.add(ManueltBehandlesGrunn.OMGJORING_OMGJOR_FLERE_VEDTAK)
    }

    val nåtilstand = saksopplysninger.getTiltaksdeltakelse(tiltaksdeltakerId)
    if (nåtilstand == null) {
        grunner.add(ManueltBehandlesGrunn.OMGJORING_FANT_IKKE_TILTAKSDELTAKELSE)
    }

    if (nåtilstand != null && nåtilstand.deltakelseStatus != TiltakDeltakerstatus.Deltar) {
        grunner.add(ManueltBehandlesGrunn.OMGJORING_DELTAKELSEN_HAR_IKKE_STATUS_DELTAR)
    }

    if (nåtilstand != null && !nåtilstand.rettPåTiltakspenger) {
        grunner.add(ManueltBehandlesGrunn.OMGJORING_TILTAKET_GIR_IKKE_RETT)
    }

    val nySluttdato = nåtilstand?.deltakelseTilOgMed
    if (nåtilstand != null && nySluttdato == null) {
        grunner.add(ManueltBehandlesGrunn.OMGJORING_DELTAKELSEN_MANGLER_SLUTTDATO)
    }

    if (nySluttdato != null && nySluttdato.isAfter(iDag)) {
        grunner.add(ManueltBehandlesGrunn.OMGJORING_SLUTTDATO_ER_IKKE_PASSERT)
    }

    val førsteInnvilgedeDag = innvilgetForDeltakelse.firstOrNull()?.periode?.fraOgMed
    val sisteInnvilgedeDag = innvilgetForDeltakelse.lastOrNull()?.periode?.tilOgMed
    if (sisteInnvilgedeDag != null && nySluttdato != null && !nySluttdato.isBefore(sisteInnvilgedeDag)) {
        grunner.add(ManueltBehandlesGrunn.OMGJORING_SLUTTDATO_ER_IKKE_FOR_INNVILGELSEN_SLUTTER)
    }

    if (førsteInnvilgedeDag != null && nySluttdato != null && nySluttdato.isBefore(førsteInnvilgedeDag)) {
        grunner.add(ManueltBehandlesGrunn.OMGJORING_SLUTTDATO_ER_FOR_INNVILGELSEN_STARTER)
    }

    val tidligereDeltakelse = innvilgetForDeltakelse.lastOrNull()?.verdi?.valgtTiltaksdeltakelse
    if (tidligereDeltakelse != null && nåtilstand != null && nåtilstand.deltakelseFraOgMed != tidligereDeltakelse.deltakelseFraOgMed) {
        grunner.add(ManueltBehandlesGrunn.OMGJORING_ENDRET_STARTDATO)
    }

    if (tidligereDeltakelse != null &&
        nåtilstand != null &&
        !tidligereDeltakelse.tilDeltakelsestilstand().harSammeDeltakelsesmengde(nåtilstand.tilDeltakelsestilstand())
    ) {
        grunner.add(ManueltBehandlesGrunn.OMGJORING_ENDRET_DELTAKELSESMENGDE)
    }

    val innvilgelsesperioder = if (
        gjeldendeInnvilgelse != null &&
        førsteInnvilgedeDag != null &&
        nySluttdato != null &&
        !nySluttdato.isBefore(førsteInnvilgedeDag)
    ) {
        gjeldendeInnvilgelse.avkortForDeltakelse(tiltaksdeltakerId, nySluttdato)
    } else {
        null
    }

    // Omgjøringen oppdaterer alle deltakelsene i innvilgelsen fra saksopplysningene.
    // Gir det andre perioder enn de utledede, er noe annet enn sluttdatoen endret, og det må saksbehandler vurdere.
    if (innvilgelsesperioder != null &&
        innvilgelsesperioder.oppdaterTiltaksdeltakelser(saksopplysninger.tiltaksdeltakelser)?.perioder?.leggSammen() !=
        innvilgelsesperioder.perioder.leggSammen()
    ) {
        grunner.add(ManueltBehandlesGrunn.OMGJORING_ANDRE_ENDRINGER_I_TILTAKSDELTAKELSENE)
    }

    grunner.toNonEmptyListOrNull()?.let { return it.left() }

    // Uten grunner til manuell behandling er alle verdiene utledet.
    return AutomatiskOmgjøringAvSluttdato(
        vedtaksperiode = vedtaksperiode!!,
        innvilgelsesperioder = innvilgelsesperioder!!.tilKommando(),
        barnetillegg = vedtak.barnetillegg?.krympTilPerioder(innvilgelsesperioder.perioder)
            ?: Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder),
        begrunnelse = lagBegrunnelse(
            tidligereSluttdato = tidligereDeltakelse!!.deltakelseTilOgMed!!,
            nySluttdato = nySluttdato!!,
        ),
        fritekstTilVedtaksbrev = FritekstTilVedtaksbrev.create(FRITEKST_TIL_VEDTAKSBREV_VED_AVKORTET_SLUTTDATO)!!,
    ).right()
}

/**
 * Fjerner dagene etter [nySluttdato] fra periodene der [tiltaksdeltakerId] er innvilget.
 * Periodene der andre deltakelser er innvilget, beholdes.
 */
private fun Innvilgelsesperioder.avkortForDeltakelse(
    tiltaksdeltakerId: TiltaksdeltakerId,
    nySluttdato: LocalDate,
): Innvilgelsesperioder = Innvilgelsesperioder(
    periodisering.perioderMedVerdi.mapNotNull { periodeMedVerdi ->
        if (periodeMedVerdi.verdi.valgtTiltaksdeltakelse.internDeltakelseId != tiltaksdeltakerId) {
            return@mapNotNull periodeMedVerdi
        }
        periodeMedVerdi.periode.overlappendePeriode(periodeMedVerdi.periode.fraOgMed til nySluttdato)
            ?.let { PeriodeMedVerdi(verdi = periodeMedVerdi.verdi, periode = it) }
    }.tilIkkeTomPeriodisering(),
)

private fun Innvilgelsesperioder.tilKommando(): IkkeTomPeriodisering<InnvilgelsesperiodeKommando> =
    periodisering.perioderMedVerdi.map {
        PeriodeMedVerdi(
            verdi = InnvilgelsesperiodeKommando(
                periode = it.periode,
                antallDagerPerMeldeperiode = it.verdi.antallDagerPerMeldeperiode,
                internDeltakelseId = it.verdi.valgtTiltaksdeltakelse.internDeltakelseId,
            ),
            periode = it.periode,
        )
    }.tilIkkeTomPeriodisering()

private fun lagBegrunnelse(
    tidligereSluttdato: LocalDate,
    nySluttdato: LocalDate,
): Begrunnelse = Begrunnelse.create(
    "Automatisk behandlet.\n" +
        "Sluttdatoen for tiltaksdeltakelsen er endret fra ${tidligereSluttdato.format(norskDatoFormatter)} " +
        "til ${nySluttdato.format(norskDatoFormatter)}, og deltakelsen har fortsatt status deltar.\n" +
        "Vedtaket omgjøres slik at tiltakspenger innvilges til og med ny sluttdato.",
)!!

const val FRITEKST_TIL_VEDTAKSBREV_VED_AVKORTET_SLUTTDATO =
    "Vi har fått opplysninger om at sluttdato for tiltaket er endret. " +
        "Du er innvilget tiltakspenger ut perioden du deltar i tiltaket."
