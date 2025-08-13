@file:Suppress("unused", "ktlint")

package db.migration

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresTransactionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer.toAttesteringer
import no.nav.tiltakspenger.saksbehandling.beregning.MeldekortBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.infra.repo.tilMeldeperiodeBeregningerFraMeldekort
import no.nav.tiltakspenger.saksbehandling.felles.toAttesteringer
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toAvbrutt
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.AvbruttMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingBegrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDager
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.BrukersMeldekortPostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.tilMeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.tilMeldekortBehandlingType
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.tilMeldekortDager
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.tilMeldekortDagerDbJson
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.toDbJson
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.toMeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.toMeldekortDager
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.toSimuleringFraDbJson
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

internal class V109__ikke_rett_status : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val logger = KotlinLogging.logger {}
        val dataSource = context.configuration.dataSource
        val sessionFactory = PostgresSessionFactory(dataSource, SessionCounter(logger))

        sessionFactory.withTransactionContext { tx ->
            val alleMeldekortBruker = tx.withSession { session ->
                session.run(
                    sqlQuery("""select * from meldekort_bruker""")
                        .map { row -> fromRowTilBrukersMeldekort(row, session) }.asList,
                )
            }

            val alleMeldekortBehandlinger = tx.withSession { session ->
                session.run(
                    sqlQuery("""select * from meldekortbehandling mb join sak s on mb.sak_id = s.id""")
                        .map { row -> fromRowTilMeldekortbehandling(row, session) }.asList,
                )
            }

            alleMeldekortBruker.forEach { meldekort ->
                val meldeperiode = meldekort.meldeperiode

                val dager = meldeperiode.girRett.toList().zip(meldekort.dager).map { (girRett, meldekortDag) ->
                    val harRettPåDenneDagen = girRett.second

                    if (harRettPåDenneDagen) {
                        //hvis bruker har rett, så skal dagen være utfylt med noe fra før (??)
                        BrukersMeldekort.BrukersMeldekortDag(
                            dato = meldekortDag.dato,
                            status = meldekortDag.status,
                        )
                    } else {
                        //har ikke rett - skal sette status til IKKE_RETT_TIL_TILTAKSPENGER (denne skal være IKKE_BESVART)
                        BrukersMeldekort.BrukersMeldekortDag(
                            dato = meldekortDag.dato,
                            status = InnmeldtStatus.IKKE_RETT_TIL_TILTAKSPENGER,
                        )
                    }
                }

                sqlQuery(
                    """update meldekort_bruker set dager = :dager where id = :id""",
                    "dager" to dager.toDbJson(),
                    "id" to meldekort.id.toString(),
                ).asUpdate
            }

            alleMeldekortBehandlinger.forEach { behandling ->
                val meldeperiode = behandling.meldeperiode
                val dager = meldeperiode.girRett.toList().zip(behandling.dager).map { (girRett, meldekortDag) ->
                    val harRettPåDenneDagen = girRett.second

                    if (harRettPåDenneDagen) {
                        //hvis bruker har rett, så skal dagen være utfylt med noe fra før (??)
                        MeldekortDag(
                            dato = meldekortDag.dato,
                            status = meldekortDag.status,
                        )
                    } else {
                        //har ikke rett - skal sette status til IKKE_RETT_TIL_TILTAKSPENGER (denne skal være IKKE_BESVART)
                        MeldekortDag(
                            dato = meldekortDag.dato,
                            status = MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
                        )
                    }
                }

                sqlQuery(
                    """update saksbehandling.public.meldekortbehandling set meldekortdager = :dager where id = :id""",
                    "dager" to MeldekortDager(verdi = dager, meldeperiode = meldeperiode).tilMeldekortDagerDbJson(),
                    "id" to behandling.id.toString(),
                ).asUpdate
            }
        }
    }
}


//copy-pasta fra MeldekortPostgresRepo.kt
private fun fromRowTilBrukersMeldekort(
    row: Row,
    session: Session,
): BrukersMeldekort {
    return BrukersMeldekort(
        id = MeldekortId.fromString(row.string("id")),
        mottatt = row.localDateTime("mottatt"),
        meldeperiode = MeldeperiodePostgresRepo.hentForMeldeperiodeId(
            MeldeperiodeId.fromString(row.string("meldeperiode_id")),
            session,
        )!!,
        sakId = SakId.fromString(row.string("sak_id")),
        dager = row.string("dager").toMeldekortDager(),
        journalpostId = JournalpostId(row.string("journalpost_id")),
        oppgaveId = row.stringOrNull("oppgave_id")?.let { OppgaveId(it) },
        behandlesAutomatisk = row.boolean("behandles_automatisk"),
        behandletAutomatiskStatus = row.stringOrNull("behandlet_automatisk_status")
            ?.tilMeldekortBehandletAutomatiskStatus(),
    )
}

