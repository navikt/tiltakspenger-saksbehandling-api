package no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat.OmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
import java.time.Clock

fun Revurdering.oppdaterOmgjøring(
    kommando: OppdaterOmgjøringKommando,
    utbetaling: BehandlingUtbetaling?,
    finnRammevedtakSomOmgjøres: (vedtaksperiode: Periode) -> OmgjørRammevedtak,
    rammevedtaksliste: Rammevedtaksliste,
    clock: Clock,
): Either<KanIkkeOppdatereBehandling, Revurdering> {
    require(this.resultat is Omgjøringsresultat)

    kanOppdatere(kommando.saksbehandler).onLeft { return it.left() }

    return when (kommando) {
        is OppdaterOmgjøringKommando.OmgjøringInnvilgelse -> oppdaterOmgjøringInnvilgelse(
            kommando = kommando,
            utbetaling = utbetaling,
            finnRammevedtakSomOmgjøres = finnRammevedtakSomOmgjøres,
            clock = clock,
        )

        is OppdaterOmgjøringKommando.OmgjøringOpphør -> oppdaterOmgjøringOpphør(
            kommando = kommando,
            utbetaling = utbetaling,
            finnRammevedtakSomOmgjøres = finnRammevedtakSomOmgjøres,
            rammevedtaksliste = rammevedtaksliste,
            clock = clock,
        )

        is OppdaterOmgjøringKommando.OmgjøringIkkeValgt -> oppdaterOmgjøringIkkeValgt(
            kommando = kommando,
            clock = clock,
        )
    }
}

/**
 * Vedtaksperioden kan omgjøre ett eller flere vedtak, og kan ha hull både innad i et vedtak og mellom vedtakene.
 * Vedtaket omgjøringen ble startet fra er kun et utgangspunkt; det er vedtaksperioden som avgjør hvilke vedtak som omgjøres.
 */
private fun Revurdering.oppdaterOmgjøringInnvilgelse(
    kommando: OppdaterOmgjøringKommando.OmgjøringInnvilgelse,
    utbetaling: BehandlingUtbetaling?,
    finnRammevedtakSomOmgjøres: (vedtaksperiode: Periode) -> OmgjørRammevedtak,
    clock: Clock,
): Either<KanIkkeOppdatereBehandling, Revurdering> {
    val nyVedtaksperiode = kommando.vedtaksperiode

    val rammevedtakSomOmgjøres = finnRammevedtakSomOmgjøres(nyVedtaksperiode)

    validerRammevedtakSomOmgjøres(rammevedtakSomOmgjøres).onLeft { return it.left() }

    if (!nyVedtaksperiode.inneholderHele(kommando.innvilgelsesperioder.totalPeriode)) {
        return KanIkkeOppdatereOmgjøring.VedtaksperiodeMåInneholdeInnvilgelsesperiodene.left()
    }

    val oppdaterteInnvilgelsesperioder = kommando
        .tilInnvilgelseperioder(this)
        .oppdaterTiltaksdeltakelser(saksopplysninger.tiltaksdeltakelser)

    if (oppdaterteInnvilgelsesperioder == null) {
        return KanIkkeOppdatereBehandling.InnvilgelsesperiodeHarIngenOverlappMedTiltaksdeltakelserFraSaksopplysninger.left()
    }

    return this.copy(
        sistEndret = nå(clock),
        begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
        fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
        resultat = OmgjøringInnvilgelse(
            vedtaksperiode = nyVedtaksperiode,
            innvilgelsesperioder = oppdaterteInnvilgelsesperioder,
            barnetillegg = kommando.barnetillegg,
            omgjørRammevedtak = rammevedtakSomOmgjøres,
        ),
        utbetaling = utbetaling,
        skalSendeVedtaksbrev = kommando.skalSendeVedtaksbrev,
        skalJournalføreNotat = kommando.skalJournalføreNotat,
        prioritering = kommando.prioritering,
    ).right()
}

/**
 * Fyller ut et opphør som systemet har opprettet klart til behandling, før det overlates til en saksbehandler.
 * Behandlingen er ikke tildelt noen, så det sjekkes ikke hvem som oppdaterer den, men opphøret valideres som ved [oppdaterOmgjøring].
 */
fun Revurdering.fyllUtAutomatiskOpprettetOpphør(
    kommando: OppdaterOmgjøringKommando.OmgjøringOpphør,
    utbetaling: BehandlingUtbetaling?,
    finnRammevedtakSomOmgjøres: (vedtaksperiode: Periode) -> OmgjørRammevedtak,
    rammevedtaksliste: Rammevedtaksliste,
    clock: Clock,
): Either<KanIkkeOppdatereBehandling, Revurdering> {
    require(automatiskOpprettetGrunn != null && erKlarTilBehandling && resultat is Omgjøringsresultat.OmgjøringIkkeValgt) {
        "Kun automatisk opprettede omgjøringer som ikke er påbegynt kan fylles ut automatisk. Status: $status, resultat: ${resultat::class.simpleName}"
    }
    return oppdaterOmgjøringOpphør(
        kommando = kommando,
        utbetaling = utbetaling,
        finnRammevedtakSomOmgjøres = finnRammevedtakSomOmgjøres,
        rammevedtaksliste = rammevedtaksliste,
        clock = clock,
    )
}

