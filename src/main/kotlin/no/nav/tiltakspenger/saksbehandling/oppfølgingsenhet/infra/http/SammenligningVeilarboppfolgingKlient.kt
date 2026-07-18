package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.throwableOrNull
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.BruktNavkontorKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteKontorhistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteNavkontor
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Klientkall
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorhistorikkMedMetadata
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorMedMetadata
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.beskrivelse
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.medKontorhistorikkKall
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.throwableForLogg

/**
 * Eneste implementasjon av [NavkontorKlient], og eneste konsument av de to underliggende klientene.
 * Kjører [KontorhistorikkHttpklient] (ny) i parallell med [VeilarboppfolgingHttpClient] (gammel).
 * Klassen er midlertidig: når gammel klient slettes forsvinner den, og loggingen flyttes til [NavkontorService].
 *
 * Begge klientene kalles alltid - også i prod - slik at vi kan:
 * 1. Lagre rå request/response fra begge tjenestene (via [NavkontorMedMetadata]/[KanIkkeHenteNavkontor]).
 * 2. Falle tilbake på den nye klienten i de tilfellene den gamle ikke fant noe navkontor
 *    ([KanIkkeHenteNavkontor.ManglerOppfolgingsenhet]).
 *
 * Valgregel for hvilket svar som returneres:
 * - Gammel klient OK -> bruk gammel.
 * - Gammel klient feilet med [KanIkkeHenteNavkontor.ManglerOppfolgingsenhet] og ny klient ga et brukbart kontor ([Kontorhistorikk.nyesteAktuelleKontor]) -> bruk det fra ny.
 * - Ellers -> propagér feilen fra gammel klient.
 *
 * All logging for navkontor-oppslaget skjer her - klientene og [NavkontorService] logger ikke selv:
 * - Én error per klient som feiler (throwable og rå request/response kun til sikkerlogg).
 * - Feiler ingen av klientene sammenlignes svarene: error dersom ny klient ikke har et brukbart kontor eller svarene er ulike, info når vi faller tilbake på ny klient, og ingen logging ved likt svar.
 */
