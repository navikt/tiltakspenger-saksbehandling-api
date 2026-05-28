package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.tiltakspenger.libs.common.Fnr

/**
 * Dekoratør som kjører [KontorhistorikkKlient] i parallell med eksisterende [VeilarboppfolgingKlient]
 * og logger eventuelle forskjeller. Resultatet som returneres er alltid det fra den eksisterende klienten -
 * vi endrer ikke faktisk oppførsel i denne iterasjonen.
 *
 * Sammenligningen kjøres kun dersom [kjørSammenligning] er sann (typisk dev/local/test).
 *
 * [KontorhistorikkKlient] gjør sin egen detaljerte logging (inkludert sikkerlogg). Vi nøyer oss derfor
 * her med å logge utfallet av selve sammenligningen.
 */
class SammenligningVeilarboppfolgingKlient(
    private val eksisterende: VeilarboppfolgingKlient,
    private val kontorhistorikkKlient: KontorhistorikkKlient,
    private val kjørSammenligning: Boolean,
) : VeilarboppfolgingKlient {
    private val logger = KotlinLogging.logger {}

    override suspend fun hentOppfolgingsenhet(
        fnr: Fnr,
        sakId: String?,
        saksnummer: String?,
        rammebehandlingId: String?,
        meldekortbehandlingId: String?,
    ): Navkontor {
        val loggkontekst = lagLoggkontekst(sakId, saksnummer, rammebehandlingId, meldekortbehandlingId)
        if (!kjørSammenligning) {
            return eksisterende.hentOppfolgingsenhet(
                fnr = fnr,
                sakId = sakId,
                saksnummer = saksnummer,
                rammebehandlingId = rammebehandlingId,
                meldekortbehandlingId = meldekortbehandlingId,
            )
        }
        return coroutineScope {
            val eksisterendeDeferred = async {
                Either.catch {
                    eksisterende.hentOppfolgingsenhet(
                        fnr = fnr,
                        sakId = sakId,
                        saksnummer = saksnummer,
                        rammebehandlingId = rammebehandlingId,
                        meldekortbehandlingId = meldekortbehandlingId,
                    )
                }
            }
            val nyDeferred = async {
                Either.catch {
                    kontorhistorikkKlient.hentKontorhistorikk(
                        fnr = fnr,
                        sakId = sakId,
                        saksnummer = saksnummer,
                        rammebehandlingId = rammebehandlingId,
                        meldekortbehandlingId = meldekortbehandlingId,
                    )
                }.fold(
                    ifLeft = {
                        logger.warn { "Sammenligning navkontor: ny klient kastet exception. Gammel flyt fortsetter. $loggkontekst" }
                        KanIkkeHenteKontorhistorikk.KallFeilet.left()
                    },
                    ifRight = { it },
                )
            }

            val eksisterendeResultat = eksisterendeDeferred.await()
            val nyResultat = nyDeferred.await()

            loggSammenligning(eksisterendeResultat, nyResultat, loggkontekst)

            eksisterendeResultat.getOrElse { throw it }
        }
    }

    private fun loggSammenligning(
        eksisterendeResultat: Either<Throwable, Navkontor>,
        nyResultat: Either<KanIkkeHenteKontorhistorikk, Kontorhistorikk>,
        loggkontekst: String,
    ) {
        val eksisterende = eksisterendeResultat.getOrNull()
        val eksisterendeFeil = eksisterendeResultat.leftOrNull()
        val historikk = nyResultat.getOrNull()
        val nyFeil = nyResultat.leftOrNull()

        // Eksisterende tjeneste gir Arena med fallback til geografisk tilknytning. Vi tar med
        // ARBEIDSOPPFOLGING som førstevalg slik at sammenligningen fortsatt er meningsfull når nytt API
        // begynner å levere ARBEIDSOPPFOLGING-innslag i prod.
        val nyesteSammenlignbar = historikk?.nyesteAktuelleKontor()

        val sammeKontornummer = eksisterende != null &&
            nyesteSammenlignbar != null &&
            eksisterende.kontornummer == nyesteSammenlignbar.kontorId

        when {
            eksisterendeFeil != null && nyFeil != null ->
                logger.warn(eksisterendeFeil) {
                    "Sammenligning navkontor: begge klientene feilet. " +
                        "Eksisterende: ${eksisterendeFeil.message}, Ny: $nyFeil. $loggkontekst"
                }

            nyFeil != null ->
                logger.warn {
                    "Sammenligning navkontor: ny klient feilet ($nyFeil), eksisterende OK " +
                        "(kontornummer=${eksisterende?.kontornummer}). $loggkontekst"
                }

            eksisterendeFeil != null ->
                logger.warn(eksisterendeFeil) {
                    "Sammenligning navkontor: eksisterende klient feilet, ny klient OK " +
                        "(nyeste sammenlignbar kontorId=${nyesteSammenlignbar?.kontorId}). $loggkontekst"
                }

            sammeKontornummer ->
                logger.info { "Sammenligning navkontor: likt kontornummer ${eksisterende.kontornummer}. $loggkontekst" }

            else ->
                logger.warn {
                    "Sammenligning navkontor: ulikt resultat. " +
                        "Eksisterende kontornummer=${eksisterende?.kontornummer}, " +
                        "nyeste sammenlignbar kontorId=${nyesteSammenlignbar?.kontorId} (type=${nyesteSammenlignbar?.kontorType}), " +
                        "antall historikkinnslag=${historikk?.innslag?.size}. $loggkontekst"
                }
        }
    }
}

private fun lagLoggkontekst(
    sakId: String?,
    saksnummer: String?,
    rammebehandlingId: String?,
    meldekortbehandlingId: String?,
): String =
    "sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $rammebehandlingId, " +
        "meldekortbehandlingId: $meldekortbehandlingId"
