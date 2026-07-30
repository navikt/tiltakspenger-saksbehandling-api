package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRevurderingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilSøknadsbehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toBehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlageresultatstypeDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlageresultatstypeDto.Companion.toKlageresultatstypeDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagestatustypeDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagestatustypeDto.Companion.toKlagestatustypeDto
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.ÅpenBehandlingDTO.SøknadUtenBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.ÅpenBehandlingDTO.ÅpenMeldekortbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.ÅpenBehandlingDTO.ÅpenRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.ÅpenBehandlingDTO.ÅpenRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.ÅpenBehandlingDTO.ÅpenSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.ÅpenBehandlingDTO.ÅpenTilbakekrevingDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route.dto.TilbakekrevingBehandlingDTO.TilbakekrevingBehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route.dto.tilTilbakekrevingBehandlingsstatusDTO
import java.math.BigDecimal
import java.time.LocalDateTime

sealed interface ÅpenBehandlingDTO {
    val id: String
    val opprettet: LocalDateTime
    val type: ÅpenBehandlingTypeDTO

    enum class ÅpenBehandlingTypeDTO {
        SØKNAD,
        SØKNADSBEHANDLING,
        REVURDERING,
        MELDEKORT,
        KLAGE,
        TILBAKEKREVING,
    }

    sealed interface ÅpenRammebehandlingDTO : ÅpenBehandlingDTO {
        val periode: PeriodeDTO?
        val status: RammebehandlingsstatusDTO
        val underkjent: Boolean
        val erSattPåVent: Boolean
        val resultat: RammebehandlingResultatTypeDTO?
        val saksbehandler: String?
        val beslutter: String?
    }

    data class SøknadUtenBehandlingDTO(
        override val id: String,
        override val opprettet: LocalDateTime,
        val kravtidspunkt: LocalDateTime,
    ) : ÅpenBehandlingDTO {
        override val type = ÅpenBehandlingTypeDTO.SØKNAD
    }

    data class ÅpenSøknadsbehandlingDTO(
        override val id: String,
        override val opprettet: LocalDateTime,
        override val periode: PeriodeDTO?,
        override val status: RammebehandlingsstatusDTO,
        override val underkjent: Boolean,
        override val erSattPåVent: Boolean,
        override val resultat: RammebehandlingResultatTypeDTO,
        override val saksbehandler: String?,
        override val beslutter: String?,
        val kravtidspunkt: LocalDateTime?,
    ) : ÅpenRammebehandlingDTO {
        override val type = ÅpenBehandlingTypeDTO.SØKNADSBEHANDLING
    }

    data class ÅpenRevurderingDTO(
        override val id: String,
        override val opprettet: LocalDateTime,
        override val periode: PeriodeDTO?,
        override val status: RammebehandlingsstatusDTO,
        override val underkjent: Boolean,
        override val erSattPåVent: Boolean,
        override val resultat: RammebehandlingResultatTypeDTO,
        override val saksbehandler: String?,
        override val beslutter: String?,
    ) : ÅpenRammebehandlingDTO {
        override val type = ÅpenBehandlingTypeDTO.REVURDERING
    }

    data class ÅpenMeldekortbehandlingDTO(
        override val id: String,
        override val opprettet: LocalDateTime,
        val periode: PeriodeDTO,
        val saksbehandler: String?,
        val beslutter: String?,
    ) : ÅpenBehandlingDTO {
        override val type = ÅpenBehandlingTypeDTO.MELDEKORT
    }

    data class ÅpenKlagebehandlingDTO(
        override val id: String,
        override val opprettet: LocalDateTime,
        val status: KlagestatustypeDto,
        val saksbehandler: String?,
        val resultat: KlageresultatstypeDto?,
    ) : ÅpenBehandlingDTO {
        override val type = ÅpenBehandlingTypeDTO.KLAGE
    }

    data class ÅpenTilbakekrevingDTO(
        override val id: String,
        override val opprettet: LocalDateTime,
        val periode: PeriodeDTO,
        val status: TilbakekrevingBehandlingsstatusDTO,
        val totaltFeilutbetaltBeløp: BigDecimal,
        val saksbehandler: String?,
        val beslutter: String?,
    ) : ÅpenBehandlingDTO {
        override val type = ÅpenBehandlingTypeDTO.TILBAKEKREVING
    }
}

/**
 *  Returnerer en liste over søknader, rammebehandlinger, meldekortbehandlinger, klagebehandlinger og tilbakekrevinger som er åpne for behandling eller beslutning
 * */
