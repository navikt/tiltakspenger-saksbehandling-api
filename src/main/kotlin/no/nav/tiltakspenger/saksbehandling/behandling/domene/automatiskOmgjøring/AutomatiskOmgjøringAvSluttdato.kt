package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskOmgjøring

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
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
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
import java.time.LocalDate

/**
 * Det systemet har utledet for å kunne fylle ut en automatisk opprettet omgjøring når sluttdatoen for en tiltaksdeltakelse er avkortet, uten at deltakelsen er avsluttet.
 *
 * @param vedtaksperiode Perioden som omgjøres, se [finnOmgjøringsperiodeVedAvkortetSluttdato].
 * Delene av vedtaksperioden som ikke innvilges, opphøres.
 * @param innvilgelsesperioder De gjeldende innvilgelsesperiodene i vedtaksperioden, der deltakelsen ikke lenger innvilges etter ny sluttdato.
 * Antall dager per meldeperiode og andre deltakelser videreføres fra vedtakene.
 * @param barnetillegg Det gjeldende barnetillegget, krympet til [innvilgelsesperioder].
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
 * Utleder verdiene for å omgjøre vedtakene uten saksbehandler, når sluttdatoen for [tiltaksdeltakerId] er avkortet og deltakelsen fortsatt har status [TiltakDeltakerstatus.Deltar].
 * Gir alle grunnene til at omgjøringen må behandles manuelt dersom verdiene ikke kan utledes med stor sikkerhet.
 * Sjekker som bygger på en verdi som ikke kunne utledes, hoppes over, slik at saksbehandler ikke får følgefeil.
 *
 * Vedtaksperioden går fra starten av vedtaket som innvilger ny sluttdato til slutten av det siste vedtaket som innvilger deltakelsen etter den, se [finnOmgjøringsperiodeVedAvkortetSluttdato].
 * Ny sluttdato kan altså være innvilget i et tidligere vedtak enn de avkortede dagene, og ny sluttdato kan være passert.
 * Deltakelsen innvilges til og med ny sluttdato, og resten av vedtaksperioden den var innvilget i, opphøres.
 * Innvilgelsene av andre deltakelser, antall dager per meldeperiode og barnetillegget videreføres uendret fra de gjeldende vedtakene.
 *
 * Deltakelsen slik den er registrert nå, hentes fra [saksopplysninger], slik at det er de samme opplysningene beslutter ser.
 * Endringer som har kommet etter at omgjøringen ble opprettet, fanges dermed opp her, og vedtaksperioden utledes på nytt fra dem.
 * Er deltakelsen avsluttet i mellomtiden, er en stans eller et opphør riktig, og det overlates til saksbehandler.
 * Startdatoen og deltakelsesmengden må være uendret fra alle vedtakene i vedtaksperioden, siden omgjøringen bare avkorter innvilgelsen.
 * Gir de ferske saksopplysningene en annen innvilgelse enn den utledede, f.eks. fordi en annen deltakelse i vedtakene er endret, overlates omgjøringen også til saksbehandler.
 *
 * Vurderingen er bevisst konservativ, og alt som ikke er entydig overlates til saksbehandler.
 *
 * @param saksopplysninger Saksopplysningene til omgjøringen, hentet på nytt før vurderingen.
 */