//copy-pasta fra MeldekortBehandlingPostgresRepo.kt
private fun fromRowTilMeldekortbehandling(
    row: Row,
    session: Session,
): MeldekortBehandling {
    val meldeperiodeId = MeldeperiodeId.fromString(row.string("meldeperiode_id"))
    val meldeperiode = MeldeperiodePostgresRepo.hentForMeldeperiodeId(meldeperiodeId, session)!!

    val id = MeldekortId.fromString(row.string("id"))
    val sakId = SakId.fromString(row.string("sak_id"))
    val saksnummer =
        Saksnummer(row.string("saksnummer"))
    val navkontorEnhetsnummer = row.string("navkontor")
    val navkontorNavn = row.stringOrNull("navkontor_navn")
    val fnr = Fnr.fromString(row.string("fnr"))
    val opprettet = row.localDateTime("opprettet")
    val ikkeRettTilTiltakspengerTidspunkt = row.localDateTimeOrNull("ikke_rett_til_tiltakspenger_tidspunkt")
    val type = row.string("type").tilMeldekortBehandlingType()
    val begrunnelse = row.stringOrNull("begrunnelse")?.let { MeldekortBehandlingBegrunnelse(verdi = it) }

    val navkontor = Navkontor(kontornummer = navkontorEnhetsnummer, kontornavn = navkontorNavn)
    val attesteringer = row.string("attesteringer").toAttesteringer().toAttesteringer()

    val saksbehandler = row.stringOrNull("saksbehandler")

    val dager = row.string("meldekortdager").tilMeldekortDager(meldeperiode)
    val simulering = row.stringOrNull("simulering")
        ?.toSimuleringFraDbJson(MeldeperiodePostgresRepo.hentMeldeperiodekjederForSakId(sakId, session))

    val brukersMeldekort = row.stringOrNull("brukers_meldekort_id")?.let {
        BrukersMeldekortPostgresRepo.hentForMeldekortId(
            MeldekortId.fromString(it),
            session,
        )
    }

    val iverksattTidspunkt = row.localDateTimeOrNull("iverksatt_tidspunkt")

    val beregning = row.stringOrNull("beregninger")
        ?.tilMeldeperiodeBeregningerFraMeldekort(id)
        ?.let { MeldekortBeregning(it) }

    return when (val status = row.string("status").toMeldekortBehandlingStatus()) {
        MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET -> {
            requireNotNull(brukersMeldekort) {
                "Fant ikke brukers meldekort for automatisk meldekortbehandling $id"
            }

            MeldekortBehandletAutomatisk(
                id = id,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                opprettet = opprettet,
                navkontor = navkontor,
                brukersMeldekort = brukersMeldekort,
                meldeperiode = meldeperiode,
                beregning = beregning!!,
                simulering = simulering,
                dager = dager,
                type = type,
                status = status,
            )
        }

        MeldekortBehandlingStatus.GODKJENT, MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING, MeldekortBehandlingStatus.UNDER_BESLUTNING -> {
            MeldekortBehandletManuelt(
                id = id,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                opprettet = opprettet,
                navkontor = navkontor,
                ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                brukersMeldekort = brukersMeldekort,
                meldeperiode = meldeperiode,
                saksbehandler = saksbehandler!!,
                type = type,
                begrunnelse = begrunnelse,
                attesteringer = attesteringer,
                sendtTilBeslutning = row.localDateTimeOrNull("sendt_til_beslutning"),
                beslutter = row.stringOrNull("beslutter"),
                status = status,
                iverksattTidspunkt = iverksattTidspunkt,
                beregning = beregning!!,
                simulering = simulering,
                dager = dager,
            )
        }

        MeldekortBehandlingStatus.UNDER_BEHANDLING, MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING -> {
            MeldekortUnderBehandling(
                id = id,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                opprettet = opprettet,
                navkontor = navkontor,
                ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                brukersMeldekort = brukersMeldekort,
                meldeperiode = meldeperiode,
                saksbehandler = saksbehandler,
                type = type,
                begrunnelse = begrunnelse,
                attesteringer = attesteringer,
                sendtTilBeslutning = row.localDateTimeOrNull("sendt_til_beslutning"),
                beregning = beregning,
                simulering = simulering,
                dager = dager,
                status = status,
            )
        }

        MeldekortBehandlingStatus.AVBRUTT, MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> {
            AvbruttMeldekortBehandling(
                id = id,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                opprettet = opprettet,
                navkontor = navkontor,
                ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                brukersMeldekort = brukersMeldekort,
                meldeperiode = meldeperiode,
                saksbehandler = saksbehandler,
                type = type,
                begrunnelse = begrunnelse,
                attesteringer = attesteringer,
                beregning = beregning,
                simulering = simulering,
                dager = dager,
                avbrutt = row.stringOrNull("avbrutt")?.toAvbrutt(),
            )
        }
    }
}
