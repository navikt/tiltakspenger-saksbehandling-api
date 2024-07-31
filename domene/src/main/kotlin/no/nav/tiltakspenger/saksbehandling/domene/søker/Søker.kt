package no.nav.tiltakspenger.saksbehandling.domene.søker

import mu.KotlinLogging
import no.nav.tiltakspenger.felles.Rolle
import no.nav.tiltakspenger.felles.Saksbehandler
import no.nav.tiltakspenger.felles.SøkerId
import no.nav.tiltakspenger.felles.exceptions.TilgangException
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.PersonopplysningerSøker

private val LOG = KotlinLogging.logger {}
private val SECURELOG = KotlinLogging.logger("tjenestekall")

// Denne klassen brukes for å koble en ident sammen med en søkerId slik at vi kan bruke søkerId i stedet for ident
// Det er også her vi sjekker om saksbehandler har tilgang til personopplysningene til søkeren.
class Søker private constructor(
    val søkerId: SøkerId,
    val fnr: Fnr, // TODO skal denne ligge her, eller holder det at den ligger i personopplysninger?
    var personopplysninger: PersonopplysningerSøker?, // TODO her trenger vi kanskje en liste hvis vi vil ha med barn
) {
    constructor(
        fnr: Fnr,
    ) : this(
        søkerId = randomId(),
        fnr = fnr,
        personopplysninger = null,
    )

    fun sjekkOmSaksbehandlerHarTilgang(saksbehandler: Saksbehandler) {
        fun sjekkBeskyttelsesbehovStrengtFortrolig(harBeskyttelsesbehovStrengtFortrolig: Boolean) {
            if (harBeskyttelsesbehovStrengtFortrolig) {
                SECURELOG.info("erStrengtFortrolig")
                // Merk at vi ikke sjekker egenAnsatt her, strengt fortrolig trumfer det
                if (Rolle.STRENGT_FORTROLIG_ADRESSE in saksbehandler.roller) {
                    SECURELOG.info("Access granted to strengt fortrolig for ${fnr.verdi}")
                } else {
                    SECURELOG.info("Access denied to strengt fortrolig for ${fnr.verdi}")
                    throw TilgangException("Saksbehandler har ikke tilgang")
                }
            }
        }

        fun sjekkBeskytelsesbehovFortrolig(harBeskyttelsesbehovFortrolig: Boolean) {
            if (harBeskyttelsesbehovFortrolig) {
                SECURELOG.info("erFortrolig")
                // Merk at vi ikke sjekker egenAnsatt her, fortrolig trumfer det
                if (Rolle.FORTROLIG_ADRESSE in saksbehandler.roller) {
                    SECURELOG.info("Access granted to fortrolig for ${fnr.verdi}")
                } else {
                    SECURELOG.info("Access denied to fortrolig for ${fnr.verdi}")
                    throw TilgangException("Saksbehandler har ikke tilgang")
                }
            }
        }

        fun sjekkBeskyttelsesbehovSkjermet(
            erEgenAnsatt: Boolean,
            harBeskyttelsesbehovFortrolig: Boolean,
            harBeskyttelsesbehovStrengtFortrolig: Boolean,
        ) {
            if (erEgenAnsatt && !(harBeskyttelsesbehovFortrolig || harBeskyttelsesbehovStrengtFortrolig)) {
                SECURELOG.info("erEgenAnsatt")
                // Er kun egenAnsatt, har ikke et beskyttelsesbehov i tillegg
                if (Rolle.SKJERMING in saksbehandler.roller) {
                    SECURELOG.info("Access granted to egen ansatt for ${fnr.verdi}")
                } else {
                    SECURELOG.info("Access denied to egen ansatt for ${fnr.verdi}")
                    throw TilgangException("Saksbehandler har ikke tilgang")
                }
            }
        }

        fun sjekkSøkerForTilgang(personopplysninger: PersonopplysningerSøker) {
            val harBeskyttelsesbehovFortrolig = personopplysninger.fortrolig
            val harBeskyttelsesbehovStrengtFortrolig =
                personopplysninger.strengtFortrolig || personopplysninger.strengtFortroligUtland
            val erEgenAnsatt = personopplysninger.skjermet ?: false

            sjekkBeskyttelsesbehovStrengtFortrolig(harBeskyttelsesbehovStrengtFortrolig)
            sjekkBeskytelsesbehovFortrolig(harBeskyttelsesbehovFortrolig)
            sjekkBeskyttelsesbehovSkjermet(
                erEgenAnsatt,
                harBeskyttelsesbehovFortrolig,
                harBeskyttelsesbehovStrengtFortrolig,
            )
        }

        personopplysninger?.let { sjekkSøkerForTilgang(it) }
            ?: throw TilgangException("Umulig å vurdere tilgang")
    }

    companion object {
        fun randomId() = SøkerId.random()

        fun fromDb(
            søkerId: SøkerId,
            fnr: Fnr,
            personopplysninger: PersonopplysningerSøker?,
        ) = Søker(
            søkerId = søkerId,
            fnr = fnr,
            personopplysninger = personopplysninger,
        )
    }
}
