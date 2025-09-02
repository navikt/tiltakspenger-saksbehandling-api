package no.nav.tiltakspenger.saksbehandling.statistikk.meldekort

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext

interface StatistikkMeldekortRepo {
    fun lagre(dto: StatistikkMeldekortDTO, context: TransactionContext? = null)
    fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, context: TransactionContext? = null)
}
