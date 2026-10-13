@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.infra.http

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelser
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.KunneIkkeHenteTiltakshistorikk
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.lokal.søknad.infra.repo.hentSøknaderForFnr
import no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.LokalTiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.infra.repo.LokalTiltaksdeltakelsePostgresRepo
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.toTiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse as LibsTiltaksdeltakelse

/**
 * Fake for lokal kjøring (LocalApplicationContext).
 * Deltakelsene utledes fra personens lagrede søknader, og en deltakelse satt via dev-endepunktet (se [no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.LokalTiltaksdeltakelseService]) overstyrer den med samme eksterne id.
 * Deltakelser opprettet via dev-endepunktet, også lønnstilskudd, kommer i tillegg.
 * Oppslaget for valg av tiltak i manuell søknad svarer med en fast deltakelse pluss de som er satt via dev-endepunktet.
 * Testene bruker [TiltaksdeltakelseFakeTestKlient].
 */
class TiltaksdeltakelseFakeLokalKlient(
    private val sessionFactory: PostgresSessionFactory,
) : TiltaksdeltakelseKlient {

    override suspend fun hentTiltaksdeltakelser(
        fnr: Fnr,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, Tiltaksdeltakelser> {
        return Tiltaksdeltakelser(hentLokaleTiltaksdeltakelser(fnr).map { it.tilLibsDeltakelse() }).right()
    }

    override suspend fun hentTiltaksdeltakelserMedArrangørnavn(
        fnr: Fnr,
        harAdressebeskyttelse: Boolean,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, List<TiltaksdeltakelseMedArrangørnavn>> {
        val satte = hentSatteTiltaksdeltakelser(fnr).map { it.tilTiltaksdeltakelseMedArrangørnavn() }
        return (listOf(ObjectMother.tiltaksdeltakelseMedArrangørnavn()) + satte)
            .distinctBy { it.eksternDeltakelseId }
            .right()
    }

    override suspend fun hentTiltaksdeltakelse(
        fnr: Fnr,
        eksternDeltakerId: String,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, LibsTiltaksdeltakelse?> {
        return hentLokaleTiltaksdeltakelser(fnr)
            .find { it.eksternDeltakelseId == eksternDeltakerId }
            ?.tilLibsDeltakelse()
            .right()
    }

    /**
     * Deltakelsene utledet fra søknadene, der en satt deltakelse erstatter den med samme eksterne id.
     * Satte deltakelser uten tilsvarende søknadstiltak (f.eks. lønnstilskudd opprettet via dev-endepunktet) legges til bakerst.
     */
    fun hentLokaleTiltaksdeltakelser(fnr: Fnr): List<LokalTiltaksdeltakelse> {
        val satte = hentSatteTiltaksdeltakelser(fnr).associateBy { it.eksternDeltakelseId }
        val fraSøknader = hentTiltaksdeltakelserFraSøknader(fnr)
        val fraSøknaderIder = fraSøknader.map { it.eksternDeltakelseId }.toSet()
        return fraSøknader.map { satte[it.eksternDeltakelseId] ?: LokalTiltaksdeltakelse.GirRett(it) } +
            satte.values.filterNot { it.eksternDeltakelseId in fraSøknaderIder }
    }

    /**
     * Session-telleren skrus av fordi klienten kalles med en åpen sesjon.
     */
    private fun hentSatteTiltaksdeltakelser(fnr: Fnr): List<LokalTiltaksdeltakelse> =
        sessionFactory.withSession(disableSessionCounter = true) { session ->
            LokalTiltaksdeltakelsePostgresRepo.hentForFnr(fnr, session)
        }

    private fun hentTiltaksdeltakelserFraSøknader(fnr: Fnr): List<TiltaksdeltakelseIntern> {
        // TODO: Denne utledningen av tiltaksdeltakelser fra søknaden er skjør og henger tett sammen med søknadsflyten.
        // Den fungerer bare når søknaden allerede er persistert.
        // For manuelt registrerte (papir) søknader beregnes saksopplysningene før søknaden lagres (se StartBehandlingAvManueltRegistrertSøknadService), så tiltaksdeltakelsen mangler på saksopplysning-tidspunktet og lister returneres tom.
        // Den forutsetter også at toTiltak()/tilLibsDeltakelse() bevarer internDeltakelseId slik at en påfølgende innvilgelse matcher.
        // Vurder å lagre deltakelsen i lokal.tiltaksdeltakelse når en søknad opprettes (både digital seed og papir-route) i stedet for å utlede fra lagrede søknader.
        return sessionFactory.hentSøknaderForFnr(fnr)
            .sortedByDescending { it.opprettet }
            .mapNotNull { it.tiltak?.toTiltak() }
            .distinctBy { it.eksternDeltakelseId }
    }
}
