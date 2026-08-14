package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import kotliquery.Row
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.tilDbPeriode
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.åpenPeriodeOrNull
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseKilde
import java.time.Clock
import java.time.LocalDateTime

// TODO: Klassen står i whitelisten til RepoKonvensjonKonsistTest fordi den ikke har et `Repo`-grensesnitt.
//  Unntaket er ikke målet: et repo som nås fra en service skal nås gjennom en port i domenet.
//  Fiksen henger sammen med at `TiltaksdeltakerHendelse` ligger i `infra/kafka/hendelse` — en port ville dratt infra-typen inn i domenet, så typen må flyttes først.
class TiltaksdeltakerHendelsePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) {

    fun hentDeltakereMedUbehandledeHendelser(minutterForsinkelse: Long): List<TiltaksdeltakerId> =
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        select distinct tiltaksdeltaker_id
                        from tiltaksdeltaker_kafka
                        where behandlet_tidspunkt is null
                          and sist_oppdatert < :sist_oppdatert
                    """.trimIndent(),
                    "sist_oppdatert" to nå(clock).minusMinutes(minutterForsinkelse),
                ).map { row -> TiltaksdeltakerId.fromString(row.string("tiltaksdeltaker_id")) }.asList,
            )
        }

    fun hentUbehandledeForDeltaker(
        internDeltakerId: TiltaksdeltakerId,
        minutterForsinkelse: Long,
    ): List<TiltaksdeltakerHendelse> =
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        select *
                        from tiltaksdeltaker_kafka
                        where behandlet_tidspunkt is null
                          and tiltaksdeltaker_id = :tiltaksdeltaker_id
                          and sist_oppdatert < :sist_oppdatert
                        order by sist_oppdatert asc
                    """.trimIndent(),
                    "tiltaksdeltaker_id" to internDeltakerId.toString(),
                    "sist_oppdatert" to nå(clock).minusMinutes(minutterForsinkelse),
                ).map { row -> row.tilTiltaksdeltakerHendelse() }.asList,
            )
        }

    /**
     * En ny hendelse har per definisjon ingen behandling ennå.
     * `behandling_id` settes først av [markerSomBehandletMedRevurdering], og utelates derfor her.
     */
    fun lagre(
        tiltaksdeltakerHendelse: TiltaksdeltakerHendelse,
        melding: String,
        kilde: TiltaksdeltakerHendelseKilde,
        sistOppdatert: LocalDateTime = nå(clock),
    ) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        insert into tiltaksdeltaker_kafka (
                            hendelse_id,
                            deltaker_id,
                            deltakelse_periode,
                            dager_per_uke,
                            deltakelsesprosent,
                            deltakerstatus,
                            sak_id,
                            oppgave_id,
                            sist_oppdatert,
                            melding,
                            tiltaksdeltaker_id,
                            kilde
                        ) values (
                            :hendelse_id,
                            :deltaker_id,
                            :deltakelse_periode::periode_open,
                            :dager_per_uke,
                            :deltakelsesprosent,
                            :deltakerstatus,
                            :sak_id,
                            :oppgave_id,
                            :sist_oppdatert,
                            :melding,
                            :tiltaksdeltaker_id,
                            :kilde
                        )
                    """.trimIndent(),
                    "hendelse_id" to tiltaksdeltakerHendelse.id.toString(),
                    "deltaker_id" to tiltaksdeltakerHendelse.eksternDeltakerId,
                    "deltakelse_periode" to tilDbPeriode(
                        tiltaksdeltakerHendelse.deltakelseFraOgMed,
                        tiltaksdeltakerHendelse.deltakelseTilOgMed,
                    ),
                    "dager_per_uke" to tiltaksdeltakerHendelse.dagerPerUke,
                    "deltakelsesprosent" to tiltaksdeltakerHendelse.deltakelsesprosent,
                    "deltakerstatus" to tiltaksdeltakerHendelse.deltakerstatus.name,
                    "sak_id" to tiltaksdeltakerHendelse.sakId.toString(),
                    "oppgave_id" to tiltaksdeltakerHendelse.oppgaveId?.toString(),
                    "sist_oppdatert" to sistOppdatert,
                    "melding" to melding,
                    "tiltaksdeltaker_id" to tiltaksdeltakerHendelse.internDeltakerId.toString(),
                    "kilde" to kilde.name,
                ).asUpdate,
            )
        }
    }

    fun markerSomBehandletOgIgnorert(id: TiltaksdeltakerHendelseId) {
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        update tiltaksdeltaker_kafka 
                        set behandlet_tidspunkt = :behandlet_tidspunkt
                        where hendelse_id = :hendelse_id
                    """.trimIndent(),
                    "behandlet_tidspunkt" to nå(clock),
                    "hendelse_id" to id.toString(),
                ).asUpdate,
            )
        }
    }

    fun markerSomBehandletMedOppgave(id: TiltaksdeltakerHendelseId, oppgaveId: OppgaveId) {
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        update tiltaksdeltaker_kafka 
                        set behandlet_tidspunkt = :behandlet_tidspunkt,
                            oppgave_id = :oppgave_id
                        where hendelse_id = :hendelse_id
                    """.trimIndent(),
                    "behandlet_tidspunkt" to nå(clock),
                    "oppgave_id" to oppgaveId.toString(),
                    "hendelse_id" to id.toString(),
                ).asUpdate,
            )
        }
    }

    fun markerSomBehandletMedRevurdering(id: TiltaksdeltakerHendelseId, behandlingId: RammebehandlingId) {
        sessionFactory.withSession {
            it.run(
                sqlQuery(
                    """
                        update tiltaksdeltaker_kafka 
                        set behandlet_tidspunkt = :behandlet_tidspunkt,
                            behandling_id = :behandling_id
                        where hendelse_id = :hendelse_id
                    """.trimIndent(),
                    "behandlet_tidspunkt" to nå(clock),
                    "behandling_id" to behandlingId.toString(),
                    "hendelse_id" to id.toString(),
                ).asUpdate,
            )
        }
    }
}

/**
 * Toppnivå fordi testlaget gjør sine egne oppslag mot `tiltaksdeltaker_kafka` med egen SQL, og skal slippe å duplisere mappingen.
 * Mappingen brukes av prodspørringene i [TiltaksdeltakerHendelsePostgresRepo], så den hører hjemme her.
 */
fun Row.tilTiltaksdeltakerHendelse(): TiltaksdeltakerHendelse {
    val deltakelse = åpenPeriodeOrNull("deltakelse_periode")
    return TiltaksdeltakerHendelse(
        id = TiltaksdeltakerHendelseId.fromString(string("hendelse_id")),
        eksternDeltakerId = string("deltaker_id"),
        deltakelseFraOgMed = deltakelse?.fraOgMed,
        deltakelseTilOgMed = deltakelse?.tilOgMed,
        dagerPerUke = floatOrNull("dager_per_uke"),
        deltakelsesprosent = floatOrNull("deltakelsesprosent"),
        deltakerstatus = TiltakDeltakerstatus.valueOf(string("deltakerstatus")),
        sakId = SakId.fromString(string("sak_id")),
        oppgaveId = stringOrNull("oppgave_id")?.let { OppgaveId(it) },
        internDeltakerId = TiltaksdeltakerId.fromString(string("tiltaksdeltaker_id")),
        behandlingId = stringOrNull("behandling_id")?.let { RammebehandlingId.fromString(it) },
    )
}
