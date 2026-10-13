package no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeLokalKlient
import no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.infra.repo.LokalTiltaksdeltakelsePostgresRepo
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.tilTiltakstype
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseSomIkkeGirRett
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.erLønnstilskudd
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

/**
 * Oppretter eller endrer tiltaksdeltakelsen [TiltaksdeltakelseFakeLokalKlient] leverer for en deltaker ved lokal kjøring.
 * En eksisterende deltakelse finnes på ekstern id, og kun feltene som er satt i kommandoen endres.
 * Finnes den ikke, opprettes en ny på saken i kommandoen, med generert ekstern id dersom den mangler.
 * Nye deltakelser kan være tiltakstyper som gir rett eller lønnstilskudd, slik at visningen av lønnstilskudd kan testes lokalt.
 */
class LokalTiltaksdeltakelseService(
    private val tiltaksdeltakelseKlient: TiltaksdeltakelseFakeLokalKlient,
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
    private val sakRepo: SakRepo,
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) {
    fun opprettEllerOppdater(
        command: OpprettEllerOppdaterLokalTiltaksdeltakelseCommand,
    ): Either<KunneIkkeLagreLokalTiltaksdeltakelse, LagretLokalTiltaksdeltakelse> {
        val eksisterende = command.eksternDeltakelseId?.let { finnEksisterende(it) }
        return when {
            eksisterende != null -> oppdater(eksisterende.first, eksisterende.second, command)
            command.sakId != null -> opprett(command.sakId, command)
            else -> KunneIkkeLagreLokalTiltaksdeltakelse.FantIkkeDeltakelse.left()
        }
    }

    private fun oppdater(
        fnr: Fnr,
        gjeldende: LokalTiltaksdeltakelse,
        command: OpprettEllerOppdaterLokalTiltaksdeltakelseCommand,
    ): Either<KunneIkkeLagreLokalTiltaksdeltakelse, LagretLokalTiltaksdeltakelse> {
        if (command.sakId != null && sakRepo.hentFnrForSakId(command.sakId) != fnr) {
            return KunneIkkeLagreLokalTiltaksdeltakelse.TilhørerAnnenSak.left()
        }
        if (command.tiltakskode != null && command.tiltakskode != gjeldende.tiltakskode) {
            return KunneIkkeLagreLokalTiltaksdeltakelse.KanIkkeEndres("tiltakstype").left()
        }
        if (command.kilde != null && command.kilde != gjeldende.kilde) {
            return KunneIkkeLagreLokalTiltaksdeltakelse.KanIkkeEndres("kilde").left()
        }
        val oppdatert = gjeldende.endre(command)
        valider(oppdatert).onLeft { return it.left() }

        val nå = nå(clock)
        val deltaker = tiltaksdeltakerRepo.hentTiltaksdeltaker(oppdatert.eksternDeltakelseId)
        sessionFactory.withTransactionContext { tx ->
            sessionFactory.withTransaction(tx) { session ->
                LokalTiltaksdeltakelsePostgresRepo.lagre(fnr, oppdatert, nå, session)
            }
            // Kafka-consumerne registrerer kun endringer på deltakere vi kjenner, og lønnstilskudd lagres aldri som deltaker.
            // Tilbakedatert med forsinkelsen, slik at endringen plukkes opp ved neste kjøring av jobben i stedet for om et kvarter.
            if (deltaker != null) {
                tiltaksdeltakerRepo.registrerUbehandletEndring(
                    id = deltaker.id,
                    sakId = deltaker.sakId,
                    tidspunkt = nå.minusMinutes(OppdatertTiltaksdeltakelseJobb.MINUTTER_FORSINKELSE),
                    sessionContext = tx,
                )
            }
        }
        return LagretLokalTiltaksdeltakelse(oppdatert, opprettet = false).right()
    }

    private fun opprett(
        sakId: SakId,
        command: OpprettEllerOppdaterLokalTiltaksdeltakelseCommand,
    ): Either<KunneIkkeLagreLokalTiltaksdeltakelse, LagretLokalTiltaksdeltakelse> {
        val fnr = sakRepo.hentFnrForSakId(sakId) ?: return KunneIkkeLagreLokalTiltaksdeltakelse.FantIkkeSak.left()
        val tiltakskode = command.tiltakskode ?: return KunneIkkeLagreLokalTiltaksdeltakelse.ManglerTiltakstype.left()
        val tiltakstypeSomGirRett = TiltakstypeSomGirRettDTO.entries.find { it.name == tiltakskode }
        val kilde = command.kilde ?: standardkilde(tiltakskode, girRett = tiltakstypeSomGirRett != null)
        val eksternId = command.eksternDeltakelseId ?: genererEksternId(kilde)
        val deltaker = tiltaksdeltakerRepo.hentTiltaksdeltaker(eksternId)
        if (deltaker != null && deltaker.sakId != sakId) {
            return KunneIkkeLagreLokalTiltaksdeltakelse.TilhørerAnnenSak.left()
        }

        val ny: LokalTiltaksdeltakelse = if (tiltakstypeSomGirRett != null) {
            LokalTiltaksdeltakelse.GirRett(
                TiltaksdeltakelseIntern(
                    eksternDeltakelseId = eksternId,
                    gjennomføringId = null,
                    typeNavn = command.typeNavn ?: tiltakstypeSomGirRett.tilTiltakstype().navn,
                    typeKode = tiltakstypeSomGirRett,
                    rettPåTiltakspenger = true,
                    deltakelseFraOgMed = command.fraOgMed,
                    deltakelseTilOgMed = command.tilOgMed,
                    deltakelseStatus = command.status ?: TiltakDeltakerstatus.Deltar,
                    deltakelseProsent = command.deltakelseProsent ?: 100f,
                    antallDagerPerUke = command.antallDagerPerUke ?: 5f,
                    kilde = kilde,
                    deltidsprosentGjennomforing = null,
                    // Erstattes av deltakerens id ved lagring, slik oppslaget i saksopplysningene ville gjort.
                    internDeltakelseId = TiltaksdeltakerId.random(),
                ),
            )
        } else {
            LokalTiltaksdeltakelse.GirIkkeRett(
                TiltaksdeltakelseSomIkkeGirRett(
                    eksternDeltakelseId = eksternId,
                    typeNavn = command.typeNavn ?: standardnavn(tiltakskode),
                    tiltakskodeFraKilden = tiltakskode,
                    deltakelseFraOgMed = command.fraOgMed,
                    deltakelseTilOgMed = command.tilOgMed,
                    deltakelseStatus = command.status ?: TiltakDeltakerstatus.Deltar,
                    deltakelseProsent = command.deltakelseProsent ?: 100f,
                    antallDagerPerUke = command.antallDagerPerUke ?: 5f,
                    kilde = kilde,
                ),
            )
        }
        val libsDeltakelse = valider(ny).getOrElse { return it.left() }
        if (libsDeltakelse is Tiltaksdeltakelse.GirIkkeRett && !libsDeltakelse.erLønnstilskudd) {
            return KunneIkkeLagreLokalTiltaksdeltakelse.UkjentTiltakstype(tiltakskode).left()
        }

        val lagret = sessionFactory.withTransactionContext { tx ->
            val medInternId = when (ny) {
                is LokalTiltaksdeltakelse.GirRett -> LokalTiltaksdeltakelse.GirRett(
                    ny.deltakelse.copy(
                        internDeltakelseId = tiltaksdeltakerRepo.hentEllerLagre(
                            eksternId = eksternId,
                            tiltakstype = ny.deltakelse.typeKode.tilTiltakstype(),
                            sakId = sakId,
                            sessionContext = tx,
                        ),
                    ),
                )

                is LokalTiltaksdeltakelse.GirIkkeRett -> ny
            }
            sessionFactory.withTransaction(tx) { session ->
                LokalTiltaksdeltakelsePostgresRepo.lagre(fnr, medInternId, nå(clock), session)
            }
            medInternId
        }
        return LagretLokalTiltaksdeltakelse(lagret, opprettet = true).right()
    }

    /**
     * Fødselsnummeret slås først opp i de satte deltakelsene, og deretter via deltakeren og saken.
     */
    private fun finnEksisterende(eksternId: String): Pair<Fnr, LokalTiltaksdeltakelse>? {
        val fnr = sessionFactory.withSession { session -> LokalTiltaksdeltakelsePostgresRepo.hentFnr(eksternId, session) }
            ?: tiltaksdeltakerRepo.hentTiltaksdeltaker(eksternId)?.let { sakRepo.hentFnrForSakId(it.sakId) }
            ?: return null
        val deltakelse = tiltaksdeltakelseKlient.hentLokaleTiltaksdeltakelser(fnr)
            .find { it.eksternDeltakelseId == eksternId }
            ?: return null
        return fnr to deltakelse
    }

    /**
     * Klienten mapper til tiltakshistorikkens modell ved hvert oppslag, så en deltakelse den ikke kan mappe skal avvises her.
     */
    private fun valider(deltakelse: LokalTiltaksdeltakelse): Either<KunneIkkeLagreLokalTiltaksdeltakelse, Tiltaksdeltakelse> =
        Either.catch { deltakelse.tilLibsDeltakelse() }.mapLeft {
            KunneIkkeLagreLokalTiltaksdeltakelse.UgyldigDeltakelse(it.message ?: it.toString())
        }

    private fun standardnavn(tiltakskode: String): String =
        teamTiltaknavn[tiltakskode]
            ?: TiltakResponsDTO.TiltakTypeDTO.entries.find { it.name == tiltakskode }?.navn
            ?: tiltakskode

    private fun standardkilde(tiltakskode: String, girRett: Boolean): Tiltakskilde = when {
        girRett -> Tiltakskilde.Komet
        tiltakskode in teamTiltaknavn -> Tiltakskilde.TeamTiltak
        else -> Tiltakskilde.Arena
    }

    private fun genererEksternId(kilde: Tiltakskilde): String = generateSequence {
        when (kilde) {
            Tiltakskilde.Arena -> "TA${Random.nextInt(1_000_000, 10_000_000)}"
            Tiltakskilde.Komet, Tiltakskilde.TeamTiltak -> UUID.randomUUID().toString()
        }
    }.first { finnEksisterende(it) == null && tiltaksdeltakerRepo.hentTiltaksdeltaker(it) == null }

    private companion object {
        // Team Tiltaks koder finnes ikke blant Arena-typene i libs, og får derfor navnet her.
        val teamTiltaknavn = mapOf(
            "MIDLERTIDIG_LONNSTILSKUDD" to "Midlertidig lønnstilskudd",
            "VARIG_LONNSTILSKUDD" to "Varig lønnstilskudd",
        )
    }
}

