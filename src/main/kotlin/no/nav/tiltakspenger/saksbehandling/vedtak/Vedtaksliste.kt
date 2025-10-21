package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortVedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortVedtaksliste
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDate

/**
 * En kombinasjon av [no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortVedtak] og [Rammevedtak].
 */
data class Vedtaksliste(
    val rammevedtaksliste: Rammevedtaksliste,
    val meldekortVedtaksliste: MeldekortVedtaksliste,
) : List<Vedtak> by slåSammenVedtakslistene(rammevedtaksliste, meldekortVedtaksliste) {

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

    val slåttSammen: List<Vedtak> by lazy { slåSammenVedtakslistene(rammevedtaksliste, meldekortVedtaksliste) }

    val fnr: Fnr? by lazy { slåttSammen.distinctBy { it.fnr }.map { it.fnr }.singleOrNullOrThrow() }
    val sakId: SakId? by lazy { slåttSammen.distinctBy { it.sakId }.map { it.sakId }.singleOrNullOrThrow() }
    val saksnummer: Saksnummer? by lazy {
        slåttSammen.distinctBy { it.saksnummer }.map { it.saksnummer }.singleOrNullOrThrow()
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

    fun leggTilRammevedtak(rammevedtak: Rammevedtak): Vedtaksliste {
        return copy(rammevedtaksliste = rammevedtaksliste.leggTil(rammevedtak))
    }

    fun leggTilMeldekortvedtak(meldekortVedtak: MeldekortVedtak): Vedtaksliste {
        return copy(meldekortVedtaksliste = meldekortVedtaksliste.leggTil(meldekortVedtak))
    }

    init {
        require(slåttSammen.distinctBy { it.opprettet }.size == slåttSammen.size) {
            "Vedtakene i Vedtaksliste kan ikke ha samme opprettet-tidspunkt."
        }
        require(slåttSammen.distinctBy { it.id }.size == slåttSammen.size) {
            "Vedtakene i Vedtaksliste må ha unike IDer."
        }
    }

    companion object {
        fun empty(): Vedtaksliste {
            return Vedtaksliste(
                rammevedtaksliste = Rammevedtaksliste.empty(),
                meldekortVedtaksliste = MeldekortVedtaksliste.empty(),
            )
        }
    }
}

private fun slåSammenVedtakslistene(
    rammevedtaksliste: Rammevedtaksliste,
    meldekortVedtaksliste: MeldekortVedtaksliste,

): List<Vedtak> {
    return (rammevedtaksliste.verdi + meldekortVedtaksliste.verdi).sortedBy { it.opprettet }
}