fun Sak.tilÅpneBehandlingerDTO(): List<ÅpenBehandlingDTO> {
    val søknaderUtenBehandling: List<SøknadUtenBehandlingDTO> = this.tilSøknaderUtenBehandling()

    val åpneRammebehandlinger: List<ÅpenRammebehandlingDTO> = this.tilÅpneRammebehandlinger()

    val åpneMeldekort: List<ÅpenMeldekortbehandlingDTO> = this.tilÅpneMeldekortDTO()

    val åpneKlager = this.behandlinger.klagebehandlinger.filter { it.erÅpen }.toÅpenKlagebehandlingDTO()

    val åpneTilbakekrevinger: List<ÅpenTilbakekrevingDTO> = this.tilÅpneTilbakekrevingerDTO()

    return søknaderUtenBehandling
        .plus(åpneRammebehandlinger)
        .plus(åpneMeldekort)
        .plus(åpneKlager)
        .plus(åpneTilbakekrevinger)
        .sortedByDescending { it.opprettet }
}

private fun List<Klagebehandling>.toÅpenKlagebehandlingDTO(): List<ÅpenBehandlingDTO.ÅpenKlagebehandlingDTO> =
    this.map {
        ÅpenBehandlingDTO.ÅpenKlagebehandlingDTO(
            id = it.id.toString(),
            opprettet = it.opprettet,
            status = it.status.toKlagestatustypeDto(),
            saksbehandler = it.saksbehandler,
            resultat = it.resultat?.toKlageresultatstypeDto(),
        )
    }

/**
 *  Returnerer søknader som ikke har en tilknyttet søknadsbehandling
 *  Normalt skal det opprettes søknadsbehandlinger automatisk for nye søknader men vi tar med denne for å liste ut evt. søknader der dette har feilet
 *  */
private fun Sak.tilSøknaderUtenBehandling(): List<SøknadUtenBehandlingDTO> {
    return this.søknader
        .filter { søknad ->
            !søknad.erAvbrutt && rammebehandlinger.søknadsbehandlinger.none { it.søknad.id == søknad.id }
        }
        .map {
            SøknadUtenBehandlingDTO(
                id = it.id.toString(),
                opprettet = it.opprettet,
                kravtidspunkt = it.tidsstempelHosOss,
            )
        }
}

private fun Sak.tilÅpneRammebehandlinger(): List<ÅpenRammebehandlingDTO> {
    return this.rammebehandlinger.åpneBehandlinger.map {
        val id = it.id.toString()
        val periode = it.vedtaksperiode?.toDTO()
        val status = it.status.toBehandlingsstatusDTO()
        val underkjent = it.attesteringer.any { attestering -> attestering.isUnderkjent() }

        when (it) {
            is Søknadsbehandling -> ÅpenSøknadsbehandlingDTO(
                id = id,
                opprettet = it.opprettet,
                periode = periode,
                status = status,
                kravtidspunkt = it.kravtidspunkt,
                underkjent = underkjent,
                resultat = it.resultat.tilSøknadsbehandlingResultatTypeDTO(),
                saksbehandler = it.saksbehandler,
                beslutter = it.beslutter,
                erSattPåVent = it.ventestatus.erSattPåVent,
            )

            is Revurdering -> ÅpenRevurderingDTO(
                id = id,
                opprettet = it.opprettet,
                periode = periode,
                status = status,
                underkjent = underkjent,
                resultat = it.resultat.tilRevurderingResultatTypeDTO(),
                saksbehandler = it.saksbehandler,
                beslutter = it.beslutter,
                erSattPåVent = it.ventestatus.erSattPåVent,
            )
        }
    }
}

// Returnerer en evt. åpen meldekortbehandling
private fun Sak.tilÅpneMeldekortDTO(): List<ÅpenMeldekortbehandlingDTO> {
    val åpenMeldekortbehandling = this.meldekortbehandlinger.åpenMeldekortbehandling ?: return emptyList()

    return listOf(
        ÅpenMeldekortbehandlingDTO(
            id = åpenMeldekortbehandling.id.toString(),
            periode = åpenMeldekortbehandling.periode.toDTO(),
            opprettet = åpenMeldekortbehandling.opprettet,
            saksbehandler = åpenMeldekortbehandling.saksbehandler,
            beslutter = åpenMeldekortbehandling.beslutter,
        ),
    )
}

private fun Sak.tilÅpneTilbakekrevingerDTO(): List<ÅpenTilbakekrevingDTO> {
    return this.tilbakekrevinger
        .filter { it.status != TilbakekrevingBehandlingsstatus.AVSLUTTET }
        .map {
            ÅpenTilbakekrevingDTO(
                id = it.id.toString(),
                opprettet = it.opprettet,
                periode = it.kravgrunnlagTotalPeriode.toDTO(),
                status = it.statusIntern.tilTilbakekrevingBehandlingsstatusDTO(),
                totaltFeilutbetaltBeløp = it.totaltFeilutbetaltBeløp,
                saksbehandler = it.saksbehandler,
                beslutter = it.beslutter,
            )
        }
}
