package no.nav.tiltakspenger.saksbehandling.service.sak

import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import java.time.LocalDate

sealed interface KanIkkeStarteRevurdering {

    data class HarIkkeTilgang(
        val kreverEnAvRollene: Set<Saksbehandlerrolle>,
        val harRollene: Set<Saksbehandlerrolle>,
    ) : KanIkkeStarteRevurdering

    data class KanIkkeStanseGodkjentMeldeperiode(
        val førsteMuligeStansdato: LocalDate,
        val ønsketStansdato: LocalDate,
    ) : KanIkkeStarteRevurdering
}
