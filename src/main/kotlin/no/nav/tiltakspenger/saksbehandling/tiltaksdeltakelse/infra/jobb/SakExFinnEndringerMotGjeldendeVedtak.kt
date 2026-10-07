package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.VurdertTiltaksdeltakerEndring.Endret
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.VurdertTiltaksdeltakerEndring.IngenEndring
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.Clock
import java.time.LocalDate

/**
 * Sammenligner nå-tilstanden fra tiltakshistorikk med deltakelsen slik den er innvilget i de gjeldende vedtakene på saken, og tolker forskjellen til ett utfall.
 * Hvert felt sammenlignes med det gjeldende vedtaket som styrer den delen av tidslinjen feltet påvirker, se [GjeldendeInnvilgelse].
 * Kun endringer som kan påvirke retten i de gjeldende vedtakene regnes som relevante.
 * Endringen vurderes også opp mot hvilken revurdering som kan opprettes automatisk, se [vurderAutomatiskRevurdering].
 *
 * Åpne manuelle behandlinger med deltakelsen tas med som kjent tilstand, se [deltakelseISisteÅpneManuelleBehandling].
 * Kjenner den nyeste av dem allerede nå-tilstanden, er endringen fanget opp der, og det gis [VurdertTiltaksdeltakerEndring.IngenEndring].
 * Er deltakelsen ikke innvilget i noe gjeldende vedtak, sammenlignes nå-tilstanden med den åpne behandlingen i stedet, og endringen må følges opp manuelt.
 *
 * Gir [VurdertTiltaksdeltakerEndring.IngenEndring] dersom deltakelsen verken er innvilget i noe gjeldende vedtak eller er med i en åpen manuell behandling, eller dersom ingenting relevant er endret.
 * Det gjelder også når deltakelsen har vært innvilget, men er stanset, opphørt eller erstattet av en annen deltakelse i alle periodene.
 */
fun Sak.finnEndringerForDeltakelse(
    tiltaksdeltakerId: TiltaksdeltakerId,
    oppdatertDeltakelse: Tiltaksdeltakelse.GirRett,
    clock: Clock,
): VurdertTiltaksdeltakerEndring {
    val iDag = LocalDate.now(clock)
    val nåtilstand = oppdatertDeltakelse.tilDeltakelsestilstand(clock)

    val endringMotÅpenBehandling = deltakelseISisteÅpneManuelleBehandling(tiltaksdeltakerId)
        ?.let {
            it.tilDeltakelsestilstand().finnEndringer(nåtilstand, clock)
                ?: return IngenEndring
        }

    val gjeldendeInnvilgelse = gjeldendeVedtakForDeltakelse(tiltaksdeltakerId).tilGjeldendeInnvilgelse(iDag)
        ?: return endringMotÅpenBehandling?.let { Endret(endring = it, automatiskRevurdering = null) } ?: IngenEndring

    val endring = gjeldendeInnvilgelse.finnEndringer(nåtilstand, iDag)
        ?: endringMotÅpenBehandling
        ?: return IngenEndring

    return Endret(
        endring = endring,
        automatiskRevurdering = vurderAutomatiskRevurdering(
            tiltaksdeltakerId,
            endring,
            nåtilstand,
            gjeldendeInnvilgelse,
            iDag,
        ),
    )
}

/**
 * Tilstanden deltakelsen har i den sist endrede åpne behandlingen som har den med i saksopplysningene.
 * Behandlinger under automatisk behandling tas ikke med, siden de vurderes på nytt når de vekkes.
 */
private fun Sak.deltakelseISisteÅpneManuelleBehandling(tiltaksdeltakerId: TiltaksdeltakerId): TiltaksdeltakelseIntern? =
    rammebehandlinger.åpneBehandlinger
        .filter { !it.erUnderAutomatiskBehandling }
        .mapNotNull { behandling ->
            behandling.getTiltaksdeltakelse(tiltaksdeltakerId)?.let { behandling.sistEndret to it }
        }
        .maxByOrNull { (sistEndret) -> sistEndret }
        ?.second

/**
 * @param deltakelse Tilstanden deltakelsen sist ble innvilget med i perioden.
 * @param gjeldendeVedtak Vedtaket som gjelder for perioden i dag.
 * Kan være en innvilgelse av denne eller en annen deltakelse, en stans eller et opphør.
 * @param erInnvilgetIGjeldendeVedtak Om det gjeldende vedtaket innvilger denne deltakelsen i perioden.
 * Er den true, er [deltakelse] tilstanden fra det gjeldende vedtaket, siden det er det nyeste vedtaket for perioden.
 */
