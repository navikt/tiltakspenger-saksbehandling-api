package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock
import java.time.LocalDate

suspend fun Sak.startRevurdering(
    kommando: StartRevurderingKommando,
    clock: Clock,
    hentSaksopplysninger: suspend (fnr: Fnr, correlationId: CorrelationId, saksopplysningsperiode: Periode) -> Saksopplysninger,
): Pair<Sak, Revurdering> {
    val saksbehandler = kommando.saksbehandler
    krevSaksbehandlerRolle(saksbehandler)

    require(this.vedtaksliste.antallInnvilgelsesperioder == 1) {
        "Kan kun opprette en stansrevurdering dersom vi har en sammenhengende innvilgelsesperiode. sakId=${this.id}"
    }

    val hentSaksopplysninger: suspend (Periode) -> Saksopplysninger = { periode: Periode ->
        hentSaksopplysninger(
            fnr,
            kommando.correlationId,
            periode,
        )
    }

    val revurdering = when (kommando.revurderingType) {
        RevurderingUtfallType.STANS -> Revurdering.opprettStans(
            sakId = this.id,
            saksnummer = this.saksnummer,
            fnr = this.fnr,
            saksbehandler = saksbehandler,
            hentSaksopplysninger = hentSaksopplysninger,
            saksopplysningsperiode = this.vedtaksliste.innvilgelsesperiode!!,
            clock = clock,
        )

        RevurderingUtfallType.INNVILGELSESPERIODE -> throw NotImplementedError("Revurdering av innvilgelsesperiode er ikke implementert ennå")
    }

    return Pair(
        copy(behandlinger = behandlinger.leggTilRevurdering(revurdering)),
        revurdering,
    )
}

fun Sak.sendRevurderingTilBeslutning(
    kommando: RevurderingTilBeslutningKommando,
    clock: Clock,
): Either<KanIkkeSendeTilBeslutter, Revurdering> {
    krevSaksbehandlerRolle(kommando.saksbehandler)

    val behandling = this.hentBehandling(kommando.behandlingId)
    require(behandling is Revurdering) { "Behandlingen må være en revurdering, men var: ${behandling?.behandlingstype}" }

    if (kommando is RevurderingStansTilBeslutningKommando) {
        validerStansDato(kommando.stansDato)
    }

    return behandling.tilBeslutning(
        kommando = kommando,
        sisteDagSomGirRett = sisteDagSomGirRett,
        clock = clock,
    ).right()
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
