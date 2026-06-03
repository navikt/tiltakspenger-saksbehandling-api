package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.logging.Sikkerlogg

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
            // Navkontor er stedslokaliserende persondata og skal kun til sikkerlogg, ikke vanlig logg.
            logger.info {
                "Navkontor: bruker svar fra GAMMEL klient (veilarboppfølging). Se sikkerlogg for kontornummer. $loggkontekst"
            }
            Sikkerlogg.info {
                "Navkontor: bruker svar fra GAMMEL klient (veilarboppfølging). " +
                    "navkontor=${it.navkontor.toStringForSikkerlogg()}. $loggkontekst"
            }
            return it.copy(kontorhistorikkKall = nyttKall).right()
        }

        val gammelFeil = eksisterendeResultat.leftOrNull()!!
        val nyttKontor = (gammelFeil as? KanIkkeHenteOppfølgingsenhet.ManglerOppfolgingsenhet)
            ?.let { nyttResultat.getOrNull()?.kontorhistorikk?.nyesteAktuelleKontor() }

        if (nyttKontor != null) {
            logger.info {
                "Navkontor: bruker svar fra NY klient (kontorhistorikk). " +
                    "Gammel klient fant ingen oppfølgingsenhet. Se sikkerlogg for kontorId. $loggkontekst"
            }
            Sikkerlogg.info {
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
                "Feil=${gammelFeil.beskrivelse()}. $loggkontekst"
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
                        "Eksisterende: ${eksisterendeFeil.beskrivelse()}, Ny: ${nyFeil.beskrivelse()}. $loggkontekst"
                }

            nyFeil != null -> {
                // kontornummer er stedslokaliserende og logges kun til sikkerlogg.
                logger.warn {
                    "Sammenligning navkontor: ny klient feilet (${nyFeil.beskrivelse()}), eksisterende OK. " +
                        "Se sikkerlogg for kontornummer. $loggkontekst"
                }
                Sikkerlogg.warn {
                    "Sammenligning navkontor: ny klient feilet (${nyFeil.beskrivelse()}), eksisterende OK " +
                        "(navkontor=${eksisterende?.toStringForSikkerlogg()}). $loggkontekst"
                }
            }

            eksisterendeFeil != null -> {
                // ManglerOppfolgingsenhet er forventet (brukeren har ikke aktivt oppfølgingsnavkontor i gammel
                // tjeneste) og er nettopp tilfellet der vi faller tilbake på ny klient. Da holder det med info.
                val erForventetFallback = eksisterendeFeil is KanIkkeHenteOppfølgingsenhet.ManglerOppfolgingsenhet
                if (erForventetFallback) {
                    logger.info {
                        "Sammenligning navkontor: eksisterende klient fant ingen oppfølgingsenhet, ny klient OK. " +
                            "Se sikkerlogg for kontorId. $loggkontekst"
                    }
                    Sikkerlogg.info {
                        "Sammenligning navkontor: eksisterende klient fant ingen oppfølgingsenhet, ny klient OK " +
                            "(nyeste sammenlignbar kontorId=${nyesteSammenlignbar?.kontorId}). $loggkontekst"
                    }
                } else {
                    logger.warn {
                        "Sammenligning navkontor: eksisterende klient feilet (${eksisterendeFeil.beskrivelse()}), ny klient OK. " +
                            "Se sikkerlogg for kontorId. $loggkontekst"
                    }
                    Sikkerlogg.warn {
                        "Sammenligning navkontor: eksisterende klient feilet (${eksisterendeFeil.beskrivelse()}), ny klient OK " +
                            "(nyeste sammenlignbar kontorId=${nyesteSammenlignbar?.kontorId}). $loggkontekst"
                    }
                }
            }

            sammeKontornummer -> {
                logger.info { "Sammenligning navkontor: likt kontornummer. Se sikkerlogg for kontornummer. $loggkontekst" }
                Sikkerlogg.info { "Sammenligning navkontor: likt navkontor ${eksisterende.toStringForSikkerlogg()}. $loggkontekst" }
            }

            else -> {
                logger.warn {
                    "Sammenligning navkontor: ulikt resultat. " +
                        "Se sikkerlogg for kontornummer/kontorId. " +
                        "antall historikkinnslag=${historikk?.innslag?.size}. $loggkontekst"
                }
                Sikkerlogg.warn {
                    "Sammenligning navkontor: ulikt resultat. " +
                        "Eksisterende navkontor=${eksisterende?.toStringForSikkerlogg()}, " +
                        "nyeste sammenlignbar kontorId=${nyesteSammenlignbar?.kontorId} (type=${nyesteSammenlignbar?.kontorType}), " +
                        "antall historikkinnslag=${historikk?.innslag?.size}. $loggkontekst"
                }
            }
        }
    }
}

/**
 * Nøytral, ikke-sensitiv beskrivelse av en feil for bruk i vanlig logg.
 *
 * Feiltypene bærer med seg [Klientkall] med rå request/response, og en default `toString()` ville
 * derfor lekke stedslokaliserende persondata (f.eks. navkontor) til vanlig logg. Vi logger derfor
 * kun feiltypen (og ev. HTTP-status, som ikke er sensitivt) her - rådata hører hjemme i sikkerlogg.
 */
private fun KanIkkeHenteKontorhistorikk.beskrivelse(): String = when (this) {
    is KanIkkeHenteKontorhistorikk.KallFeilet -> "KallFeilet"
    is KanIkkeHenteKontorhistorikk.UventetHttpStatus -> "UventetHttpStatus(status=$status)"
    is KanIkkeHenteKontorhistorikk.GraphQlFeil -> "GraphQlFeil"
}

private fun KanIkkeHenteOppfølgingsenhet.beskrivelse(): String = when (this) {
    is KanIkkeHenteOppfølgingsenhet.KallFeilet -> "KallFeilet"
    is KanIkkeHenteOppfølgingsenhet.UventetHttpStatus -> "UventetHttpStatus(status=$status)"
    is KanIkkeHenteOppfølgingsenhet.ManglerOppfolgingsenhet -> "ManglerOppfolgingsenhet"
}

private fun lagLoggkontekst(
    sakId: String?,
    saksnummer: String?,
    rammebehandlingId: String?,
    meldekortbehandlingId: String?,
): String =
    "sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $rammebehandlingId, " +
        "meldekortbehandlingId: $meldekortbehandlingId"
