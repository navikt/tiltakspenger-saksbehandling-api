package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.tilDbPeriode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseKilde
import java.time.Clock

// TODO: Klassen står i whitelisten til RepoKonvensjonKonsistTest fordi den ikke har et `Repo`-grensesnitt.
//  Unntaket er ikke målet: et repo som nås fra en service skal nås gjennom en port i domenet.
//  Innfør en port for lagring av hendelseshistorikken i domenet for å fjerne unntaket.
class TiltaksdeltakerHendelsePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) {

    /**
     * Bevarer hendelsen som historikk.
     * Behandling av endringer styres av markøren på tiltaksdeltakeren, ikke av hendelsesraden.
     */
    fun lagre(
        tiltaksdeltakerHendelse: TiltaksdeltakerHendelse,
        melding: String,
        kilde: TiltaksdeltakerHendelseKilde,
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
                    "sist_oppdatert" to nå(clock),
                    "melding" to melding,
                    "tiltaksdeltaker_id" to tiltaksdeltakerHendelse.internDeltakerId.toString(),
                    "kilde" to kilde.name,
                ).asUpdate,
            )
        }
    }
}
