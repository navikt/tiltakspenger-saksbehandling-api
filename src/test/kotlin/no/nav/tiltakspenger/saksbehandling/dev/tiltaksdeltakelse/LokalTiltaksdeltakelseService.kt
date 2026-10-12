package no.nav.tiltakspenger.saksbehandling.dev.tiltaksdeltakelse

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.dev.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeLokalKlient
import no.nav.tiltakspenger.saksbehandling.dev.tiltaksdeltakelse.infra.repo.LokalTiltaksdeltakelsePostgresRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse
import java.time.Clock
import java.time.LocalDate

/**
 * Setter tiltaksdeltakelsen [TiltaksdeltakelseFakeLokalKlient] leverer for en deltaker ved lokal kjøring, og simulerer en hendelse fra Kafka.
 * Utgangspunktet er deltakelsen klienten leverer i dag, og kun feltene som er satt i kommandoen endres.
 */
class LokalTiltaksdeltakelseService(
    private val tiltaksdeltakelseKlient: TiltaksdeltakelseFakeLokalKlient,
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
    private val sakRepo: SakRepo,
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) {
    fun oppdater(command: OppdaterLokalTiltaksdeltakelseCommand): Either<KunneIkkeOppdatereLokalTiltaksdeltakelse, TiltaksdeltakelseIntern> {
        val deltaker = tiltaksdeltakerRepo.hentTiltaksdeltaker(command.eksternDeltakelseId)
            ?: return KunneIkkeOppdatereLokalTiltaksdeltakelse.FantIkkeDeltaker.left()
        val fnr = sakRepo.hentFnrForSakId(deltaker.sakId)!!
        val gjeldende = tiltaksdeltakelseKlient.hentTiltaksdeltakelserIntern(fnr)
            .find { it.eksternDeltakelseId == command.eksternDeltakelseId }
            ?: return KunneIkkeOppdatereLokalTiltaksdeltakelse.FantIkkeDeltakelse.left()

        val oppdatert = gjeldende.copy(
            deltakelseFraOgMed = command.fraOgMed ?: gjeldende.deltakelseFraOgMed,
            deltakelseTilOgMed = command.tilOgMed ?: gjeldende.deltakelseTilOgMed,
            deltakelseStatus = command.status ?: gjeldende.deltakelseStatus,
            deltakelseProsent = command.deltakelseProsent ?: gjeldende.deltakelseProsent,
            antallDagerPerUke = command.antallDagerPerUke ?: gjeldende.antallDagerPerUke,
        )
        // Klienten mapper til tiltakshistorikkens modell ved hvert oppslag, så en deltakelse den ikke kan mappe skal avvises her.
        Either.catch { oppdatert.tilLibsDeltakelse() }.getOrElse {
            return KunneIkkeOppdatereLokalTiltaksdeltakelse.UgyldigDeltakelse(it.message ?: it.toString()).left()
        }

        val nå = nå(clock)
        sessionFactory.withTransactionContext { tx ->
            sessionFactory.withTransaction(tx) { session ->
                LokalTiltaksdeltakelsePostgresRepo.lagre(fnr, oppdatert, nå, session)
            }
            // Tilbakedatert med forsinkelsen, slik at endringen plukkes opp ved neste kjøring av jobben i stedet for om et kvarter.
            tiltaksdeltakerRepo.registrerUbehandletEndring(
                id = deltaker.id,
                sakId = deltaker.sakId,
                tidspunkt = nå.minusMinutes(OppdatertTiltaksdeltakelseJobb.MINUTTER_FORSINKELSE),
                sessionContext = tx,
            )
        }
        return oppdatert.right()
    }
}

data class OppdaterLokalTiltaksdeltakelseCommand(
    val eksternDeltakelseId: String,
    val fraOgMed: LocalDate?,
    val tilOgMed: LocalDate?,
    val status: TiltakDeltakerstatus?,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
)

sealed interface KunneIkkeOppdatereLokalTiltaksdeltakelse {
    data object FantIkkeDeltaker : KunneIkkeOppdatereLokalTiltaksdeltakelse

    data object FantIkkeDeltakelse : KunneIkkeOppdatereLokalTiltaksdeltakelse

    data class UgyldigDeltakelse(val melding: String) : KunneIkkeOppdatereLokalTiltaksdeltakelse
}
