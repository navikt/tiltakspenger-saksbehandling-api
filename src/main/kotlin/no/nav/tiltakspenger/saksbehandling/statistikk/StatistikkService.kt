package no.nav.tiltakspenger.saksbehandling.statistikk

import arrow.core.getOrElse
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.saksbehandler.RolleForStrengtFortroligAdresseService
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.Skjermingsoppslag
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkDTO
import java.time.Clock

/**
 * Gjør [Statistikkhendelse]r om til rader og lagrer dem; se `docs/statistikk/README.md`.
 * [generer] slår opp strengt fortrolig adresse i PDL og saksbehandlerens grupper i entra-proxy, og skal derfor kalles før transaksjonen, mens [lagre] kalles i samme transaksjon som endringen.
 */
class StatistikkService(
    private val personKlient: PersonKlient,
    private val rolleForStrengtFortroligAdresseService: RolleForStrengtFortroligAdresseService,
    private val gitHash: String,
    private val clock: Clock,
    private val statistikkRepo: StatistikkRepo,
) {
    /**
     * Klarer vi ikke å slå opp rollen, regnes behandlingen å ikke ligge hos Vikafossen, så identen og enheten blir `SKJERMET`.
     * Det er trygt for brukeren, og statistikken skal ikke stoppe opp fordi entra-proxy feiler.
     */
    private val skjermingsoppslag = object : Skjermingsoppslag {
        override suspend fun gjelderKode6(fnr: Fnr): Boolean =
            personKlient.hentEnkelPerson(fnr).adressebeskyttelse.erKode6

        override suspend fun harRolleForStrengtFortroligAdresse(navIdent: String): Boolean =
            rolleForStrengtFortroligAdresseService.harRolleForStrengtFortroligAdresse(navIdent).getOrElse { false }
    }

    suspend fun generer(
        vararg hendelse: Statistikkhendelse,
    ): StatistikkDTO {
        return generer(Statistikkhendelser(*hendelse))
    }

    suspend fun generer(
        hendelser: Statistikkhendelser,
    ): StatistikkDTO {
        return hendelser.tilStatistikkDto(
            skjermingsoppslag = skjermingsoppslag,
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
