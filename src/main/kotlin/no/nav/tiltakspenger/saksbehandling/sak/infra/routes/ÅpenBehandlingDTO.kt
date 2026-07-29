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
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlageresultatstypeDto.Companion.toKlageresultatstypDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagestatustypeDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagestatustypeDto.Companion.toKlagestatustypeDto
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.ÅpenBehandlingDTO.SøknadUtenBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.ÅpenBehandlingDTO.ÅpenRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.ÅpenBehandlingDTO.ÅpenRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.ÅpenBehandlingDTO.ÅpenSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.ÅpenBehandlingDTO.ÅpentMeldekortDTO
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
        KLAGE,
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

    data class ÅpentMeldekortDTO(
        override val id: String,
        override val sakId: String,
        override val saksnummer: String,
        override val opprettet: LocalDateTime,
        val subtype: ÅpentMeldekortType,
        val periode: PeriodeDTO,
        val meldekortbehandlingId: String?,
        val saksbehandler: String?,
        val beslutter: String?,
    ) : ÅpenBehandlingDTO {
        override val type = ÅpenBehandlingTypeDTO.MELDEKORT

        enum class ÅpentMeldekortType {
            MELDEKORTBEHANDLING,
            INNSENDT_MELDEKORT,
            KORRIGERT_MELDEKORT,
        }
    }

    data class ÅpenKlagebehandlingDTO(
        override val id: String,
        override val sakId: String,
        override val saksnummer: String,
        override val opprettet: LocalDateTime,
        val status: KlagestatustypeDto,
        val saksbehandler: String?,
        val resultat: KlageresultatstypeDto?,
    ) : ÅpenBehandlingDTO {
        override val type = ÅpenBehandlingTypeDTO.KLAGE
    }
}

/**
 *  Returnerer en liste over søknader, rammebehandlinger og meldeperiodekjeder som er åpne for behandling eller beslutning
 * */
fun Sak.tilÅpneBehandlingerDTO(): List<ÅpenBehandlingDTO> {
    val søknaderUtenBehandling: List<SøknadUtenBehandlingDTO> = this.tilSøknaderUtenBehandling()

    val åpneRammebehandlinger: List<ÅpenRammebehandlingDTO> = this.tilÅpneRammebehandlinger()

    val åpneMeldekort: List<ÅpentMeldekortDTO> = this.tilÅpneMeldekortDTO()

    val åpneKlager = this.behandlinger.klagebehandlinger.filter { it.erÅpen }.toÅpenKlagebehandlingDTO()

    return søknaderUtenBehandling
        .plus(åpneRammebehandlinger)
        .plus(åpneMeldekort)
        .plus(åpneKlager)
        .sortedByDescending { it.opprettet }
}

private fun List<Klagebehandling>.toÅpenKlagebehandlingDTO(): List<ÅpenBehandlingDTO.ÅpenKlagebehandlingDTO> =
    this.map {
        ÅpenBehandlingDTO.ÅpenKlagebehandlingDTO(
            id = it.id.toString(),
            sakId = it.sakId.toString(),
            saksnummer = it.saksnummer.toString(),
            opprettet = it.opprettet,
            status = it.status.toKlagestatustypeDto(),
            saksbehandler = it.saksbehandler,
            resultat = it.resultat?.toKlageresultatstypDto(),
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
                sakId = this.id.toString(),
                saksnummer = this.saksnummer.toString(),
                opprettet = it.opprettet,
                kravtidspunkt = it.tidsstempelHosOss,
            )
        }
}

private fun Sak.tilÅpneRammebehandlinger(): List<ÅpenRammebehandlingDTO> {
    val sakId = this.id.toString()
    val saksnummer = this.saksnummer.toString()

    return this.rammebehandlinger.åpneBehandlinger.map {
        val id = it.id.toString()
        val periode = it.vedtaksperiode?.toDTO()
        val status = it.status.toBehandlingsstatusDTO()
        val underkjent = it.attesteringer.any { attestering -> attestering.isUnderkjent() }

        when (it) {
            is Søknadsbehandling -> ÅpenSøknadsbehandlingDTO(
                id = id,
                sakId = sakId,
                saksnummer = saksnummer,
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
                sakId = sakId,
                saksnummer = saksnummer,
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

// Returnerer meldeperiodekjeder med en åpen meldekortbehandling, eller med et brukers meldekort som ikke har blitt behandlet
private fun Sak.tilÅpneMeldekortDTO(): List<ÅpentMeldekortDTO> {
    val sakId = this.id.toString()
    val saksnummer = this.saksnummer.toString()

    return this.meldeperiodeKjeder.mapNotNull { kjede ->
        val kjedeId = kjede.kjedeId
        val periode = kjede.periode.toDTO()

        meldekortbehandlinger.åpenMeldekortbehandling?.also {
            if (it.kjedeIder.contains(kjedeId)) {
                return@mapNotNull ÅpentMeldekortDTO(
                    id = kjedeId.toString(),
                    sakId = sakId,
                    saksnummer = saksnummer,
                    periode = periode,
                    meldekortbehandlingId = it.id.toString(),
                    opprettet = it.opprettet,
                    saksbehandler = it.saksbehandler,
                    beslutter = it.beslutter,
                    subtype = ÅpentMeldekortDTO.ÅpentMeldekortType.MELDEKORTBEHANDLING,
                )
            }
        }

        val brukersMeldekort = brukersMeldekort
            .filter { it.kjedeId == kjedeId }

        val sisteBrukersMeldekort = brukersMeldekort.maxByOrNull { it.mottatt }

        if (sisteBrukersMeldekort == null) {
            return@mapNotNull null
        }

        // Skal ikke fremheve meldekort hvis de venter på automatisk behandling
        if (sisteBrukersMeldekort.behandletAutomatiskStatus === MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING) {
            return@mapNotNull null
        }

        val sisteBehandledeMeldekort =
            meldekortbehandlinger.behandledeMeldekortPerKjede[kjedeId]?.lastOrNull()

        val harBehandletMeldekortet =
            sisteBehandledeMeldekort != null && sisteBehandledeMeldekort.sistEndret > sisteBrukersMeldekort.mottatt

        if (harBehandletMeldekortet) {
            return@mapNotNull null
        }

        val harAvbruttBehandlingAvMeldekortet = meldekortbehandlinger.avbrutteMeldekortbehandlinger
            .filter { it.kjedeIder.contains(kjedeId) }
            .any { it.avbrutt != null && it.avbrutt.tidspunkt > sisteBrukersMeldekort.mottatt }

        // Stygg workaround for at saksbehandler skal kunne bli kvitt korrigeringer som ikke skal behandles i vår løsning
        if (harAvbruttBehandlingAvMeldekortet) {
            return@mapNotNull null
        }

        return@mapNotNull ÅpentMeldekortDTO(
            id = kjedeId.toString(),
            sakId = this.id.toString(),
            saksnummer = this.saksnummer.toString(),
            periode = periode,
            meldekortbehandlingId = null,
            opprettet = sisteBrukersMeldekort.mottatt,
            saksbehandler = null,
            beslutter = null,
            subtype = if (brukersMeldekort.size == 1) {
                ÅpentMeldekortDTO.ÅpentMeldekortType.INNSENDT_MELDEKORT
            } else {
                ÅpentMeldekortDTO.ÅpentMeldekortType.KORRIGERT_MELDEKORT
            },
        )
    }
}