/**
 * Enten [eksternDeltakelseId] eller [sakId] må være satt.
 * Uten en eksisterende deltakelse på [eksternDeltakelseId] opprettes en ny på [sakId], og da er [tiltakskode] påkrevd.
 * [tiltakskode] og [kilde] kan ikke endres på en eksisterende deltakelse.
 *
 * @param tiltakskode navnet på en [TiltakstypeSomGirRettDTO] eller kildens kode for et lønnstilskudd, f.eks. `MIDLONTIL`.
 */
data class OpprettEllerOppdaterLokalTiltaksdeltakelseCommand(
    val eksternDeltakelseId: String?,
    val sakId: SakId?,
    val tiltakskode: String?,
    val typeNavn: String?,
    val kilde: Tiltakskilde?,
    val fraOgMed: LocalDate?,
    val tilOgMed: LocalDate?,
    val status: TiltakDeltakerstatus?,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
) {
    init {
        require(eksternDeltakelseId != null || sakId != null) { "Enten eksternDeltakelseId eller sakId må være satt" }
    }
}

data class LagretLokalTiltaksdeltakelse(
    val deltakelse: LokalTiltaksdeltakelse,
    val opprettet: Boolean,
)

sealed interface KunneIkkeLagreLokalTiltaksdeltakelse {
    data object FantIkkeDeltakelse : KunneIkkeLagreLokalTiltaksdeltakelse

    data object FantIkkeSak : KunneIkkeLagreLokalTiltaksdeltakelse

    data object TilhørerAnnenSak : KunneIkkeLagreLokalTiltaksdeltakelse

    data object ManglerTiltakstype : KunneIkkeLagreLokalTiltaksdeltakelse

    data class UkjentTiltakstype(val tiltakskode: String) : KunneIkkeLagreLokalTiltaksdeltakelse

    data class KanIkkeEndres(val felt: String) : KunneIkkeLagreLokalTiltaksdeltakelse

    data class UgyldigDeltakelse(val melding: String) : KunneIkkeLagreLokalTiltaksdeltakelse
}
