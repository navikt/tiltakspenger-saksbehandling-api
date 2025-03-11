package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.KanIkkeStarteRevurdering

private val loggerForStartRevurdering = KotlinLogging.logger { }

suspend fun Sak.startRevurdering(
    kommando: StartRevurderingKommando,
    hentSaksopplysninger: suspend (fnr: Fnr, correlationId: CorrelationId, saksopplysningsperiode: Periode) -> Saksopplysninger,
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
    )

    return Pair(leggTilRevurdering(revurdering), revurdering).right()
}

fun Sak.leggTilRevurdering(
    revurdering: Behandling,
): Sak {
    return copy(behandlinger = behandlinger.leggTilRevurdering(revurdering))
}
