package no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknadstype
import no.nav.tiltakspenger.saksbehandling.benk.domene.finnGyldigeKommandoer
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommandoDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandler.tilDTO

enum class BenkSøknadstypeDTO {
    DIGITAL,
    PAPIR_SKJEMA,
    PAPIR_FRIHAND,
    MODIA,
    ANNET,
}

enum class BenkSøknadsbehandlingResultatDTO {
    INNVILGELSE,
    AVSLAG,
    IKKE_VALGT,
}

data class BenkSøknadsbehandlingDTO(
    override val type: BenkBehandlingstypeDTO = BenkBehandlingstypeDTO.SØKNADSBEHANDLING,
    override val id: String,
    override val sakId: String,
    override val fnr: String,
    override val saksnummer: String,
    override val startet: String,
    override val sistEndret: String,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val erUnderkjent: Boolean,
    override val ventestatus: BenkVentestatusDTO,
    val status: BenkBehandlingsstatusDTO,
    val søknadstype: BenkSøknadstypeDTO,
    val kravtidspunkt: String,
    val resultat: BenkSøknadsbehandlingResultatDTO,
    val gyldigeKommandoer: List<SaksbehandlerBehandlingKommandoDTO>,
) : BenkBehandlingDTO

fun BenkSøknadsbehandling.toDTO(saksbehandler: Saksbehandler): BenkSøknadsbehandlingDTO = BenkSøknadsbehandlingDTO(
    id = id.toString(),
    sakId = felles.sakId.toString(),
    fnr = felles.fnr.verdi,
    saksnummer = felles.saksnummer.verdi,
    startet = felles.startet.toString(),
    sistEndret = felles.sistEndret.toString(),
    saksbehandler = felles.saksbehandler,
    beslutter = felles.beslutter,
    erUnderkjent = felles.erUnderkjent,
    ventestatus = felles.ventestatus.toDTO(),
    status = status.toDTO(),
    søknadstype = søknadstype.toDTO(),
    kravtidspunkt = kravtidspunkt.toString(),
    resultat = resultat.toDTO(),
    gyldigeKommandoer = finnGyldigeKommandoer(saksbehandler).tilDTO(),
)

private fun BenkSøknadstype.toDTO(): BenkSøknadstypeDTO = when (this) {
    BenkSøknadstype.DIGITAL -> BenkSøknadstypeDTO.DIGITAL
    BenkSøknadstype.PAPIR_SKJEMA -> BenkSøknadstypeDTO.PAPIR_SKJEMA
    BenkSøknadstype.PAPIR_FRIHAND -> BenkSøknadstypeDTO.PAPIR_FRIHAND
    BenkSøknadstype.MODIA -> BenkSøknadstypeDTO.MODIA
    BenkSøknadstype.ANNET -> BenkSøknadstypeDTO.ANNET
}

private fun BenkSøknadsbehandlingResultat.toDTO(): BenkSøknadsbehandlingResultatDTO = when (this) {
    BenkSøknadsbehandlingResultat.INNVILGELSE -> BenkSøknadsbehandlingResultatDTO.INNVILGELSE
    BenkSøknadsbehandlingResultat.AVSLAG -> BenkSøknadsbehandlingResultatDTO.AVSLAG
    BenkSøknadsbehandlingResultat.IKKE_VALGT -> BenkSøknadsbehandlingResultatDTO.IKKE_VALGT
}

fun BenkSøknadstypeDTO.tilDomene(): BenkSøknadstype = when (this) {
    BenkSøknadstypeDTO.DIGITAL -> BenkSøknadstype.DIGITAL
    BenkSøknadstypeDTO.PAPIR_SKJEMA -> BenkSøknadstype.PAPIR_SKJEMA
    BenkSøknadstypeDTO.PAPIR_FRIHAND -> BenkSøknadstype.PAPIR_FRIHAND
    BenkSøknadstypeDTO.MODIA -> BenkSøknadstype.MODIA
    BenkSøknadstypeDTO.ANNET -> BenkSøknadstype.ANNET
}

fun BenkSøknadsbehandlingResultatDTO.tilDomene(): BenkSøknadsbehandlingResultat = when (this) {
    BenkSøknadsbehandlingResultatDTO.INNVILGELSE -> BenkSøknadsbehandlingResultat.INNVILGELSE
    BenkSøknadsbehandlingResultatDTO.AVSLAG -> BenkSøknadsbehandlingResultat.AVSLAG
    BenkSøknadsbehandlingResultatDTO.IKKE_VALGT -> BenkSøknadsbehandlingResultat.IKKE_VALGT
}
