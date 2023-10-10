package no.nav.tiltakspenger.vedtak.repository.behandling

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import no.nav.tiltakspenger.domene.behandling.Behandling
import no.nav.tiltakspenger.domene.behandling.BehandlingIverksatt
import no.nav.tiltakspenger.domene.behandling.BehandlingVilkårsvurdert
import no.nav.tiltakspenger.domene.behandling.Søknadsbehandling
import no.nav.tiltakspenger.felles.BehandlingId
import no.nav.tiltakspenger.felles.Periode
import no.nav.tiltakspenger.felles.SakId
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.vedtak.db.DataSource
import org.intellij.lang.annotations.Language
import java.time.LocalDate

private val LOG = KotlinLogging.logger {}
private val SECURELOG = KotlinLogging.logger("tjenestekall")

// todo Må enten endres til å kunne hente og lagre alle typer behandlinger og ikke bare Søknadsbehandlinger
//      eller så må vi lage egne Repo for de andre type behandlingene
internal class PostgresBehandlingRepo(
    private val saksopplysningDAO: SaksopplysningDAO = SaksopplysningDAO(),
    private val vurderingDAO: VurderingDAO = VurderingDAO(),
) : BehandlingRepo {
    override fun hent(behandlingId: BehandlingId): Søknadsbehandling? {
        return sessionOf(DataSource.hikariDataSource).use {
            it.transaction { txSession ->
                txSession.run(
                    queryOf(
                        SqlHentBehandling,
                        mapOf(
                            "id" to behandlingId.toString(),
                        ),
                    ).map { row ->
                        row.toBehandling(txSession)
                    }.asSingle,
                )
            }
        }
    }

    override fun hentForSak(sakId: SakId): List<Søknadsbehandling> {
        return sessionOf(DataSource.hikariDataSource).use {
            it.transaction { txSession ->
                txSession.run(
                    queryOf(
                        SqlHentBehandlingForSak,
                        mapOf(
                            "sakId" to sakId,
                        ),
                    ).map { row ->
                        row.toBehandling(txSession)
                    }.asList,
                )
            }
        }
    }

    override fun lagre(behandling: Søknadsbehandling): Søknadsbehandling {
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction { txSession ->
                val sistEndret = hentSistEndret(behandling.id, txSession)
                if (sistEndret == null) {
                    opprettBehandling(behandling, txSession)
                } else {
                    oppdaterBehandling(sistEndret, behandling, txSession)
                }.also {
                    when (behandling) {
                        // søknadDAO.lagre(behandling.id, behandling.søknader)
                        is BehandlingIverksatt -> {
                            saksopplysningDAO.lagre(behandling.id, behandling.saksopplysninger)
                            vurderingDAO.lagre(behandling.id, behandling.vilkårsvurderinger)
                        }

                        is BehandlingVilkårsvurdert -> {
                            saksopplysningDAO.lagre(behandling.id, behandling.saksopplysninger)
                            vurderingDAO.lagre(behandling.id, behandling.vilkårsvurderinger)
                        }

                        is Søknadsbehandling.Opprettet -> {}
                    }
                }
            }
        }
        return behandling
    }

    private fun oppdaterBehandling(
        sistEndret: LocalDate,
        behandling: Søknadsbehandling,
        txSession: TransactionalSession,
    ): Behandling {
        SECURELOG.info { "Oppdaterer behandling ${behandling.id}" }

        val antRaderOppdatert = txSession.run(
            queryOf(
                SqlOppdaterBehandling,
                mapOf(
                    "id" to behandling.id.toString(),
                    "fom" to behandling.vurderingsperiode.fra,
                    "tom" to behandling.vurderingsperiode.til,
                    "tilstand" to finnTilstand(behandling),
                    "sistEndretOld" to sistEndret,
                    "sistEndret" to nå(),
                ),
            ).asUpdate,
        )
        if (antRaderOppdatert == 0) {
            throw IllegalStateException("Noen andre har endret denne behandlingen ${behandling.id}")
        }
        return behandling
    }

    private fun opprettBehandling(behandling: Søknadsbehandling, txSession: TransactionalSession): Behandling {
        SECURELOG.info { "Oppretter behandling ${behandling.id}" }

        txSession.run(
            queryOf(
                sqlOpprettBehandling,
                mapOf(
                    "id" to behandling.id.toString(),
                    "fom" to behandling.vurderingsperiode.fra,
                    "tom" to behandling.vurderingsperiode.til,
                    "tilstand" to finnTilstand(behandling),
                    "status" to finnStatus(behandling),
                    "sistEndret" to nå(),
                ),
            ).asUpdate,
        )
        return behandling
    }

    private fun hentSistEndret(behandlingId: BehandlingId, txSession: TransactionalSession): LocalDate? =
        txSession.run(
            queryOf(
                sqlHentSistEndret,
                mapOf(
                    "id" to behandlingId.toString(),
                ),
            ).map { row -> row.localDate("sist_endret") }.asSingle,
        )

    private fun Row.toBehandling(txSession: TransactionalSession): Søknadsbehandling {
        val id = BehandlingId.fromDb(string("id"))
        val fom = localDate("fom")
        val tom = localDate("tom")
        val status = string("status")
        val saksbehandler = string("saksbehandler")
        return when (val type = string("tilstand")) {
            "søknadsbehandling" -> Søknadsbehandling.Opprettet.fromDb(
                id = id,
                søknader = listOf(ObjectMother.nySøknadMedTiltak()),
                vurderingsperiode = Periode(fom, tom),
                saksopplysninger = saksopplysningDAO.hent(id),
            )

            "behandlingVilkårsvurdert" -> BehandlingVilkårsvurdert.fromDb(
                id = id,
                søknader = listOf(ObjectMother.nySøknadMedTiltak()),
                vurderingsperiode = Periode(fom, tom),
                saksopplysninger = saksopplysningDAO.hent(id),
                vilkårsvurderinger = vurderingDAO.hent(id),
                status = status,
            )

            "behandlingIverksatt" -> BehandlingIverksatt.fromDb(
                id = id,
                søknader = listOf(ObjectMother.nySøknadMedTiltak()),
                vurderingsperiode = Periode(fom, tom),
                saksopplysninger = saksopplysningDAO.hent(id),
                vilkårsvurderinger = vurderingDAO.hent(id),
                status = status,
                saksbehandler = saksbehandler,
            )

            else -> throw IllegalStateException("Hentet en Behandling $id med ukjent status : $type")
        }
    }

    private fun finnTilstand(behandling: Søknadsbehandling) =
        when (behandling) {
            is Søknadsbehandling.Opprettet -> "Opprettet"
            is BehandlingVilkårsvurdert -> "Vilkårsvurdert"
            is BehandlingIverksatt -> "Iverksatt"
        }

    private fun finnStatus(behandling: Søknadsbehandling) =
        when (behandling) {
            is Søknadsbehandling.Opprettet -> "Opprettet"
            is BehandlingVilkårsvurdert.Avslag -> "Avslag"
            is BehandlingVilkårsvurdert.Innvilget -> "Innvilget"
            is BehandlingVilkårsvurdert.Manuell -> "Manuell"
            is BehandlingIverksatt.Avslag -> "Avslag"
            is BehandlingIverksatt.Innvilget -> "Innvilget"
        }

    private val sqlHentSistEndret = """
        select sist_endret from behandling where id = :id
    """.trimIndent()

    @Language("SQL")
    private val sqlOpprettBehandling = """
        insert into behandling (
            id,
            sakId,
            fom,
            tom,
            tilstand,
            status,
            sist_endret
        ) values (
            :id,
            :sakId,
            :fom,
            :tom,
            :tilstand,
            :status,
            :sistEndret
        )
    """.trimIndent()

    @Language("SQL")
    private val SqlOppdaterBehandling = """
        update behandling set 
            fom = :fom,
            tom = :tom,
            sakId = :sakId,
            tilstand = :tilstand,
            status = :status,
            sist_endret = :sistEndret,
        where id = :id
          and sist_endret = :sistEndretOld
    """.trimIndent()

    @Language("SQL")
    private val SqlHentBehandling = """
        select * from behandling where id = :id
    """.trimIndent()

    @Language("SQL")
    private val SqlHentBehandlingForSak = """
        select * from behandling where sakId = :sakid
    """.trimIndent()

//    override fun hent(behandlingId: BehandlingId): Behandling? {
//
//
//        // TODO: Denne skal ikke opprette behandling på sikt, men skal hente ut fra databasen.
//        return Søknadsbehandling.Opprettet.opprettBehandling(
//            søknad = ObjectMother.nySøknadMedTiltak(),
//        )
//    }
}
