package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.tiltakspenger.libs.common.Fnr

/**
 * Dekoratør som kjører [KontorhistorikkKlient] i parallell med eksisterende [VeilarboppfolgingKlient]
 * og logger eventuelle forskjeller.
 *
 * Begge klientene kalles alltid - også i prod - slik at vi kan:
 * 1. Lagre rå request/response fra begge tjenestene (via [NavkontorMedMetadata]/[KanIkkeHenteOppfølgingsenhet]).
 * 2. Falle tilbake på den nye klienten i de tilfellene den gamle ikke fant noe navkontor
 *    ([KanIkkeHenteOppfølgingsenhet.ManglerOppfolgingsenhet]).
 *
 * Valgregel for hvilket svar som returneres:
 * - Gammel klient OK -> bruk gammel.
 * - Gammel klient feilet med [KanIkkeHenteOppfølgingsenhet.ManglerOppfolgingsenhet] og ny klient ga
 *   et brukbart kontor ([Kontorhistorikk.nyesteAktuelleKontor]) -> bruk det fra ny.
 * - Ellers -> propagér feilen fra gammel klient.
 *
 * I alle tilfeller logges utfallet av sammenligningen og hvilken klient sitt svar som faktisk ble
 * benyttet, slik at vi har tydelig sporbarhet både i prod og i dev.
 *
 * [KontorhistorikkKlient] og [VeilarboppfolgingKlient]-implementasjonene gjør sin egen detaljerte
 * logging (inkludert sikkerlogg). Vi nøyer oss derfor her med å logge utfallet av sammenligningen
 * og valget.
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
                        logger.warn { "Sammenligning navkontor: ny klient kastet exception. $loggkontekst" }
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

            velgResultat(eksisterendeResultat, nyttResultat, nyttKall, loggkontekst)
        }
    }

    /**
     * Velger hvilket svar som returneres til konsumenten:
     * - Gammel OK -> gammel svaret (beriket med kontorhistorikkKall).
     * - Gammel [KanIkkeHenteOppfølgingsenhet.ManglerOppfolgingsenhet] + ny ga et brukbart kontor -> bruk ny.
     * - Ellers -> propagér Left fra gammel (beriket med kontorhistorikkKall).
     *
     * Hvilken klient som ble brukt logges eksplisitt.
     */
    private fun velgResultat(
        eksisterendeResultat: Either<KanIkkeHenteOppfølgingsenhet, NavkontorMedMetadata>,
        nyttResultat: Either<KanIkkeHenteKontorhistorikk, KontorhistorikkMedMetadata>,
        nyttKall: Klientkall?,
        loggkontekst: String,
    ): Either<KanIkkeHenteOppfølgingsenhet, NavkontorMedMetadata> {
        eksisterendeResultat.onRight {
            logger.info {
                "Navkontor: bruker svar fra GAMMEL klient (veilarboppfølging). " +
                    "kontornummer=${it.navkontor.kontornummer}. $loggkontekst"
            }
            return it.copy(kontorhistorikkKall = nyttKall).right()
        }

        val gammelFeil = eksisterendeResultat.leftOrNull()!!
        val nyttKontor = (gammelFeil as? KanIkkeHenteOppfølgingsenhet.ManglerOppfolgingsenhet)
            ?.let { nyttResultat.getOrNull()?.kontorhistorikk?.nyesteAktuelleKontor() }

        if (nyttKontor != null) {
            logger.info {
                "Navkontor: bruker svar fra NY klient (kontorhistorikk). " +
                    "Gammel klient fant ingen oppfølgingsenhet, ny klient ga " +
                    "kontorId=${nyttKontor.kontorId} (type=${nyttKontor.kontorType}). $loggkontekst"
            }
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

        logger.info {
            "Navkontor: bruker svar fra GAMMEL klient (feil propageres). " +
                "Feil=$gammelFeil. $loggkontekst"
        }
        return gammelFeil.medKontorhistorikkKall(nyttKall).left()
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