private data class VedtakForDeltakelse(
    val deltakelse: Deltakelsestilstand,
    val gjeldendeVedtak: Rammevedtak,
    val erInnvilgetIGjeldendeVedtak: Boolean,
)

/**
 * Nåtilstanden for vedtakene i alle perioder der deltakelsen er eller har vært innvilget.
 * Periodene er unionen av innvilgelsesperiodene der deltakelsen er valgt, på tvers av alle innvilgelsesvedtak, også de som senere er omgjort, stanset eller opphørt.
 * Gjeldende vedtak hentes fra vedtakstidslinjen, som ikke inkluderer avslag.
 * Periodene deles opp slik at hver periode enten er innvilget for deltakelsen i det gjeldende vedtaket, eller ikke.
 * Gir en tom periodisering dersom deltakelsen aldri har vært innvilget.
 */
private fun Sak.gjeldendeVedtakForDeltakelse(tiltaksdeltakerId: TiltaksdeltakerId): Periodisering<VedtakForDeltakelse> {
    // Vedtakslisten er sortert på opprettet, så den sist innvilgede tilstanden vinner der periodene overlapper.
    val sistInnvilgetDeltakelse: Periodisering<TiltaksdeltakelseIntern> =
        rammevedtaksliste
            .filter { it.rammebehandlingsresultat is Rammebehandlingsresultat.Innvilgelse }
            .flatMap { vedtak ->
                vedtak.valgteTiltaksdeltakelser!!.perioderMedVerdi.filter {
                    it.verdi.internDeltakelseId == tiltaksdeltakerId
                }
            }
            .fold(Periodisering.empty<TiltaksdeltakelseIntern>() as Periodisering<TiltaksdeltakelseIntern>) { tidslinje, (deltakelse, periode) ->
                tidslinje.setVerdiForDelperiode(deltakelse, periode)
            }

    return sistInnvilgetDeltakelse.flatMap { (deltakelse, periode) ->
        rammevedtaksliste.tidslinje.krymp(periode).perioderMedVerdi.flatMap { (gjeldendeVedtak, vedtaksperiode) ->
            val innvilgetForDeltakelse = gjeldendeVedtak.valgteTiltaksdeltakelser
                ?.krymp(vedtaksperiode)
                ?.filter { it.verdi.internDeltakelseId == tiltaksdeltakerId }
                ?.perioder
                .orEmpty()
            val ikkeInnvilgetForDeltakelse = vedtaksperiode.trekkFra(innvilgetForDeltakelse)

            fun vedtakForDeltakelse(erInnvilget: Boolean) = VedtakForDeltakelse(
                deltakelse = deltakelse.tilDeltakelsestilstand(),
                gjeldendeVedtak = gjeldendeVedtak,
                erInnvilgetIGjeldendeVedtak = erInnvilget,
            )

            innvilgetForDeltakelse.map { PeriodeMedVerdi(vedtakForDeltakelse(true), it) }
                .plus(
                    ikkeInnvilgetForDeltakelse.map { PeriodeMedVerdi(vedtakForDeltakelse(false), it) },
                )
                .sortedBy { it.periode.fraOgMed }
        }
    }
}

/**
 * Det endringene i deltakelsen sammenlignes med, utledet fra periodene der deltakelsen er innvilget i gjeldende vedtak.
 * @param innvilgedePerioder Periodene deltakelsen er innvilget i gjeldende vedtak, sortert og ikke tom.
 * @param perioderForDeltakelsesmengde Periodene deltakelsesmengden sammenlignes med.
 * Det er de innvilgede periodene fra og med i dag.
 * Er alle periodene passert, er det den siste innvilgede perioden, med mindre slutten er avsluttet av et vedtak.
 * @param sluttenErAvsluttetAvVedtak Den siste dagen deltakelsen har vært innvilget er stanset, opphørt eller innvilget for en annen deltakelse.
 * Saken har da allerede tatt stilling til slutten av deltakelsen, og en ny status eller en forlengelse er ikke lenger relevant.
 */
