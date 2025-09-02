package no.nav.tiltakspenger.saksbehandling.statistikk.meldekort

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext

class StatistikkMeldekortFakeRepo : StatistikkMeldekortRepo {
    private val data = arrow.atomic.Atomic(mutableMapOf<String, StatistikkMeldekortDTO>())

    override fun lagre(
        dto: StatistikkMeldekortDTO,
        context: TransactionContext?,
    ) {
        val id = dto.sakId.toString() + dto.meldeperiodeKjedeId.toString()
        data.get()[id] = dto
    }

    override fun oppdaterFnr(
        gammeltFnr: Fnr,
        nyttFnr: Fnr,
        context: TransactionContext?,
    ) {
        val statistikkMeldekort = data.get().values.find { it.brukerId == gammeltFnr.verdi }
        statistikkMeldekort?.let {
            val id = it.sakId.toString() + it.meldeperiodeKjedeId.toString()
            data.get()[id] = it.copy(
                brukerId = nyttFnr.verdi,
            )
        }
    }
}