class SammenligningVeilarboppfolgingKlient(
    private val eksisterende: VeilarboppfolgingHttpClient,
    private val kontorhistorikkKlient: KontorhistorikkHttpklient,
) : NavkontorKlient {
    private val logger = KotlinLogging.logger {}

    override suspend fun hentNavkontor(
        fnr: Fnr,
        loggkontekst: String,
    ): Either<KanIkkeHenteNavkontor, NavkontorMedMetadata> {
        return coroutineScope {
            val eksisterendeDeferred = async {
                eksisterende.hentOppfolgingsenhet(
                    fnr = fnr,
                )
            }
            val nyDeferred = async {
                Either.catch {
                    kontorhistorikkKlient.hentKontorhistorikk(
                        fnr = fnr,
                    )
                }.getOrElse {
                    // Skal ikke skje - kontrakten er å returnere Either.
                    // Either.catch slipper igjennom CancellationException, som er ønskelig.
                    // All feillogging skjer i [loggUtfall]; stacktracen følger med dit via [KanIkkeHenteKontorhistorikk.throwableForLogg].
                    KanIkkeHenteKontorhistorikk.UventetFeil(it).left()
                }
            }

            val eksisterendeResultat = eksisterendeDeferred.await()
            val nyttResultat = nyDeferred.await()

            // Hent ut Klientkall fra både gammel og ny - uavhengig av om svaret var Left eller Right - slik at konsumenten kan lagre rådata fra begge tjenestene.
            val nyttKall: Klientkall? = nyttResultat.fold(
                ifLeft = { it.kall },
                ifRight = { it.kall },
            )

            loggUtfall(eksisterendeResultat, nyttResultat, loggkontekst)

            velgResultat(eksisterendeResultat, nyttResultat, nyttKall)
        }
    }

    /**
     * Velger hvilket svar som returneres til konsumenten:
     * - Gammel OK -> gammel svaret (beriket med kontorhistorikkKall).
     * - Gammel [KanIkkeHenteNavkontor.ManglerOppfolgingsenhet] + ny ga et brukbart kontor -> bruk ny.
     * - Ellers -> propagér Left fra gammel (beriket med kontorhistorikkKall).
     *
     * Logger ikke - det gjør [loggUtfall].
     */
    private fun velgResultat(
        eksisterendeResultat: Either<KanIkkeHenteNavkontor, NavkontorMedMetadata>,
        nyttResultat: Either<KanIkkeHenteKontorhistorikk, KontorhistorikkMedMetadata>,
        nyttKall: Klientkall?,
    ): Either<KanIkkeHenteNavkontor, NavkontorMedMetadata> {
        eksisterendeResultat.onRight {
            return it.copy(kontorhistorikkKall = nyttKall).right()
        }

        val gammelFeil = eksisterendeResultat.leftOrNull()!!
        val nyttKontor = (gammelFeil as? KanIkkeHenteNavkontor.ManglerOppfolgingsenhet)
            ?.let { nyttResultat.getOrNull()?.kontorhistorikk?.nyesteAktuelleKontor() }

        if (nyttKontor != null) {
            return NavkontorMedMetadata(
                navkontor = Navkontor(
                    kontornummer = nyttKontor.kontorId,
                    kontornavn = nyttKontor.kontorNavn,
                ),
                brukteKlient = BruktNavkontorKlient.KONTORHISTORIKK,
                veilarboppfolgingKall = gammelFeil.veilarboppfolgingKall,
                kontorhistorikkKall = nyttKall,
            ).right()
        }

        return gammelFeil.medKontorhistorikkKall(nyttKall).left()
    }

    /**
     * All logging for navkontor-oppslaget skjer her - én gang per klient som feiler.
     *
     * [KanIkkeHenteNavkontor.ManglerOppfolgingsenhet] regnes ikke som klientfeil: det er et forventet nullsvar (bruker uten aktivt oppfølgingsnavkontor i gammel tjeneste) og nettopp tilfellet der vi faller tilbake på ny klient.
     *
     * Feiler ingen av klientene sammenlignes svarene: error dersom ny klient ikke har et brukbart kontor eller svarene er ulike, info når vi faller tilbake på ny klient, og ingen logging ved likt svar.
     * Navkontor/kontorId er stedslokaliserende persondata og logges kun til sikkerlogg.
     */
    private fun loggUtfall(
        eksisterendeResultat: Either<KanIkkeHenteNavkontor, NavkontorMedMetadata>,
        nyttResultat: Either<KanIkkeHenteKontorhistorikk, KontorhistorikkMedMetadata>,
        loggkontekst: String,
    ) {
        val gammelFeil = eksisterendeResultat.leftOrNull()
            ?.takeUnless { it is KanIkkeHenteNavkontor.ManglerOppfolgingsenhet }
        if (gammelFeil != null) {
            logger.error {
                "Navkontor: kall mot veilarboppfolging (gammel klient) feilet: ${gammelFeil.beskrivelse()}. " +
                    "Se sikkerlogg for detaljer. $loggkontekst"
            }
            Sikkerlogg.error(gammelFeil.httpKlientError?.throwableOrNull()) {
                "Navkontor: kall mot veilarboppfolging (gammel klient) feilet: ${gammelFeil.beskrivelse()}. " +
                    "request=${gammelFeil.veilarboppfolgingKall?.request}, response=${gammelFeil.veilarboppfolgingKall?.response}, " +
                    "httpStatus=${gammelFeil.veilarboppfolgingKall?.httpStatus}. $loggkontekst"
            }
        }

        val nyFeil = nyttResultat.leftOrNull()
        if (nyFeil != null) {
            logger.error {
                "Navkontor: kall mot kontorhistorikk (ny klient) feilet: ${nyFeil.beskrivelse()}. " +
                    "Se sikkerlogg for detaljer. $loggkontekst"
            }
            Sikkerlogg.error(nyFeil.throwableForLogg()) {
                "Navkontor: kall mot kontorhistorikk (ny klient) feilet: ${nyFeil.beskrivelse()}. " +
                    "request=${nyFeil.kall?.request}, response=${nyFeil.kall?.response}, " +
                    "httpStatus=${nyFeil.kall?.httpStatus}. $loggkontekst"
            }
        }

        if (gammelFeil != null || nyFeil != null) return

        // Ingen klientfeil: sammenlign svarene. `null` gammelt kontor betyr ManglerOppfolgingsenhet.
        val gammeltKontor = eksisterendeResultat.getOrNull()?.navkontor

        // Eksisterende tjeneste gir Arena med fallback til geografisk tilknytning.
        // Vi tar med ARBEIDSOPPFOLGING som førstevalg slik at sammenligningen fortsatt er meningsfull når nytt API begynner å levere ARBEIDSOPPFOLGING-innslag i prod.
        val nyttKontor = nyttResultat.getOrNull()!!.kontorhistorikk.nyesteAktuelleKontor()

        when {
            nyttKontor == null -> {
                logger.error {
                    "Navkontor: ny klient (kontorhistorikk) har ikke et brukbart kontor. " +
                        "Se sikkerlogg for gammelt svar. $loggkontekst"
                }
                Sikkerlogg.error {
                    "Navkontor: ny klient (kontorhistorikk) har ikke et brukbart kontor. " +
                        "Gammelt svar: ${gammeltKontor?.toStringForSikkerlogg() ?: "fant ingen oppfølgingsenhet"}. $loggkontekst"
                }
            }

            gammeltKontor == null -> {
                logger.info {
                    "Navkontor: gammel klient fant ingen oppfølgingsenhet - bruker svar fra ny klient (kontorhistorikk). " +
                        "Se sikkerlogg for kontorId. $loggkontekst"
                }
                Sikkerlogg.info {
                    "Navkontor: gammel klient fant ingen oppfølgingsenhet - bruker " +
                        "kontorId=${nyttKontor.kontorId} (type=${nyttKontor.kontorType}) fra ny klient. $loggkontekst"
                }
            }

            gammeltKontor.kontornummer != nyttKontor.kontorId -> {
                logger.error { "Navkontor: ulikt svar fra gammel og ny klient. Se sikkerlogg for detaljer. $loggkontekst" }
                Sikkerlogg.error {
                    "Navkontor: ulikt svar fra gammel og ny klient. " +
                        "Gammel=${gammeltKontor.toStringForSikkerlogg()}, " +
                        "ny kontorId=${nyttKontor.kontorId} (type=${nyttKontor.kontorType}). $loggkontekst"
                }
            }

            // Likt svar: ingen logging.
        }
    }
}
