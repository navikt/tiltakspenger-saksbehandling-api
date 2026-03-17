package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk

import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.StatistikkUtbetalingDTO

data class StatistikkDTO(
    val saksstatistikk: List<SaksstatistikkDTO>,
    val stønadsstatistikk: List<StatistikkStønadDTO>,
    val meldekortstatistikk: List<StatistikkMeldekortDTO>,
    val utbetalingsstatistikk: List<StatistikkUtbetalingDTO>,
)
