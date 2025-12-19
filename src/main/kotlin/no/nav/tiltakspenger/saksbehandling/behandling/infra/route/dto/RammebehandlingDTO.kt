package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.infra.route.AttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AvbruttDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.VentestatusHendelseDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.tilVentestatusHendelseDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAvbruttDTO
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.SøknadDTO
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.toSøknadDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import java.time.LocalDateTime

sealed interface RammebehandlingDTO : RammebehandlingResultatDTO {
    val id: String
    val type: RammebehandlingstypeDTO
    val status: RammebehandlingsstatusDTO
    val sakId: String
    val saksnummer: String
    val rammevedtakId: String?
    val saksbehandler: String?
    val beslutter: String?
    val saksopplysninger: SaksopplysningerDTO
    val attesteringer: List<AttesteringDTO>
    val vedtaksperiode: PeriodeDTO?
    val fritekstTilVedtaksbrev: String?
    val begrunnelseVilkårsvurdering: String?
    val avbrutt: AvbruttDTO?
    val sistEndret: LocalDateTime
    val iverksattTidspunkt: LocalDateTime?
    val ventestatus: VentestatusHendelseDTO?
    val utbetaling: BehandlingUtbetalingDTO?
}

data class SøknadsbehandlingDTO(
    override val id: String,
    override val status: RammebehandlingsstatusDTO,
    override val sakId: String,
    override val saksnummer: String,
    override val rammevedtakId: String?,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val saksopplysninger: SaksopplysningerDTO,
    override val attesteringer: List<AttesteringDTO>,
    override val vedtaksperiode: PeriodeDTO?,
    override val fritekstTilVedtaksbrev: String?,
    override val begrunnelseVilkårsvurdering: String?,
    override val avbrutt: AvbruttDTO?,
    override val sistEndret: LocalDateTime,
    override val iverksattTidspunkt: LocalDateTime?,
    override val ventestatus: VentestatusHendelseDTO?,
    override val utbetaling: BehandlingUtbetalingDTO?,
    @param:JsonUnwrapped val resultatDTO: SøknadsbehandlingResultatDTO,
    val søknad: SøknadDTO,
    val automatiskSaksbehandlet: Boolean,
    val manueltBehandlesGrunner: List<String>,
    val kanInnvilges: Boolean,
) : RammebehandlingDTO,
    SøknadsbehandlingResultatDTO by resultatDTO {
    override val type = RammebehandlingstypeDTO.SØKNADSBEHANDLING
}

data class RevurderingDTO(
    override val id: String,
    override val status: RammebehandlingsstatusDTO,
    override val sakId: String,
    override val saksnummer: String,
    override val rammevedtakId: String?,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val saksopplysninger: SaksopplysningerDTO,
    override val attesteringer: List<AttesteringDTO>,
    // TODO: Rename til vedtaksperiode i frontend+backend
    override val vedtaksperiode: PeriodeDTO?,
    override val fritekstTilVedtaksbrev: String?,
    override val begrunnelseVilkårsvurdering: String?,
    override val avbrutt: AvbruttDTO?,
    override val sistEndret: LocalDateTime,
    override val iverksattTidspunkt: LocalDateTime?,
    override val ventestatus: VentestatusHendelseDTO?,
    override val utbetaling: BehandlingUtbetalingDTO?,
    @param:JsonUnwrapped val resultatDTO: RevurderingResultatDTO,
) : RammebehandlingDTO,
    RevurderingResultatDTO by resultatDTO {
    override val type = RammebehandlingstypeDTO.REVURDERING
}

fun Sak.tilBehandlingDTO(behandlingId: BehandlingId): RammebehandlingDTO {
    val behandling = rammebehandlinger.hentBehandling(behandlingId)

    requireNotNull(behandling) {
        "Fant ingen behandling med id $behandlingId"
    }

    val rammevedtakId = rammevedtaksliste.finnRammevedtakForBehandling(behandlingId)?.id

    return when (behandling) {
        is Revurdering -> behandling.tilRevurderingDTO(
            utbetalingsstatus = utbetalinger.hentUtbetalingForBehandlingId(behandlingId)?.status,
            beregninger = meldeperiodeBeregninger,
            rammevedtakId = rammevedtakId,
        )

        is Søknadsbehandling -> behandling.tilSøknadsbehandlingDTO(
            utbetalingsstatus = utbetalinger.hentUtbetalingForBehandlingId(behandlingId)?.status,
            beregninger = meldeperiodeBeregninger,
            rammevedtakId = rammevedtakId,
        )
    }
}

fun Sak.tilBehandlingerDTO(): List<RammebehandlingDTO> {
    return this.rammebehandlinger.map { this.tilBehandlingDTO(it.id) }
}

fun Søknadsbehandling.tilSøknadsbehandlingDTO(
    utbetalingsstatus: Utbetalingsstatus?,
    beregninger: MeldeperiodeBeregningerVedtatt,
    rammevedtakId: VedtakId?,
): SøknadsbehandlingDTO {
    return SøknadsbehandlingDTO(
        id = this.id.toString(),
        status = this.status.toBehandlingsstatusDTO(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        rammevedtakId = rammevedtakId.toString(),
        saksbehandler = this.saksbehandler,
        beslutter = this.beslutter,
        attesteringer = this.attesteringer.toAttesteringDTO(),
        saksopplysninger = this.saksopplysninger.toSaksopplysningerDTO(),
        søknad = this.søknad.toSøknadDTO(),
        avbrutt = this.avbrutt?.toAvbruttDTO(),
        sistEndret = this.sistEndret,
        iverksattTidspunkt = this.iverksattTidspunkt,
        fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev?.verdi,
        begrunnelseVilkårsvurdering = this.begrunnelseVilkårsvurdering?.verdi,
        vedtaksperiode = this.vedtaksperiode?.toDTO(),
        automatiskSaksbehandlet = this.automatiskSaksbehandlet,
        manueltBehandlesGrunner = this.manueltBehandlesGrunner.map { it.name },
        ventestatus = ventestatus.ventestatusHendelser.lastOrNull()?.tilVentestatusHendelseDTO(),
        utbetaling = utbetaling?.tilDTO(utbetalingsstatus, beregninger),
        resultatDTO = this.resultat.tilSøknadsbehandlingResultatDTO(),
        kanInnvilges = this.kanInnvilges,
    )
}

fun Revurdering.tilRevurderingDTO(
    utbetalingsstatus: Utbetalingsstatus?,
    beregninger: MeldeperiodeBeregningerVedtatt,
    rammevedtakId: VedtakId?,
): RevurderingDTO {
    return RevurderingDTO(
        id = this.id.toString(),
        status = this.status.toBehandlingsstatusDTO(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        rammevedtakId = rammevedtakId.toString(),
        saksbehandler = this.saksbehandler,
        beslutter = this.beslutter,
        attesteringer = this.attesteringer.toAttesteringDTO(),
        saksopplysninger = this.saksopplysninger.toSaksopplysningerDTO(),
        vedtaksperiode = this.vedtaksperiode?.toDTO(),
        fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev?.verdi,
        begrunnelseVilkårsvurdering = this.begrunnelseVilkårsvurdering?.verdi,
        avbrutt = this.avbrutt?.toAvbruttDTO(),
        sistEndret = this.sistEndret,
        iverksattTidspunkt = this.iverksattTidspunkt,
        ventestatus = ventestatus.ventestatusHendelser.lastOrNull()?.tilVentestatusHendelseDTO(),
        utbetaling = utbetaling?.tilDTO(utbetalingsstatus, beregninger),
        resultatDTO = this.resultat.tilRevurderingResultatDTO(),
    )
}
