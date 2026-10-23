package no.nav.tiltakspenger.saksbehandling.søknad.domene

import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import java.time.LocalDateTime

/**
 * Siste kjente utfall av behandlingen av en digital søknad.
 * Sendes til tiltakspenger-soknad-api, som kjenner søknaden på samme [søknadId], slik at bruker kan se om søknaden er ferdig behandlet.
 * Søknad-api beholder utfallet med nyest [tidspunkt], så rekkefølgen utfallene sendes i spiller ingen rolle.
 *
 * @property vedtakId Vedtaket i søknadsbehandlingen.
 * Satt for [Søknadsutfallresultat.INNVILGELSE] og [Søknadsutfallresultat.AVSLAG], og null for avbrudd og gjenåpning, som ikke har vedtak.
 * @property tidspunkt Når vedtaket ble opprettet, eller når søknaden ble avbrutt eller gjenåpnet.
 */
data class Søknadsutfall(
    val søknadId: SøknadId,
    val resultat: Søknadsutfallresultat,
    val tidspunkt: LocalDateTime,
    val vedtakId: VedtakId?,
) {
    init {
        require((vedtakId != null) == resultat.harVedtak) {
            "Søknadsutfall $resultat for søknad $søknadId skal ${if (resultat.harVedtak) "" else "ikke "}ha vedtakId"
        }
    }
}

enum class Søknadsutfallresultat(val harVedtak: Boolean) {
    INNVILGELSE(harVedtak = true),
    AVSLAG(harVedtak = true),

    /** Søknaden ble avbrutt sammen med den siste aktive søknadsbehandlingen, uten vedtak. */
    AVBRUTT(harVedtak = false),

    /**
     * En avbrutt søknad er gjenåpnet og under behandling igjen.
     * Opphever et tidligere [AVBRUTT].
     */
    GJENÅPNET(harVedtak = false),
}

fun SøknadsbehandlingsresultatType.tilSøknadsutfallresultat(): Søknadsutfallresultat = when (this) {
    SøknadsbehandlingsresultatType.INNVILGELSE -> Søknadsutfallresultat.INNVILGELSE
    SøknadsbehandlingsresultatType.AVSLAG -> Søknadsutfallresultat.AVSLAG
}

/**
 * Siste avbrudd eller gjenåpning av en søknad, som ikke er sendt til søknad-api ennå.
 *
 * @property antallHendelser Antall hendelser søknaden hadde da utfallet ble hentet.
 * Markeres som sendt, slik at en hendelse som kommer til underveis fortsatt ligger i køen.
 */
data class SøknadshendelseTilSøknadApi(
    val søknadsutfall: Søknadsutfall,
    val antallHendelser: Int,
)

fun Søknadshendelse.tilSøknadsutfall(søknadId: SøknadId): Søknadsutfall = Søknadsutfall(
    søknadId = søknadId,
    resultat = when (this) {
        is Søknadshendelse.Avbrutt -> Søknadsutfallresultat.AVBRUTT
        is Søknadshendelse.Gjenåpnet -> Søknadsutfallresultat.GJENÅPNET
    },
    tidspunkt = tidspunkt,
    vedtakId = null,
)
