package no.nav.tiltakspenger.saksbehandling.søknad.domene

import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import java.time.LocalDateTime

/**
 * Utfallet av en iverksatt søknadsbehandling for en digital søknad.
 * Sendes til tiltakspenger-soknad-api, som kjenner søknaden på samme [søknadId], slik at bruker kan se at søknaden er ferdig behandlet.
 *
 * @property vedtakId Vedtaket i søknadsbehandlingen.
 * @property vedtakstidspunkt Tidspunktet vedtaket ble opprettet, altså da søknadsbehandlingen ble iverksatt.
 */
data class Søknadsutfall(
    val vedtakId: VedtakId,
    val søknadId: SøknadId,
    val resultat: SøknadsbehandlingsresultatType,
    val vedtakstidspunkt: LocalDateTime,
)
