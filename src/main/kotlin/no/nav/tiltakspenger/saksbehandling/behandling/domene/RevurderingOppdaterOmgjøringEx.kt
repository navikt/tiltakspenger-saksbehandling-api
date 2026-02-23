package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periode.Periode
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

    validerKanOppdatere(kommando.saksbehandler).onLeft { return it.left() }

    return when (kommando) {
        is OppdaterOmgjøringKommando.OmgjøringInnvilgelse -> oppdaterOmgjøringInnvilgelse(
            kommando = kommando,
            utbetaling = utbetaling,
            finnRammevedtakSomOmgjøres = finnRammevedtakSomOmgjøres,
            omgjortVedtak = omgjortVedtak,
            clock = clock,
        )

        is OppdaterOmgjøringKommando.OmgjøringOpphør -> {
            val måHaFritekst = kommando.valgteHjemler.any {
                Omgjøringsresultat.OmgjøringOpphør.hjemlerSomMåHaFritekst.contains(it)
            }

            if (måHaFritekst && kommando.fritekstTilVedtaksbrev == null) {
                return KanIkkeOppdatereOmgjøring.MåHaFritekstForValgteHjemler(kommando.valgteHjemler).left()
            }

            oppdaterOmgjøringOpphør(
                kommando = kommando,
                utbetaling = utbetaling,
                finnRammevedtakSomOmgjøres = finnRammevedtakSomOmgjøres,
                omgjortVedtak = omgjortVedtak,
                clock = clock,
            )
        }

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

    requireNotNull(oppdaterteInnvilgelsesperioder) {
        // Dersom denne kaster og vi savner mer sakskontekst, bør denne returnere Either, slik at callee kan håndtere feilen.
        "Valgte innvilgelsesperioder har ingen overlapp med tiltaksdeltakelser fra saksopplysningene"
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
    ).right()
}

// TODO abn: sjekkene her bør også kjøres ved iverksetting
private fun Revurdering.oppdaterOmgjøringOpphør(
    kommando: OppdaterOmgjøringKommando.OmgjøringOpphør,
    utbetaling: BehandlingUtbetaling?,
    finnRammevedtakSomOmgjøres: (vedtaksperiode: Periode) -> OmgjørRammevedtak,
    omgjortVedtak: Rammevedtak,
    clock: Clock,
): Either<KanIkkeOppdatereBehandling, Revurdering> {
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