fun Sak.utledAutomatiskOmgjøringAvSluttdato(
    tiltaksdeltakerId: TiltaksdeltakerId,
    saksopplysninger: Saksopplysninger,
): Either<NonEmptyList<ManueltBehandlesGrunn>, AutomatiskOmgjøringAvSluttdato> {
    val grunner = mutableListOf<ManueltBehandlesGrunn>()

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

    val vedtaksperiode = nySluttdato?.let {
        rammevedtaksliste.finnOmgjøringsperiodeVedAvkortetSluttdato(tiltaksdeltakerId, it)
            .onLeft { grunn -> grunner.add(grunn) }
            .getOrNull()
            ?.vedtaksperiode
    }

    // Deltakelsen er innvilget på ny sluttdato når vedtaksperioden er funnet, så innvilgelsen i perioden er ikke tom.
    val gjeldendeInnvilgelse = vedtaksperiode?.let {
        Innvilgelsesperioder(rammevedtaksliste.innvilgelsesperioder.krymp(it).perioderMedVerdi.tilIkkeTomPeriodisering())
    }
    val tidligereDeltakelser = gjeldendeInnvilgelse?.periodisering?.perioderMedVerdi
        ?.map { it.verdi.valgtTiltaksdeltakelse }
        ?.filter { it.internDeltakelseId == tiltaksdeltakerId }
        .orEmpty()

    if (nåtilstand != null && tidligereDeltakelser.any { it.deltakelseFraOgMed != nåtilstand.deltakelseFraOgMed }) {
        grunner.add(ManueltBehandlesGrunn.OMGJORING_ENDRET_STARTDATO)
    }

    if (nåtilstand != null &&
        tidligereDeltakelser.any { !it.tilDeltakelsestilstand().harSammeDeltakelsesmengde(nåtilstand.tilDeltakelsestilstand()) }
    ) {
        grunner.add(ManueltBehandlesGrunn.OMGJORING_ENDRET_DELTAKELSESMENGDE)
    }

    val innvilgelsesperioder = if (gjeldendeInnvilgelse != null && nySluttdato != null) {
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
        barnetillegg = rammevedtaksliste.gjeldendeBarnetillegg(vedtaksperiode)
            ?.krympTilPerioder(innvilgelsesperioder.perioder)
            ?: Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder),
        begrunnelse = lagBegrunnelse(
            tidligereSluttdato = tidligereDeltakelser.last().deltakelseTilOgMed!!,
            nySluttdato = nySluttdato!!,
        ),
        fritekstTilVedtaksbrev = FritekstTilVedtaksbrev.create(FRITEKST_TIL_VEDTAKSBREV_VED_AVKORTET_SLUTTDATO)!!,
    ).right()
}

/**
 * Det gjeldende barnetillegget i [vedtaksperiode], sammenstilt fra vedtakene som gjelder i perioden.
 * Begrunnelsen hentes fra det sist opprettede vedtaket i perioden som har en begrunnelse for barnetillegget.
 * Gir null dersom ingen av vedtakene har barnetillegg.
 */
private fun Rammevedtaksliste.gjeldendeBarnetillegg(vedtaksperiode: Periode): Barnetillegg? {
    val barnetilleggIVedtakene = innvilgetTidslinje.krymp(vedtaksperiode).perioderMedVerdi
        .map { it.verdi }
        .sortedBy { it.opprettet }
        .mapNotNull { it.barnetillegg }
        .ifEmpty { return null }
    return Barnetillegg(
        periodisering = barnetilleggsperioder.krymp(vedtaksperiode).perioderMedVerdi.tilIkkeTomPeriodisering(),
        begrunnelse = barnetilleggIVedtakene.mapNotNull { it.begrunnelse }.lastOrNull(),
    )
}

/**
 * Fjerner dagene etter [nySluttdato] fra periodene der [tiltaksdeltakerId] er innvilget.
 * Perioder som starter etter ny sluttdato, f.eks. fra et senere vedtak, fjernes helt.
 * Periodene der andre deltakelser er innvilget, beholdes.
 */
private fun Innvilgelsesperioder.avkortForDeltakelse(
    tiltaksdeltakerId: TiltaksdeltakerId,
    nySluttdato: LocalDate,
): Innvilgelsesperioder = Innvilgelsesperioder(
    periodisering.perioderMedVerdi.mapNotNull { periodeMedVerdi ->
        val periode = periodeMedVerdi.periode
        when {
            periodeMedVerdi.verdi.valgtTiltaksdeltakelse.internDeltakelseId != tiltaksdeltakerId -> periodeMedVerdi
            periode.fraOgMed.isAfter(nySluttdato) -> null
            else -> PeriodeMedVerdi(verdi = periodeMedVerdi.verdi, periode = periode.fraOgMed til minOf(periode.tilOgMed, nySluttdato))
        }
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
