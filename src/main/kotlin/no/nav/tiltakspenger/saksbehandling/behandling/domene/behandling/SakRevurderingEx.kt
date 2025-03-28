package no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeStarteRevurdering
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

private val loggerForStartRevurdering = KotlinLogging.logger { }

suspend fun Sak.startRevurdering(
    kommando: StartRevurderingKommando,
    clock: Clock,
    hentSaksopplysninger: suspend (fnr: Fnr, correlationId: CorrelationId, saksopplysningsperiode: Periode) -> no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger,
): Either<no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeStarteRevurdering, Pair<Sak, Behandling>> {
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

    val revurdering = Behandling.opprettRevurdering(
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        saksbehandler = saksbehandler,
        hentSaksopplysninger = {
            hentSaksopplysninger(
                this.fnr,
                kommando.correlationId,
                saksopplysningsperiode,
            )
        },
        saksopplysningsperiode = saksopplysningsperiode,
        clock = clock,
    )

    return Pair(leggTilRevurdering(revurdering), revurdering).right()
}

fun Sak.leggTilRevurdering(
    revurdering: Behandling,
): Sak {
    return copy(behandlinger = behandlinger.leggTilRevurdering(revurdering))
}
