package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtaksliste
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDate

/**
 * En kombinasjon av [no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak] og [Rammevedtak].
 */
data class Vedtaksliste(
    val rammevedtaksliste: Rammevedtaksliste,
    val meldekortvedtaksliste: Meldekortvedtaksliste,
) : List<Vedtak> by slåSammenVedtakslistene(rammevedtaksliste, meldekortvedtaksliste) {

    val avslagsvedtak: List<Rammevedtak> by lazy {
        rammevedtaksliste.avslagsvedtak
    }
    val avslåtteBehandlinger: List<Søknadsbehandling> by lazy {
        avslagsvedtak.map { it.behandling as Søknadsbehandling }
    }

    val meldeperiodeBeregninger: MeldeperiodeBeregningerVedtatt by lazy {
        MeldeperiodeBeregningerVedtatt.fraVedtaksliste(this)
    }

    fun hentAvslåtteBehandlingerForSøknadId(søknadId: SøknadId): List<Søknadsbehandling> {
        return avslåtteBehandlinger.filter { it.søknad.id == søknadId }
    }

    val fnr: Fnr? by lazy { this.distinctBy { it.fnr }.map { it.fnr }.singleOrNullOrThrow() }
    val sakId: SakId? by lazy { this.distinctBy { it.sakId }.map { it.sakId }.singleOrNullOrThrow() }
    val saksnummer: Saksnummer? by lazy {
        this.distinctBy { it.saksnummer }.map { it.saksnummer }.singleOrNullOrThrow()
    }

    val harFørstegangsvedtak: Boolean by lazy {
        rammevedtaksliste.harFørstegangsvedtak
    }

    fun harInnvilgetTiltakspengerPåDato(dato: LocalDate): Boolean {
        return rammevedtaksliste.harInnvilgetTiltakspengerPåDato(dato)
    }

    fun harInnvilgetTiltakspengerEtterDato(dato: LocalDate): Boolean {
        return rammevedtaksliste.harInnvilgetTiltakspengerEtterDato(dato)
    }

    /**
     * Legger til et rammevedtak i vedtaklisten og oppdaterer omgjortAvRammevedtak per vedtak
     */
    fun leggTilRammevedtak(rammevedtak: Rammevedtak): Vedtaksliste {
        return copy(rammevedtaksliste = rammevedtaksliste.leggTil(rammevedtak))
    }

    fun leggTilMeldekortvedtak(meldekortvedtak: Meldekortvedtak): Vedtaksliste {
        return copy(meldekortvedtaksliste = meldekortvedtaksliste.leggTil(meldekortvedtak))
    }

    /**
     * Tenkt kalt under behandlingen for å avgjøre hvilke rammevedtak som vil bli omgjort.
     * Husk og dobbeltsjekk denne ved iverksettelse.
     * @param virkningsperiode vurderingsperioden/vedtaksperioden. Kan være en ren innvilgelse, et rent opphør eller en blanding.
     */
    fun finnRammevedtakSomOmgjøres(
        virkningsperiode: Periode,
    ): OmgjørRammevedtak {
        return rammevedtaksliste.finnVedtakSomOmgjøres(virkningsperiode)
    }

    fun hentRammevedtakForBehandlingId(behandlingId: BehandlingId): Rammevedtak {
        return rammevedtaksliste.hentVedtakForBehandlingId(behandlingId)
    }

    init {
        require(this.nonDistinctBy { it.opprettet }.isEmpty()) {
            "Vedtakene i Vedtaksliste kan ikke ha samme opprettet-tidspunkt."
        }
        require(this.nonDistinctBy { it.id }.isEmpty()) {
            "Vedtakene i Vedtaksliste må ha unike IDer."
        }
    }

    companion object {
        fun empty(): Vedtaksliste {
            return Vedtaksliste(
                rammevedtaksliste = Rammevedtaksliste.empty(),
                meldekortvedtaksliste = Meldekortvedtaksliste.empty(),
            )
        }
    }
}

private fun slåSammenVedtakslistene(
    rammevedtaksliste: Rammevedtaksliste,
    meldekortvedtaksliste: Meldekortvedtaksliste,
): List<Vedtak> {
    return (rammevedtaksliste.verdi + meldekortvedtaksliste.verdi).sortedBy { it.opprettet }
}
