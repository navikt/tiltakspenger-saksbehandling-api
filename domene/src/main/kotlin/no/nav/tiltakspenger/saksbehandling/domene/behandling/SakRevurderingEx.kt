package no.nav.tiltakspenger.saksbehandling.domene.behandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.krympStønadsdager
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.krympVilkårssett
import no.nav.tiltakspenger.saksbehandling.service.sak.KanIkkeStarteRevurdering

private val loggerForStartRevurdering = KotlinLogging.logger { }

fun Sak.startRevurdering(
    kommando: StartRevurderingKommando,
): Either<KanIkkeStarteRevurdering, Pair<Sak, Behandling>> {
    val saksbehandler = kommando.saksbehandler
    val periode = kommando.periode

    if (!kommando.saksbehandler.erSaksbehandler()) {
        loggerForStartRevurdering.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å starte revurdering på sak ${kommando.sakId}" }
        return KanIkkeStarteRevurdering.HarIkkeTilgang(
            kreverEnAvRollene = setOf(Saksbehandlerrolle.SAKSBEHANDLER),
            harRollene = saksbehandler.roller,
        ).left()
    }
    this.sisteUtbetalteMeldekortDag()?.let {
        if (it >= kommando.periode.fraOgMed) {
            loggerForStartRevurdering.warn { "Kan ikke stanse på dag som er utbetalt." }
            return KanIkkeStarteRevurdering.KanIkkeStanseUtbetaltDag(
                førsteMuligeStansdato = it.plusDays(1),
                ønsketStansdato = kommando.periode.fraOgMed,
            ).left()
        }
    }
    // Merk at vi beholder eventuelle tidspunkt og IDer når vi krymper.
    val vilkårssett = this.krympVilkårssett(periode).single().verdi
    val stønadsdager = this.krympStønadsdager(periode).single().verdi
    val revurdering = Behandling.opprettRevurdering(
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        saksbehandler = saksbehandler,
        periode = periode,
        vilkårssett = vilkårssett,
        stønadsdager = stønadsdager,
    )
    return Pair(leggTilRevurdering(revurdering), revurdering).right()
}

fun Sak.leggTilRevurdering(
    revurdering: Behandling,
): Sak {
    return copy(behandlinger = behandlinger.leggTilRevurdering(revurdering))
}