private data class GjeldendeInnvilgelse(
    val innvilgedePerioder: List<PeriodeMedVerdi<VedtakForDeltakelse>>,
    val perioderForDeltakelsesmengde: List<PeriodeMedVerdi<VedtakForDeltakelse>>,
    val sluttenErAvsluttetAvVedtak: Boolean,
) {
    val førsteInnvilgedeDag: LocalDate = innvilgedePerioder.first().periode.fraOgMed
    val sisteInnvilgedeDag: LocalDate = innvilgedePerioder.last().periode.tilOgMed

    /** Tilstanden fra vedtaket som innvilger [førsteInnvilgedeDag], som en tidligere startdato sammenlignes med. */
    val tilstandVedStart: Deltakelsestilstand = innvilgedePerioder.first().verdi.deltakelse

    /** Tilstanden fra vedtaket som innvilger [sisteInnvilgedeDag], som sluttdato og status sammenlignes med. */
    val tilstandVedSlutt: Deltakelsestilstand = innvilgedePerioder.last().verdi.deltakelse

    val tilstanderForDeltakelsesmengde: List<Deltakelsestilstand> =
        perioderForDeltakelsesmengde.map { it.verdi.deltakelse }.distinct()

    val vedtakVedStart: Rammevedtak = innvilgedePerioder.first().verdi.gjeldendeVedtak
    val vedtakVedSlutt: Rammevedtak = innvilgedePerioder.last().verdi.gjeldendeVedtak
    val vedtakForDeltakelsesmengde: Set<Rammevedtak> =
        perioderForDeltakelsesmengde.map { it.verdi.gjeldendeVedtak }.toSet()

    /** De gjeldende vedtakene som innvilger deltakelsen i deler av [periode]. */
    fun vedtakSomInnvilger(periode: Periode): Set<Rammevedtak> =
        innvilgedePerioder.filter { it.periode.overlapperMed(periode) }.map { it.verdi.gjeldendeVedtak }.toSet()
}

/** Gir null dersom deltakelsen ikke er innvilget i noe gjeldende vedtak. */
private fun Periodisering<VedtakForDeltakelse>.tilGjeldendeInnvilgelse(iDag: LocalDate): GjeldendeInnvilgelse? {
    val innvilget = perioderMedVerdi.filter { it.verdi.erInnvilgetIGjeldendeVedtak }.ifEmpty { return null }
    val sluttenErAvsluttetAvVedtak = !perioderMedVerdi.last().verdi.erInnvilgetIGjeldendeVedtak
    val innvilgetFremover = innvilget.filter { !it.periode.tilOgMed.isBefore(iDag) }

    return GjeldendeInnvilgelse(
        innvilgedePerioder = innvilget,
        perioderForDeltakelsesmengde = when {
            innvilgetFremover.isNotEmpty() -> innvilgetFremover
            sluttenErAvsluttetAvVedtak -> emptyList()
            else -> listOf(innvilget.last())
        },
        sluttenErAvsluttetAvVedtak = sluttenErAvsluttetAvVedtak,
    )
}

/**
 * Tolker endringene mellom nå-tilstanden fra tiltakshistorikk og de gjeldende vedtakene.
 * Utfallene prioriteres i rekkefølgen avsluttet som forventet, avbrutt, ikke aktuell, forlengelse og andre endringer.
 *
 * Startdatoen er relevant dersom deltakelsen nå starter etter første innvilgede dag, eller tidligere enn da den ble innvilget.
 * Sluttdatoen er relevant dersom deltakelsen nå slutter før siste innvilgede dag, eller slutter senere enn da den ble innvilget.
 * En endret start- eller sluttdato som ikke berører de innvilgede periodene er altså ikke relevant.
 * Avsluttet som forventet gis bare når statusen er den eneste relevante endringen.
 */
