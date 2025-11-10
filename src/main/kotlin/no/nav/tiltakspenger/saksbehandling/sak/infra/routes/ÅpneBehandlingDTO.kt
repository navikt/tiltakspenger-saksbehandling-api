package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRevurderingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilSøknadsbehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toBehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldeperiodeKjedeStatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldeperiodeKjedeStatusDTO
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDateTime

sealed interface ÅpenBehandlingDTO {
    val id: String
    val sakId: String
    val saksnummer: String
    val opprettet: LocalDateTime
    val type: ÅpenBehandlingTypeDTO

    enum class ÅpenBehandlingTypeDTO {
        SØKNAD,
        SØKNADSBEHANDLING,
        REVURDERING,
        MELDEKORT,
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
        override val sakId: String,
        override val saksnummer: String,
        val kravtidspunkt: LocalDateTime,
    ) : ÅpenBehandlingDTO {
        override val type = ÅpenBehandlingTypeDTO.SØKNAD
    }

    data class ÅpenSøknadsbehandlingDTO(
        override val id: String,
        override val sakId: String,
        override val saksnummer: String,
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
        override val sakId: String,
        override val saksnummer: String,
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

    data class MeldeperiodeKjedeSomMåBehandlesDTO(
        override val id: String,
        override val sakId: String,
        override val saksnummer: String,
        override val opprettet: LocalDateTime,
        val periode: PeriodeDTO,
        val meldekortBehandlingId: String?,
        val status: MeldeperiodeKjedeStatusDTO,
        val saksbehandler: String?,
        val beslutter: String?,
    ) : ÅpenBehandlingDTO {
        override val type = ÅpenBehandlingTypeDTO.MELDEKORT
    }
}

/**
 *  Returnerer en liste over søknader, rammebehandlinger og meldeperiodekjeder som er åpne for behandling eller beslutning
 * */
fun Sak.tilÅpneBehandlingerDTO(): List<ÅpenBehandlingDTO> {
    val søknaderUtenBehandling: List<ÅpenBehandlingDTO.SøknadUtenBehandlingDTO> = this.tilSøknaderUtenBehandling()

    val åpneRammebehandlinger: List<ÅpenBehandlingDTO.ÅpenRammebehandlingDTO> = this.tilÅpneRammebehandlinger()

    val meldeperiodeKjederSomMåBehandles: List<ÅpenBehandlingDTO.MeldeperiodeKjedeSomMåBehandlesDTO> =
        this.tilMeldeperiodeKjederSomMåBehandles()

    return søknaderUtenBehandling
        .plus(åpneRammebehandlinger)
        .plus(meldeperiodeKjederSomMåBehandles)
        .sortedByDescending { it.opprettet }
}

// Skal returnere søknader som ikke har en tilknyttet søknadsbehandling
private fun Sak.tilSøknaderUtenBehandling(): List<ÅpenBehandlingDTO.SøknadUtenBehandlingDTO> {
    return this.søknader
        .filter { søknad ->
            !søknad.erAvbrutt && rammebehandlinger.søknadsbehandlinger.none { it.søknad.id == søknad.id }
        }
        .map {
            ÅpenBehandlingDTO.SøknadUtenBehandlingDTO(
                id = it.id.toString(),
                sakId = this.id.toString(),
                saksnummer = this.saksnummer.toString(),
                opprettet = it.opprettet,
                kravtidspunkt = it.tidsstempelHosOss,
            )
        }
}

private fun Sak.tilÅpneRammebehandlinger(): List<ÅpenBehandlingDTO.ÅpenRammebehandlingDTO> {
    return this.rammebehandlinger.åpneBehandlinger.map {
        when (it) {
            is Søknadsbehandling -> ÅpenBehandlingDTO.ÅpenSøknadsbehandlingDTO(
                id = it.id.toString(),
                sakId = this.id.toString(),
                saksnummer = this.saksnummer.toString(),
                opprettet = it.opprettet,
                periode = it.virkningsperiode?.toDTO(),
                status = it.status.toBehandlingsstatusDTO(),
                kravtidspunkt = it.kravtidspunkt,
                underkjent = it.attesteringer.any { attestering -> attestering.isUnderkjent() },
                resultat = it.resultat.tilSøknadsbehandlingResultatTypeDTO(),
                saksbehandler = it.saksbehandler,
                beslutter = it.beslutter,
                erSattPåVent = it.ventestatus.erSattPåVent,
            )

            is Revurdering -> ÅpenBehandlingDTO.ÅpenRevurderingDTO(
                id = it.id.toString(),
                sakId = this.id.toString(),
                saksnummer = this.saksnummer.toString(),
                opprettet = it.opprettet,
                periode = it.virkningsperiode?.toDTO(),
                status = it.status.toBehandlingsstatusDTO(),
                underkjent = it.attesteringer.any { attestering -> attestering.isUnderkjent() },
                resultat = it.resultat.tilRevurderingResultatTypeDTO(),
                saksbehandler = it.saksbehandler,
                beslutter = it.beslutter,
                erSattPåVent = it.ventestatus.erSattPåVent,
            )
        }
    }
}

private fun Sak.tilMeldeperiodeKjederSomMåBehandles(): List<ÅpenBehandlingDTO.MeldeperiodeKjedeSomMåBehandlesDTO> {
    return this.meldeperiodeKjeder.mapNotNull { kjede ->
        val kjedeId = kjede.kjedeId

        val åpenMeldekortBehandling = meldekortbehandlinger.åpenMeldekortBehandling

        if (åpenMeldekortBehandling != null) {
            return@mapNotNull ÅpenBehandlingDTO.MeldeperiodeKjedeSomMåBehandlesDTO(
                id = kjedeId.toString(),
                sakId = this.id.toString(),
                saksnummer = this.saksnummer.toString(),
                meldekortBehandlingId = åpenMeldekortBehandling.id.toString(),
                periode = kjede.periode.toDTO(),
                opprettet = åpenMeldekortBehandling.opprettet,
                status = åpenMeldekortBehandling.status.tilMeldeperiodeKjedeStatusDTO(),
                saksbehandler = åpenMeldekortBehandling.saksbehandler,
                beslutter = åpenMeldekortBehandling.beslutter,
            )
        }

        val sisteBehandledeMeldekort =
            meldekortbehandlinger.behandledeMeldekortPerKjede[kjedeId]?.lastOrNull()

        val brukersMeldekort = brukersMeldekort
            .filter { it.kjedeId == kjedeId }

        val sisteBrukersMeldekort = brukersMeldekort.maxByOrNull { it.mottatt }

        if (sisteBrukersMeldekort == null) {
            return@mapNotNull null
        }

        if (sisteBehandledeMeldekort == null || sisteBrukersMeldekort.mottatt > sisteBehandledeMeldekort.iverksattTidspunkt) {
            return@mapNotNull ÅpenBehandlingDTO.MeldeperiodeKjedeSomMåBehandlesDTO(
                id = kjedeId.toString(),
                sakId = this.id.toString(),
                saksnummer = this.saksnummer.toString(),
                meldekortBehandlingId = null,
                periode = kjede.periode.toDTO(),
                opprettet = sisteBrukersMeldekort.mottatt,
                status = if (brukersMeldekort.size == 1) {
                    MeldeperiodeKjedeStatusDTO.KLAR_TIL_BEHANDLING
                } else {
                    MeldeperiodeKjedeStatusDTO.KORRIGERT_MELDEKORT
                },
                saksbehandler = null,
                beslutter = null,
            )
        }

        return@mapNotNull null
    }
}
