package no.nav.tiltakspenger.saksbehandling.statistikk

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkDTO
import java.time.Clock

interface StatistikkRepo {
    fun lagre(statistikk: StatistikkDTO, context: TransactionContext? = null)
    fun oppdaterFnr(
        gammeltFnr: Fnr,
        nyttFnr: Fnr,
        clock: Clock,
        transactionContext: TransactionContext? = null,
    )

    fun oppdaterAdressebeskyttelse(sakId: SakId, transactionContext: TransactionContext? = null)
}