private fun GjeldendeInnvilgelse.finnEndringer(
    oppdatertDeltakelse: Deltakelsestilstand,
    iDag: LocalDate,
): TiltaksdeltakerEndring? {
    val nyStartdato = oppdatertDeltakelse.deltakelseFraOgMed
    val nySluttdato = oppdatertDeltakelse.deltakelseTilOgMed
    val nyStatus = oppdatertDeltakelse.deltakelseStatus

    val innvilgetFørStartdato = nyStartdato != null && nyStartdato.isAfter(førsteInnvilgedeDag)
    val starterTidligere = nyStartdato != null &&
        tilstandVedStart.deltakelseFraOgMed?.let { nyStartdato.isBefore(it) } == true
    val innvilgetEtterSluttdato = nySluttdato != null && nySluttdato.isBefore(sisteInnvilgedeDag)
    // En null sluttdato i nå-tilstanden betyr en åpen deltakelse, som regnes som senere enn enhver sluttdato.
    val slutterSenere = !sluttenErAvsluttetAvVedtak &&
        tilstandVedSlutt.deltakelseTilOgMed?.let { nySluttdato == null || nySluttdato.isAfter(it) } == true

    val endretStartdato = innvilgetFørStartdato || starterTidligere
    val endretSluttdato = innvilgetEtterSluttdato || slutterSenere
    val endretStatus = !sluttenErAvsluttetAvVedtak && !tilstandVedSlutt.harSammeStatus(oppdatertDeltakelse)
    val endretDeltakelsesmengde = tilstanderForDeltakelsesmengde
        .firstOrNull { !it.harSammeDeltakelsesmengde(oppdatertDeltakelse) }
        ?.endretDeltakelsesmengde(oppdatertDeltakelse)

    val sluttdatoErPassert = nySluttdato != null && !nySluttdato.isAfter(iDag)

    return when {
        !endretStartdato && !endretSluttdato && !endretStatus && endretDeltakelsesmengde == null -> null

        endretStatus &&
            (nyStatus == TiltakDeltakerstatus.HarSluttet || nyStatus == TiltakDeltakerstatus.Fullført) &&
            sluttdatoErPassert &&
            !endretStartdato &&
            !endretSluttdato &&
            endretDeltakelsesmengde == null -> TiltaksdeltakerEndring.AvsluttetSomForventet

        (endretStatus && nyStatus == TiltakDeltakerstatus.Avbrutt) ||
            (innvilgetEtterSluttdato && sluttdatoErPassert) -> TiltaksdeltakerEndring.AvbruttDeltakelse

        endretStatus && nyStatus == TiltakDeltakerstatus.IkkeAktuell -> TiltaksdeltakerEndring.IkkeAktuellDeltakelse

        slutterSenere && nySluttdato != null && !endretStartdato -> TiltaksdeltakerEndring.Forlengelse(
            nySluttdato = nySluttdato,
            endretDeltakelsesmengde = endretDeltakelsesmengde,
        )

        else -> TiltaksdeltakerEndring.AndreEndringer(
            endretDeltakelsesmengde = endretDeltakelsesmengde,
            endretStartdato = if (endretStartdato) TiltaksdeltakerEndring.EndretStartdato(nyStartdato) else null,
            endretSluttdato = if (endretSluttdato) TiltaksdeltakerEndring.EndretSluttdato(nySluttdato) else null,
            endretStatus = if (endretStatus) TiltaksdeltakerEndring.EndretStatus(nyStatus) else null,
        )
    }
}

/**
 * Vurderer hvilken revurdering som kan opprettes automatisk for endringen, eller null dersom den må følges opp manuelt.
 * Det opprettes ikke revurdering dersom saken mangler førstegangsvedtak eller har åpne behandlinger.
 *
 * - Avbrudd gir stans, så lenge deltakelsen er innvilget fra og med i dag og ingen andre deltakelser er innvilget etter avbruddet.
 * Er andre deltakelser innvilget etter avbruddet, vurderes omgjøring i stedet, siden en stans også ville stanset dem.
 * Stansen behandles automatisk etter at den er opprettet, se [no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.utledAutomatiskStans].
 * - Forlengelse gir innvilgelse dersom ny sluttdato er etter siste dag med rett på saken.
 * Ellers vurderes omgjøring dersom deltakelsesmengden er endret samtidig.
 * - Endret startdato, sluttdato eller deltakelsesmengde gir omgjøring.
 * - Ikke aktuell, avsluttet som forventet og ren statusendring følges opp manuelt.
 *
 * Omgjøring gis bare når endringen berører nøyaktig ett gjeldende vedtak, siden en omgjøring bare kan omgjøre ett vedtak.
 * Deltakelsen kan altså være innvilget i flere vedtak, så lenge endringen bare berører periodene til ett av dem.
 */
