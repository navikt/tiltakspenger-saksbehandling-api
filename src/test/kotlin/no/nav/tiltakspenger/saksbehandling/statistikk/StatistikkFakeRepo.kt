package no.nav.tiltakspenger.saksbehandling.statistikk

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkDTO
import java.time.Clock

class StatistikkFakeRepo : StatistikkRepo {
    override fun lagre(
        statistikk: StatistikkDTO,
        context: TransactionContext?,
    ) {
    }

    override fun oppdaterFnr(
        gammeltFnr: Fnr,
        nyttFnr: Fnr,
        clock: Clock,
        transactionContext: TransactionContext?,
    ) {
    }

    override fun oppdaterAdressebeskyttelse(
        sakId: SakId,
        transactionContext: TransactionContext?,
    ) {
    }
}
