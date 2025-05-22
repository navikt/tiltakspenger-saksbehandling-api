package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeStarteRevurdering
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock
import java.time.LocalDate

private val loggerForStartRevurdering = KotlinLogging.logger { }

suspend fun Sak.startRevurdering(
    kommando: StartRevurderingKommando,
    clock: Clock,
    hentSaksopplysninger: suspend (fnr: Fnr, correlationId: CorrelationId, saksopplysningsperiode: Periode) -> Saksopplysninger,
): Either<KanIkkeStarteRevurdering, Pair<Sak, Revurdering>> {
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

    // TODO - dette gjelder bare så lenge dette er en stans revurdering
    // Her har vi ikke valgt revurderingsperioden, men ved forlengelse vil den kunne være større.
    val saksopplysningsperiode = this.vedtaksliste.innvilgelsesperiode!!

    val hentSaksopplysninger: suspend (Periode) -> Saksopplysninger = { periode: Periode ->
        hentSaksopplysninger(
            fnr,
            kommando.correlationId,
            periode,
        )
    }

    val revurdering = Revurdering.opprett(
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        saksbehandler = saksbehandler,
        hentSaksopplysninger = hentSaksopplysninger,
        saksopplysningsperiode = saksopplysningsperiode,
        clock = clock,
    )

    return Pair(leggTilRevurdering(revurdering), revurdering).right()
}

fun Sak.leggTilRevurdering(
    revurdering: Revurdering,
): Sak {
    return copy(behandlinger = behandlinger.leggTilRevurdering(revurdering))
}

fun Sak.sendRevurderingTilBeslutning(
    kommando: SendRevurderingTilBeslutningKommando,
    clock: Clock,
): Either<KanIkkeSendeTilBeslutter, Revurdering> {
    if (!kommando.saksbehandler.erSaksbehandler()) {
        return KanIkkeSendeTilBeslutter.MåVæreSaksbehandler.left()
    }

    val stansDato = kommando.stansDato
    this.validerStansDato(stansDato)

    val behandling = this.hentBehandling(kommando.behandlingId)
    require(behandling is Revurdering) { "Behandlingen må være en revurdering, men var: ${behandling?.behandlingstype}" }

    val oppdatertBehandling =
        behandling.tilBeslutning(kommando, this.vedtaksliste.sisteDagSomGirRett!!, clock)

    return oppdatertBehandling.right()
}

fun Sak.validerStansDato(stansDato: LocalDate?) {
    if (stansDato == null) {
        throw IllegalArgumentException("Stansdato er ikke satt")
    }

    this.førsteLovligeStansdato()?.also {
        if (stansDato.isBefore(it)) {
            throw IllegalArgumentException("Angitt stansdato $stansDato er før første lovlige stansdato $it")
        }
    }

    if (stansDato.isBefore(this.førsteDagSomGirRett)) {
        throw IllegalArgumentException("Kan ikke starte revurdering ($stansDato) før første innvilgetdato (${this.førsteDagSomGirRett})")
    }

    if (stansDato.isAfter(this.sisteDagSomGirRett)) {
        throw IllegalArgumentException("Kan ikke starte revurdering med stansdato ($stansDato) etter siste innvilgetdato (${this.sisteDagSomGirRett})")
    }
}
