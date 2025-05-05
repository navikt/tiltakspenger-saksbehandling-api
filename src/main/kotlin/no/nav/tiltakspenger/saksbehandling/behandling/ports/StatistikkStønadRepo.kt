package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkUtbetalingDTO

interface StatistikkStønadRepo {
    fun lagre(dto: StatistikkStønadDTO, context: TransactionContext? = null)
    fun lagre(dto: StatistikkUtbetalingDTO, context: TransactionContext? = null)
    fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr)
    fun hent(sakId: SakId): List<StatistikkStønadDTO>
}