/**
 * Opphøret kan gå over flere vedtak og over hull, men må starte og slutte i en gjeldende innvilgelsesperiode på saken.
 * Det kan gå over vedtak som er delvis opphørt (omgjøring til innvilgelse), men ikke over stans eller rent opphør.
 */
// TODO abn: sjekkene her bør også kjøres ved iverksetting
private fun Revurdering.oppdaterOmgjøringOpphør(
    kommando: OppdaterOmgjøringKommando.OmgjøringOpphør,
    utbetaling: BehandlingUtbetaling?,
    finnRammevedtakSomOmgjøres: (vedtaksperiode: Periode) -> OmgjørRammevedtak,
    rammevedtaksliste: Rammevedtaksliste,
    clock: Clock,
): Either<KanIkkeOppdatereBehandling, Revurdering> {
    val måHaFritekst = kommando.skalSendeVedtaksbrev && kommando.valgteHjemler.any {
        Omgjøringsresultat.OmgjøringOpphør.hjemlerSomMåHaFritekst.contains(it)
    }

    if (måHaFritekst && kommando.fritekstTilVedtaksbrev == null) {
        return KanIkkeOppdatereOmgjøring.MåHaFritekstForValgteHjemler(kommando.valgteHjemler).left()
    }

    val opphørtPeriode = kommando.vedtaksperiode

    val rammevedtakSomOmgjøres = finnRammevedtakSomOmgjøres(opphørtPeriode)

    validerRammevedtakSomOmgjøres(rammevedtakSomOmgjøres).onLeft { return it.left() }

    val omgjørStansEllerOpphør = rammevedtakSomOmgjøres.rammevedtakIDer
        .map { rammevedtaksliste.hentRammevedtakForId(it) }
        .any { it.erStans || it.erOpphør }

    if (omgjørStansEllerOpphør) {
        return KanIkkeOppdatereOmgjøring.KanIkkeOpphøreOverStansEllerOpphør.left()
    }

    val gjeldendeInnvilgetPerioder = rammevedtaksliste.innvilgetTidslinje.perioder

    if (gjeldendeInnvilgetPerioder.none { it.inneholder(opphørtPeriode.fraOgMed) }) {
        return KanIkkeOppdatereOmgjøring.UgyldigPeriodeForOpphør("Perioden som opphøres må starte i en gjeldende innvilgelsesperiode")
            .left()
    }

    if (gjeldendeInnvilgetPerioder.none { it.inneholder(opphørtPeriode.tilOgMed) }) {
        return KanIkkeOppdatereOmgjøring.UgyldigPeriodeForOpphør("Perioden som opphøres må slutte i en gjeldende innvilgelsesperiode")
            .left()
    }

    return this.copy(
        sistEndret = nå(clock),
        begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
        fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
        resultat = Omgjøringsresultat.OmgjøringOpphør(
            vedtaksperiode = opphørtPeriode,
            omgjørRammevedtak = rammevedtakSomOmgjøres,
            valgteHjemler = kommando.valgteHjemler,
        ),
        utbetaling = utbetaling,
        skalSendeVedtaksbrev = kommando.skalSendeVedtaksbrev,
        skalJournalføreNotat = kommando.skalJournalføreNotat,
        prioritering = kommando.prioritering,
    ).right()
}

/**
 * Beholder omgjøringsgrunnlaget fra forrige resultat, slik at vedtakene som var valgt for omgjøring ikke går tapt.
 */
private fun Revurdering.oppdaterOmgjøringIkkeValgt(
    kommando: OppdaterOmgjøringKommando.OmgjøringIkkeValgt,
    clock: Clock,
): Either<KanIkkeOppdatereBehandling, Revurdering> {
    val rammevedtakSomOmgjøres = this.resultat.omgjørRammevedtak

    return this.copy(
        sistEndret = nå(clock),
        begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
        fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
        resultat = Omgjøringsresultat.OmgjøringIkkeValgt(
            omgjørRammevedtak = rammevedtakSomOmgjøres,
        ),
        utbetaling = utbetaling,
        skalJournalføreNotat = kommando.skalJournalføreNotat,
        prioritering = kommando.prioritering,
    ).right()
}

private fun validerRammevedtakSomOmgjøres(
    rammevedtakSomOmgjøres: OmgjørRammevedtak,
): Either<KanIkkeOppdatereOmgjøring, Unit> {
    if (rammevedtakSomOmgjøres.isEmpty()) {
        return KanIkkeOppdatereOmgjøring.MåOmgjøreMinstEttVedtak.left()
    }

    return Unit.right()
}
