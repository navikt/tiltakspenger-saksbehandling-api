package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.tiltakspenger.libs.common.Fnr

/**
 * Dekoratør som kjører [KontorhistorikkKlient] i parallell med eksisterende [VeilarboppfolgingKlient]
 * og logger eventuelle forskjeller. Resultatet som returneres er fortsatt det fra den eksisterende
 * klienten - vi endrer ikke faktisk oppførsel i denne iterasjonen.
 *
 * Begge klientene kalles alltid - også i prod - slik at vi kan:
 * 1. Lagre rå request/response fra begge tjenestene (via [NavkontorMedMetadata]/[KanIkkeHenteOppfølgingsenhet]).
 * 2. Senere falle tilbake på den nye klienten dersom den gamle ikke gir svar.
 *
 * Utfallet av sammenligningen logges alltid - både ved suksess og feil - slik at vi får
 * sporbarhet på om gammel og ny tjeneste gir samme svar.
 *
 * [KontorhistorikkKlient] gjør sin egen detaljerte logging (inkludert sikkerlogg). Vi nøyer oss derfor
 * her med å logge utfallet av selve sammenligningen.
 */
class SammenligningVeilarboppfolgingKlient(
    private val eksisterende: VeilarboppfolgingKlient,
    private val kontorhistorikkKlient: KontorhistorikkKlient,
) : VeilarboppfolgingKlient {
    private val logger = KotlinLogging.logger {}

    override suspend fun hentOppfolgingsenhet(
        fnr: Fnr,
        sakId: String?,
        saksnummer: String?,
        rammebehandlingId: String?,
        meldekortbehandlingId: String?,
    ): Either<KanIkkeHenteOppfølgingsenhet, NavkontorMedMetadata> {
        val loggkontekst = lagLoggkontekst(sakId, saksnummer, rammebehandlingId, meldekortbehandlingId)
        return coroutineScope {
            val eksisterendeDeferred = async {
                eksisterende.hentOppfolgingsenhet(
                    fnr = fnr,
                    sakId = sakId,
                    saksnummer = saksnummer,
                    rammebehandlingId = rammebehandlingId,
                    meldekortbehandlingId = meldekortbehandlingId,
                )
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
                        KanIkkeHenteKontorhistorikk.KallFeilet().left()
                    },
                    ifRight = { it },
                )
            }

            val eksisterendeResultat = eksisterendeDeferred.await()
            val nyttResultat = nyDeferred.await()

            // Hent ut Klientkall fra både gammel og ny - uavhengig av om svaret var Left eller Right -
            // slik at konsumenten kan lagre rådata fra begge tjenestene.
            val nyttKall: Klientkall? = nyttResultat.fold(
                ifLeft = { it.kall },
                ifRight = { it.kall },
            )

            loggSammenligning(eksisterendeResultat, nyttResultat, loggkontekst)

            // Forberedelse: hvis den gamle klienten ikke returnerer noe vil vi senere ønske å falle tilbake
            // på data fra den nye. I dag returnerer vi fortsatt det eksisterende svaret, men beriker det
            // (og evt. feil) med metadata fra begge kall slik at konsumenten har alt som trengs for å
            // lagre rådata og hvilken klient som ble brukt.
            eksisterendeResultat
                .map { it.copy(kontorhistorikkKall = nyttKall) }
                .mapLeft { it.medKontorhistorikkKall(nyttKall) }
        }
    }

    private fun loggSammenligning(
        eksisterendeResultat: Either<KanIkkeHenteOppfølgingsenhet, NavkontorMedMetadata>,
        nyttResultat: Either<KanIkkeHenteKontorhistorikk, KontorhistorikkMedMetadata>,
        loggkontekst: String,
    ) {
        val eksisterende = eksisterendeResultat.getOrNull()?.navkontor
        val eksisterendeFeil = eksisterendeResultat.leftOrNull()
        val historikk = nyttResultat.getOrNull()?.kontorhistorikk
        val nyFeil = nyttResultat.leftOrNull()

        // Eksisterende tjeneste gir Arena med fallback til geografisk tilknytning. Vi tar med
        // ARBEIDSOPPFOLGING som førstevalg slik at sammenligningen fortsatt er meningsfull når nytt API
        // begynner å levere ARBEIDSOPPFOLGING-innslag i prod.
        val nyesteSammenlignbar = historikk?.nyesteAktuelleKontor()

        val sammeKontornummer = eksisterende != null &&
            nyesteSammenlignbar != null &&
            eksisterende.kontornummer == nyesteSammenlignbar.kontorId

        when {
            eksisterendeFeil != null && nyFeil != null ->
                logger.warn {
                    "Sammenligning navkontor: begge klientene feilet. " +
                        "Eksisterende: $eksisterendeFeil, Ny: $nyFeil. $loggkontekst"
                }

            nyFeil != null ->
                logger.warn {
                    "Sammenligning navkontor: ny klient feilet ($nyFeil), eksisterende OK " +
                        "(kontornummer=${eksisterende?.kontornummer}). $loggkontekst"
                }

            eksisterendeFeil != null ->
                logger.warn {
                    "Sammenligning navkontor: eksisterende klient feilet ($eksisterendeFeil), ny klient OK " +
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
