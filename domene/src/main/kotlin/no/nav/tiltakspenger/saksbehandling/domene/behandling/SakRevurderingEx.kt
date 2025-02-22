package no.nav.tiltakspenger.saksbehandling.domene.behandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.krympStønadsdager
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.krympVilkårssett
import no.nav.tiltakspenger.saksbehandling.service.sak.KanIkkeStarteRevurdering

private val loggerForStartRevurdering = KotlinLogging.logger { }

suspend fun Sak.startRevurdering(
    kommando: StartRevurderingKommando,
    hentSaksopplysninger: suspend (sakId: SakId, saksnummer: Saksnummer, fnr: Fnr, correlationId: CorrelationId, saksopplysningsperiode: Periode) -> Saksopplysninger,
): Either<KanIkkeStarteRevurdering, Pair<Sak, Behandling>> {
    val saksbehandler = kommando.saksbehandler
    val fraOgMed = kommando.fraOgMed

    if (!kommando.saksbehandler.erSaksbehandler()) {
        loggerForStartRevurdering.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å starte revurdering på sak ${kommando.sakId}" }
        return KanIkkeStarteRevurdering.HarIkkeTilgang(
            kreverEnAvRollene = setOf(Saksbehandlerrolle.SAKSBEHANDLER),
            harRollene = saksbehandler.roller,
        ).left()
    }
    this.sisteUtbetalteMeldekortDag()?.let {
        if (it >= fraOgMed) {
            loggerForStartRevurdering.warn { "Kan ikke stanse på dag som er utbetalt." }
            return KanIkkeStarteRevurdering.KanIkkeStanseUtbetaltDag(
                førsteMuligeStansdato = it.plusDays(1),
                ønsketStansdato = fraOgMed,
            ).left()
        }
    }
    require(this.vedtaksliste.antallInnvilgelsesperioder == 1) {
        "Kan kun opprette en stansrevurdering dersom vi har en sammenhengende innvilgelsesperiode. sakId=${this.id}"
    }

    if (fraOgMed.isBefore(this.førsteDagSomGirRett)) {
        throw IllegalArgumentException("Kan ikke starte revurdering ($fraOgMed) før første innvilgetdato (${this.førsteDagSomGirRett})")
    }
    val tilOgMed = this.sisteDagSomGirRett!!
    val revurderingsperiode = Periode(fraOgMed, tilOgMed)
    // Merk at vi beholder eventuelle tidspunkt og IDer når vi krymper.
    val vilkårssett = if (erNyFlyt!!) null else this.krympVilkårssett(revurderingsperiode).single().verdi
    val stønadsdager = if (erNyFlyt) null else this.krympStønadsdager(revurderingsperiode).single().verdi
    val revurdering = Behandling.opprettRevurdering(
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        saksbehandler = saksbehandler,
        periode = revurderingsperiode,
        vilkårssett = vilkårssett,
        stønadsdager = stønadsdager,
        hentSaksopplysninger = {
            hentSaksopplysninger(
                this.id,
                this.saksnummer,
                this.fnr,
                kommando.correlationId,
                revurderingsperiode,
            )
        },
    )
    return Pair(leggTilRevurdering(revurdering), revurdering).right()
}

suspend fun Sak.startRevurderingV2(
    kommando: StartRevurderingV2Kommando,
    hentSaksopplysninger: suspend (sakId: SakId, saksnummer: Saksnummer, fnr: Fnr, correlationId: CorrelationId, saksopplysningsperiode: Periode) -> Saksopplysninger,
): Either<KanIkkeStarteRevurdering, Pair<Sak, Behandling>> {
    val saksbehandler = kommando.saksbehandler

    if (!kommando.saksbehandler.erSaksbehandler()) {
        loggerForStartRevurdering.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å starte revurdering på sak ${kommando.sakId}" }
        return KanIkkeStarteRevurdering.HarIkkeTilgang(
            kreverEnAvRollene = setOf(Saksbehandlerrolle.SAKSBEHANDLER),
            harRollene = saksbehandler.roller,
        ).left()
    }

    require(this.vedtaksliste.antallInnvilgelsesperioder == 1) {
        "Kan kun opprette en stansrevurdering dersom vi har en sammenhengende innvilgelsesperiode. sakId=${this.id}"
    }

    requireNotNull(this.førstegangsbehandling) { "Kan ikke opprette revurdering uten en førstegangsbehandling" }

    val saksopplysningsperiode = this.saksopplysningsperiode!!

    val revurdering = Behandling.opprettRevurderingV2(
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        saksbehandler = saksbehandler,
        hentSaksopplysninger = {
            hentSaksopplysninger(
                this.id,
                this.saksnummer,
                this.fnr,
                kommando.correlationId,
                saksopplysningsperiode,
            )
        },
        saksopplysningsperiode = saksopplysningsperiode,
    )

    return Pair(leggTilRevurdering(revurdering), revurdering).right()
}

fun Sak.leggTilRevurdering(
    revurdering: Behandling,
): Sak {
    return copy(behandlinger = behandlinger.leggTilRevurdering(revurdering))
}
