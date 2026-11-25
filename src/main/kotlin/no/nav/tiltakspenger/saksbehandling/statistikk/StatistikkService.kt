package no.nav.tiltakspenger.saksbehandling.statistikk

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkDTO
import java.time.Clock

/**
 * Gjør [Statistikkhendelse]r om til rader og lagrer dem; se `docs/statistikk/README.md`.
 * [generer] slår opp strengt fortrolig adresse i PDL og skal derfor kalles før transaksjonen, mens [lagre] kalles i samme transaksjon som endringen.
 */
class StatistikkService(
    private val personKlient: PersonKlient,
    private val gitHash: String,
    private val clock: Clock,
    private val statistikkRepo: StatistikkRepo,
) {
    suspend fun generer(
        vararg hendelse: Statistikkhendelse,
    ): StatistikkDTO {
        return generer(Statistikkhendelser(*hendelse))
    }

    suspend fun generer(
        hendelser: Statistikkhendelser,
    ): StatistikkDTO {
        return hendelser.tilStatistikkDto(
            gjelderKode6 = {
                val person = personKlient.hentEnkelPerson(it)
                person.adressebeskyttelse.erKode6
            },
            versjon = gitHash,
        )
    }

    fun lagre(
        dto: StatistikkDTO,
        context: TransactionContext? = null,
    ) {
        statistikkRepo.lagre(dto, context)
    }

    suspend fun lagre(
        hendelse: Statistikkhendelse,
        context: TransactionContext? = null,
    ) {
        statistikkRepo.lagre(generer(hendelse), context)
    }

    fun oppdaterAdressebeskyttelse(sakId: SakId, transactionContext: TransactionContext? = null) {
        statistikkRepo.oppdaterAdressebeskyttelse(sakId, transactionContext)
    }

    fun oppdaterFnr(
        gammeltFnr: Fnr,
        nyttFnr: Fnr,
        tx: TransactionContext? = null,
    ) {
        statistikkRepo.oppdaterFnr(gammeltFnr, nyttFnr, clock, tx)
    }
}
