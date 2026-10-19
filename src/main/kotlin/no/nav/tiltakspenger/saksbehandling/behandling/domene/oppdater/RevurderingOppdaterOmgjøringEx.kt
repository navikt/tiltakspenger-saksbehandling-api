package no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat.OmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.Clock

fun Revurdering.oppdaterOmgjøring(
    kommando: OppdaterOmgjøringKommando,
    utbetaling: BehandlingUtbetaling?,
    finnRammevedtakSomOmgjøres: (vedtaksperiode: Periode) -> OmgjørRammevedtak,
    omgjortVedtak: Rammevedtak,
    clock: Clock,
): Either<KanIkkeOppdatereBehandling, Revurdering> {
    require(this.resultat is Omgjøringsresultat)

    kanOppdatere(kommando.saksbehandler).onLeft { return it.left() }

    return when (kommando) {
        is OppdaterOmgjøringKommando.OmgjøringInnvilgelse -> oppdaterOmgjøringInnvilgelse(
            kommando = kommando,
            utbetaling = utbetaling,
            finnRammevedtakSomOmgjøres = finnRammevedtakSomOmgjøres,
            omgjortVedtak = omgjortVedtak,
            clock = clock,
        )

        is OppdaterOmgjøringKommando.OmgjøringOpphør -> oppdaterOmgjøringOpphør(
            kommando = kommando,
            utbetaling = utbetaling,
            finnRammevedtakSomOmgjøres = finnRammevedtakSomOmgjøres,
            omgjortVedtak = omgjortVedtak,
            clock = clock,
        )

        is OppdaterOmgjøringKommando.OmgjøringIkkeValgt -> oppdaterOmgjøringIkkeValgt(
            kommando = kommando,
            finnRammevedtakSomOmgjøres = finnRammevedtakSomOmgjøres,
            omgjortVedtak = omgjortVedtak,
            clock = clock,
        )
    }
}

private fun Revurdering.oppdaterOmgjøringInnvilgelse(
    kommando: OppdaterOmgjøringKommando.OmgjøringInnvilgelse,
    utbetaling: BehandlingUtbetaling?,
    finnRammevedtakSomOmgjøres: (vedtaksperiode: Periode) -> OmgjørRammevedtak,
    omgjortVedtak: Rammevedtak,
    clock: Clock,
): Either<KanIkkeOppdatereBehandling, Revurdering> {
    val nyVedtaksperiode = kommando.vedtaksperiode

    val rammevedtakSomOmgjøres = finnRammevedtakSomOmgjøres(nyVedtaksperiode)

    validerRammevedtakSomOmgjøres(
        rammevedtakSomOmgjøres,
        omgjortVedtak.id,
    ).onLeft { return it.left() }

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
    omgjortVedtak: Rammevedtak,
    clock: Clock,
): Either<KanIkkeOppdatereBehandling, Revurdering> {
    require(automatiskOpprettetGrunn != null && erKlarTilBehandling && resultat is Omgjøringsresultat.OmgjøringIkkeValgt) {
        "Kun automatisk opprettede omgjøringer som ikke er påbegynt kan fylles ut automatisk. Status: $status, resultat: ${resultat::class.simpleName}"
    }
    return oppdaterOmgjøringOpphør(
        kommando = kommando,
        utbetaling = utbetaling,
        finnRammevedtakSomOmgjøres = finnRammevedtakSomOmgjøres,
        omgjortVedtak = omgjortVedtak,
        clock = clock,
    )
}

// TODO abn: sjekkene her bør også kjøres ved iverksetting
private fun Revurdering.oppdaterOmgjøringOpphør(
    kommando: OppdaterOmgjøringKommando.OmgjøringOpphør,
    utbetaling: BehandlingUtbetaling?,
    finnRammevedtakSomOmgjøres: (vedtaksperiode: Periode) -> OmgjørRammevedtak,
    omgjortVedtak: Rammevedtak,
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

    validerRammevedtakSomOmgjøres(
        rammevedtakSomOmgjøres,
        omgjortVedtak.id,
    ).onLeft { return it.left() }

    if (omgjortVedtak.gyldigOpphørskommando == null) {
        return KanIkkeOppdatereOmgjøring.KanIkkeOpphøreVedtakUtenGjeldendeInnvilgelse.left()
    }

    if (omgjortVedtak.gjeldendeInnvilgetPerioder.none { it.inneholder(opphørtPeriode.fraOgMed) }) {
        return KanIkkeOppdatereOmgjøring.UgyldigPeriodeForOpphør("Perioden som opphøres må starte i en gjeldende innvilgelsesperiode")
            .left()
    }

    if (omgjortVedtak.gjeldendeInnvilgetPerioder.none { it.inneholder(opphørtPeriode.tilOgMed) }) {
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

private fun Revurdering.oppdaterOmgjøringIkkeValgt(
    kommando: OppdaterOmgjøringKommando.OmgjøringIkkeValgt,
    finnRammevedtakSomOmgjøres: (vedtaksperiode: Periode) -> OmgjørRammevedtak,
    omgjortVedtak: Rammevedtak,
    clock: Clock,
): Either<KanIkkeOppdatereBehandling, Revurdering> {
    val rammevedtakSomOmgjøres =
        omgjortVedtak.gjeldendeTotalPeriode?.let { finnRammevedtakSomOmgjøres(it) } ?: OmgjørRammevedtak.empty

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
    valgtRammevedtakId: VedtakId,
): Either<KanIkkeOppdatereOmgjøring, Unit> {
    if (rammevedtakSomOmgjøres.rammevedtakIDer.size > 1) {
        return KanIkkeOppdatereOmgjøring.KanIkkeOmgjøreFlereVedtak.left()
    }

    if (rammevedtakSomOmgjøres.rammevedtakIDer.size == 0) {
        return KanIkkeOppdatereOmgjøring.MåOmgjøreMinstEttVedtak.left()
    }

    if (rammevedtakSomOmgjøres.rammevedtakIDer.single() != valgtRammevedtakId) {
        return KanIkkeOppdatereOmgjøring.MåOmgjøreAngittVedtak.left()
    }

    if (rammevedtakSomOmgjøres.perioder.size != 1) {
        return KanIkkeOppdatereOmgjøring.MåOmgjøreEnSammenhengendePeriode.left()
    }

    return Unit.right()
}
