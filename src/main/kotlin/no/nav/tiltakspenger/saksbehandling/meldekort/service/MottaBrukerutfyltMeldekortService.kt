package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort.Companion.MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.LagreBrukersMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak

class MottaBrukerutfyltMeldekortService(
    private val brukersMeldekortRepo: BrukersMeldekortRepo,
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val sakService: SakService,
) {
    val logger = KotlinLogging.logger { }

    fun mottaBrukerutfyltMeldekort(kommando: LagreBrukersMeldekortKommando): Either<KanIkkeLagreBrukersMeldekort, Unit> {
        val sakId = kommando.sakId
        val meldekortId = kommando.id
        val meldeperiodeId = kommando.meldeperiodeId

        val meldeperiode = meldeperiodeRepo.hentForMeldeperiodeId(meldeperiodeId)

        requireNotNull(meldeperiode) {
            "Fant ikke meldeperioden $meldeperiodeId for meldekortet $meldekortId"
        }

        val sak = sakService.hentForSakId(sakId)

        val kanBehandlesAutomatisk = sak.kanBehandlesAutomatisk(kommando, meldeperiode)

        val nyttMeldekort = kommando.tilBrukersMeldekort(
            meldeperiode,
            behandlesAutomatisk = kanBehandlesAutomatisk.isRight(),
            behandletAutomatiskStatus = kanBehandlesAutomatisk.fold(
                ifLeft = { it },
                ifRight = { MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING },
            ),
        )

        Either.catch {
            brukersMeldekortRepo.lagre(nyttMeldekort)
        }.onLeft {
            val eksisterendeMeldekort = brukersMeldekortRepo.hentForMeldekortId(meldekortId)

            if (eksisterendeMeldekort == null) {
                with("Kunne ikke lagre brukers meldekort $meldekortId - Ukjent feil") {
                    logger.error(it) { this }
                    Sikkerlogg.error(it) { "$this - ${it.message}" }
                }
                return KanIkkeLagreBrukersMeldekort.UkjentFeil.left()
            }

            if (kommando.matcherBrukersMeldekort(eksisterendeMeldekort)) {
                logger.info { "Meldekortet med id $meldekortId var allerede lagret med samme data" }
                return KanIkkeLagreBrukersMeldekort.AlleredeLagretUtenDiff.left()
            }

            with("Kunne ikke lagre brukers meldekort $meldekortId - Meldekortet er allerede lagret med andre data") {
                logger.error(it) { this }
                Sikkerlogg.error(it) { "$this - ${it.message}" }
            }

            return KanIkkeLagreBrukersMeldekort.AlleredeLagretMedDiff.left()
        }

        return Unit.right()
    }

    private fun Sak.kanBehandlesAutomatisk(
        kommando: LagreBrukersMeldekortKommando,
        meldeperiode: Meldeperiode,
    ): Either<MeldekortBehandletAutomatiskStatus, Unit> {
        val kjedeId = meldeperiode.kjedeId
        val sakId = meldeperiode.sakId

        /** Denne sjekken tar ikke hensyn til om det ble opprettet en manuell meldekortbehandling på meldeperiodekjeden
         *  før første meldekort ble mottatt fra bruker. Dette tilfellet håndteres uansett av jobben som oppretter den
         *  automatiske behandlingen. Kan evt vurdere å også sjekke om en behandling eksisterer på kjeden her, men dette
         *  burde være et sjeldent tilfelle.
         * */
        if (brukersMeldekortRepo.hentForKjedeId(kjedeId, sakId).isNotEmpty()) {
            logger.info { "Finnes allerede et meldekort for kjede $kjedeId på sak $sakId - behandler ikke meldekortet automatisk ${kommando.id} (antatt korrigering)" }
            return MeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET.left()
        }

        val antallDagerMedRegistrering = kommando.antallDagerRegistrert
        val maksDagerMedRegistrering = meldeperiode.maksAntallDagerForMeldeperiode

        if (antallDagerMedRegistrering > maksDagerMedRegistrering) {
            logger.error { "Brukers meldekort ${kommando.id} har for mange dager registrert ($antallDagerMedRegistrering) - maks $maksDagerMedRegistrering" }
            return MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_REGISTRERT.left()
        }

        if (kommando.harRegistrertHelg() && !this.kanSendeInnHelgForMeldekort) {
            logger.error { "Brukers meldekort ${kommando.id} har registret helgedager, men saken tillater ikke helg på meldekort" }
            return MeldekortBehandletAutomatiskStatus.KAN_IKKE_MELDE_HELG.left()
        }

        val harForMangeDagerSammenhengendeGodkjentFravær = kommando.dager
            .windowed(MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER + 1)
            .any { forMangeDager -> forMangeDager.all { it.status == InnmeldtStatus.FRAVÆR_GODKJENT_AV_NAV } }

        if (harForMangeDagerSammenhengendeGodkjentFravær) {
            return MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_GODKJENT_FRAVÆR.left()
        }

        return Unit.right()
    }
}

sealed interface KanIkkeLagreBrukersMeldekort {
    data object AlleredeLagretUtenDiff : KanIkkeLagreBrukersMeldekort
    data object AlleredeLagretMedDiff : KanIkkeLagreBrukersMeldekort
    data object UkjentFeil : KanIkkeLagreBrukersMeldekort
}
