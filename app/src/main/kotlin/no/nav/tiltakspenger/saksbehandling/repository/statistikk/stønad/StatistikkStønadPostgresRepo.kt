package no.nav.tiltakspenger.saksbehandling.repository.statistikk.stønad

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.statistikk.stønad.StatistikkStønadDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.statistikk.stønad.StatistikkUtbetalingDTO
import org.intellij.lang.annotations.Language
import java.time.Clock

class StatistikkStønadPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) : StatistikkStønadRepo {
    override fun lagre(
        dto: StatistikkStønadDTO,
        context: TransactionContext?,
    ) {
        sessionFactory.withTransaction(context) { tx ->
            lagre(dto, tx)
        }
    }

    internal fun lagre(
        dto: StatistikkStønadDTO,
        tx: TransactionalSession,
    ) {
        tx.run(
            queryOf(
                lagreSql,
                mapOf(
                    "id" to dto.id.toString(),
                    "brukerId" to dto.brukerId,
                    "sakId" to dto.sakId,
                    "saksnummer" to dto.saksnummer,
                    "resultat" to dto.resultat,
                    "sakDato" to dto.sakDato,
                    "gyldigFraDato" to dto.sakFraDato,
                    "gyldigTilDato" to dto.sakTilDato,
                    "ytelse" to dto.ytelse,
                    "soknadId" to dto.søknadId,
                    "opplysning" to dto.opplysning,
                    "soknadDato" to dto.søknadDato,
                    "gyldigFraDatoSoknad" to dto.søknadFraDato,
                    "gyldigTilDatoSoknad" to dto.søknadTilDato,
                    "vedtakId" to dto.vedtakId,
                    "type" to dto.vedtaksType,
                    "vedtakDato" to dto.vedtakDato,
                    "fom" to dto.vedtakFom,
                    "tom" to dto.vedtakTom,
                    "oppfolgingEnhetKode" to null,
                    "oppfolgingEnhetNavn" to null,
                    "beslutningEnhetKode" to null,
                    "beslutningEnhetNavn" to null,
                    "tilhorighetEnhetKode" to null,
                    "tilhorighetEnhetNavn" to null,
                    "vilkarId" to null,
                    "vilkarType" to null,
                    "vilkarStatus" to null,
                    "lovparagraf" to null,
                    "beskrivelse" to null,
                    "gyldigFraDatoVilkar" to null,
                    "gyldigTilDatoVilkar" to null,
                    // Er dette tiltak pr utbetaling, eller tiltak på rammevedtaket?
                    "tiltakId" to null,
                    "tiltakType" to null,
                    "tiltakBeskrivelse" to null,
                    "fagsystem" to null,
                    "tiltakDato" to null,
                    "gyldigFraDatoTiltak" to null,
                    "gyldigTilDatoTiltak" to null,
                    "sistEndret" to nå(clock),
                    "opprettet" to nå(clock),
                ),
            ).asUpdate,
        )
    }

    override fun lagre(
        dto: StatistikkUtbetalingDTO,
        context: TransactionContext?,
    ) {
        sessionFactory.withTransaction(context) { tx ->
            lagre(dto, tx)
        }
    }

    fun lagre(
        dto: StatistikkUtbetalingDTO,
        tx: TransactionalSession,
    ) {
        tx.run(
            queryOf(
                lagreUtbetalingSql,
                mapOf(
                    "id" to dto.id,
                    "sakId" to dto.sakId,
                    "saksnummer" to dto.saksnummer,
                    "belop" to dto.totalBeløp,
                    "ordinaerBelop" to dto.ordinærBeløp,
                    "barnetilleggBelop" to dto.barnetilleggBeløp,
                    "belopBeskrivelse" to dto.beløpBeskrivelse,
                    "arsak" to dto.årsak,
                    "posteringsDato" to dto.posteringDato,
                    "gyldigFraDato" to dto.gyldigFraDatoPostering,
                    "gyldigTilDato" to dto.gyldigTilDatoPostering,
                ),
            ).asUpdate,
        )
    }

    @Language("SQL")
    private val lagreSql =
        """
        insert into statistikk_stonad (
        id,
        bruker_id,
        sak_id,
        saksnummer,
        resultat,
        sak_dato,
        gyldig_fra_dato,
        gyldig_til_dato,
        ytelse,
        søknad_id,
        opplysning,
        søknad_dato,
        gyldig_fra_dato_søknad,
        gyldig_til_dato_søknad,
        vedtak_id,
        type,
        vedtak_dato,
        fra_og_med,
        til_og_med,
        oppfølging_enhet_kode,
        oppfølging_enhet_navn,
        beslutning_enhet_kode,
        beslutning_enhet_navn,
        tilhørighet_enhet_kode,
        tilhørighet_enhet_navn,
        vilkår_id,
        vilkår_type,
        vilkår_status,
        lovparagraf,
        beskrivelse,
        gyldig_fra_dato_vilkår,
        gyldig_til_dato_vilkår,
        tiltak_id,
        tiltak_type,
        tiltak_beskrivelse,
        fagsystem,
        tiltak_dato,
        gyldig_fra_dato_tiltak,
        gyldig_til_dato_tiltak,
        sist_endret,
        opprettet       
        ) values (
        :id,
        :brukerId,
        :sakId,
        :saksnummer,
        :resultat,
        :sakDato,
        :gyldigFraDato,
        :gyldigTilDato,
        :ytelse,
        :soknadId,
        :opplysning,
        :soknadDato,
        :gyldigFraDatoSoknad,
        :gyldigTilDatoSoknad,
        :vedtakId,
        :type,
        :vedtakDato,
        :fom,
        :tom,
        :oppfolgingEnhetKode,
        :oppfolgingEnhetNavn,
        :beslutningEnhetKode,
        :beslutningEnhetNavn,
        :tilhorighetEnhetKode,
        :tilhorighetEnhetNavn,
        :vilkarId,
        :vilkarType,
        :vilkarStatus,
        :lovparagraf,
        :beskrivelse,
        :gyldigFraDatoVilkar,
        :gyldigTilDatoVilkar,
        :tiltakId,
        :tiltakType,
        :tiltakBeskrivelse,
        :fagsystem,
        :tiltakDato,
        :gyldigFraDatoTiltak,
        :gyldigTilDatoTiltak,
        :sistEndret,
        :opprettet
        )
        """.trimIndent()

    @Language("SQL")
    private val lagreUtbetalingSql =
        """
        insert into statistikk_utbetaling (
        id,
        sak_id,
        saksnummer,
        beløp,
        ordinær_beløp,
        barnetillegg_beløp,
        beløp_beskrivelse,
        årsak,
        posteringsdato,
        gyldig_fra_dato,
        gyldig_til_dato     
        ) values (
        :id,
        :sakId,
        :saksnummer,
        :belop,
        :ordinaerBelop,
        :barnetilleggBelop,
        :belopBeskrivelse,
        :arsak,
        :posteringsDato,
        :gyldigFraDato,
        :gyldigTilDato
        )
        """.trimIndent()
}