private fun Sak.vurderAutomatiskRevurdering(
    tiltaksdeltakerId: TiltaksdeltakerId,
    endring: TiltaksdeltakerEndring,
    nåtilstand: Deltakelsestilstand,
    gjeldendeInnvilgelse: GjeldendeInnvilgelse,
    iDag: LocalDate,
): AutomatiskRevurderingAvEndring? {
    if (!harFørstegangsvedtak || rammebehandlinger.åpneBehandlinger.isNotEmpty()) {
        return null
    }

    return when (endring) {
        is TiltaksdeltakerEndring.AvbruttDeltakelse -> vurderRevurderingForAvbrudd(
            tiltaksdeltakerId,
            nåtilstand,
            gjeldendeInnvilgelse,
            iDag,
        )

        is TiltaksdeltakerEndring.Forlengelse -> {
            val sisteDagSomGirRett = sisteDagSomGirRett
            if (sisteDagSomGirRett == null || endring.nySluttdato.isAfter(sisteDagSomGirRett)) {
                AutomatiskRevurderingAvEndring.Innvilgelse
            } else {
                endring.endretDeltakelsesmengde?.let { omgjøringAv(gjeldendeInnvilgelse.vedtakForDeltakelsesmengde) }
            }
        }

        is TiltaksdeltakerEndring.AndreEndringer -> omgjøringAv(gjeldendeInnvilgelse.vedtakBerørtAv(endring))

        is TiltaksdeltakerEndring.IkkeAktuellDeltakelse,
        is TiltaksdeltakerEndring.AvsluttetSomForventet,
        -> null
    }
}

/**
 * Avbruddet gjelder fra dagen etter ny sluttdato dersom den er avkortet inn i de innvilgede periodene, og ellers fra i dag.
 * Avbrudd i innvilgelser som er passert følges opp manuelt.
 */
private fun Sak.vurderRevurderingForAvbrudd(
    tiltaksdeltakerId: TiltaksdeltakerId,
    nåtilstand: Deltakelsestilstand,
    gjeldendeInnvilgelse: GjeldendeInnvilgelse,
    iDag: LocalDate,
): AutomatiskRevurderingAvEndring? {
    if (gjeldendeInnvilgelse.sisteInnvilgedeDag.isBefore(iDag)) {
        return null
    }

    val nySluttdato = nåtilstand.deltakelseTilOgMed
    val avbruttFraOgMed = if (nySluttdato != null && nySluttdato.isBefore(gjeldendeInnvilgelse.sisteInnvilgedeDag)) {
        maxOf(nySluttdato.plusDays(1), gjeldendeInnvilgelse.førsteInnvilgedeDag)
    } else {
        iDag
    }

    val harAndreDeltakelserInnvilgetEtterAvbruddet = rammevedtaksliste.valgteTiltaksdeltakelser.perioderMedVerdi.any {
        !it.periode.tilOgMed.isBefore(avbruttFraOgMed) && it.verdi.internDeltakelseId != tiltaksdeltakerId
    }

    if (harAndreDeltakelserInnvilgetEtterAvbruddet) {
        return omgjøringAv(
            gjeldendeInnvilgelse.vedtakSomInnvilger(avbruttFraOgMed til gjeldendeInnvilgelse.sisteInnvilgedeDag),
        )
    }

    return AutomatiskRevurderingAvEndring.Stans
}

/** De gjeldende vedtakene som innvilger periodene endret startdato, sluttdato eller deltakelsesmengde berører. */
private fun GjeldendeInnvilgelse.vedtakBerørtAv(endring: TiltaksdeltakerEndring.AndreEndringer): Set<Rammevedtak> {
    val berørtAvStartdato = endring.endretStartdato?.nyStartdato?.let { nyStartdato ->
        if (nyStartdato.isAfter(førsteInnvilgedeDag)) {
            vedtakSomInnvilger(førsteInnvilgedeDag til minOf(nyStartdato.minusDays(1), sisteInnvilgedeDag))
        } else {
            setOf(vedtakVedStart)
        }
    }.orEmpty()

    val berørtAvSluttdato = endring.endretSluttdato?.let { (nySluttdato) ->
        if (nySluttdato != null && nySluttdato.isBefore(sisteInnvilgedeDag)) {
            vedtakSomInnvilger(maxOf(nySluttdato.plusDays(1), førsteInnvilgedeDag) til sisteInnvilgedeDag)
        } else {
            setOf(vedtakVedSlutt)
        }
    }.orEmpty()

    val berørtAvDeltakelsesmengde =
        if (endring.endretDeltakelsesmengde != null) vedtakForDeltakelsesmengde else emptySet()

    return berørtAvStartdato + berørtAvSluttdato + berørtAvDeltakelsesmengde
}

/** Gir null dersom ingen eller flere vedtak berøres, slik at saksbehandler selv må ta stilling til hva som skal omgjøres. */
private fun omgjøringAv(berørteVedtak: Set<Rammevedtak>): AutomatiskRevurderingAvEndring? =
    berørteVedtak.singleOrNull()?.let { AutomatiskRevurderingAvEndring.Omgjøring(it.id) }
