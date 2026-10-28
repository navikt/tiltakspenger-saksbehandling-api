package no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.felles.lukketEllerÅpenPeriode
import no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeLokalKlient
import no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.infra.repo.LokalTiltaksdeltakelsePostgresRepo
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.tilTiltakstype
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseSomFratarRett
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

/**
 * Oppretter eller endrer tiltaksdeltakelsen [TiltaksdeltakelseFakeLokalKlient] leverer for en deltaker ved lokal kjøring.
 * En eksisterende deltakelse finnes på ekstern id, og kun feltene som er satt i kommandoen endres.
 * Finnes den ikke, opprettes en ny på saken i kommandoen, med generert ekstern id dersom den mangler.
 * Nye deltakelser kan være tiltakstyper som gir eller fratar rett, slik at visningen av deltakelser som fratar rett (lønnstilskudd) kan testes lokalt.
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
            // Kafka-consumerne registrerer kun endringer på deltakere vi kjenner, og deltakelser som fratar rett lagres aldri som deltaker.
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
        if (tiltakstypeSomGirRett == null && tiltakskode !in koderSomFratarRett) {
            return KunneIkkeLagreLokalTiltaksdeltakelse.UkjentTiltakstype(tiltakskode).left()
        }
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
                    periode = lukketEllerÅpenPeriode(command.fraOgMed, command.tilOgMed),
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
            LokalTiltaksdeltakelse.FratarRett(
                TiltaksdeltakelseSomFratarRett(
                    eksternDeltakelseId = eksternId,
                    typeNavn = command.typeNavn ?: standardnavn(tiltakskode),
                    tiltakskodeFraKilden = tiltakskode,
                    periode = lukketEllerÅpenPeriode(command.fraOgMed, command.tilOgMed),
                    deltakelseStatus = command.status ?: TiltakDeltakerstatus.Deltar,
                    deltakelseProsent = command.deltakelseProsent ?: 100f,
                    antallDagerPerUke = command.antallDagerPerUke ?: 5f,
                    kilde = kilde,
                ),
            )
        }
        valider(ny).getOrElse { return it.left() }

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

                is LokalTiltaksdeltakelse.FratarRett -> ny
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
        // Speiler kodetabellene i libs (arenakoderSomFratarRett og teamTiltakkoderSomFratarRett), som er internal der.
        val koderSomFratarRett = setOf(
            "FIREARIG_LONNSTILSKUDD",
            "FLEKSJOBB",
            "LONNTIL",
            "LONNTILAAP",
            "LONNTILL",
            "LONNTILS",
            "MIDLONTIL",
            "REAKTUFOR",
            "TIDSUBLONN",
            "UFØREPENLØ",
            "VARLONTIL",
            "MIDLERTIDIG_LONNSTILSKUDD",
            "VARIG_LONNSTILSKUDD",
        )

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
 * @param tiltakskode navnet på en [TiltakstypeSomGirRettDTO] eller kildens kode for en tiltakstype som fratar rett, f.eks. `MIDLONTIL`.
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
