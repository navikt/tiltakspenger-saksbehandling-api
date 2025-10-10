package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

data class Behandlinger(
    val rammebehandlinger: Rammebehandlinger,
    val meldekortbehandlinger: Meldekortbehandlinger,
) : List<Behandling> by slåSammenBehandlingene(rammebehandlinger, meldekortbehandlinger) {

    val slåttSammen: List<Behandling> by lazy { slåSammenBehandlingene(rammebehandlinger, meldekortbehandlinger) }

    val fnr: Fnr? by lazy { slåttSammen.distinctBy { it.fnr }.map { it.fnr }.singleOrNullOrThrow() }
    val sakId: SakId? by lazy { slåttSammen.distinctBy { it.sakId }.map { it.sakId }.singleOrNullOrThrow() }
    val saksnummer: Saksnummer? by lazy {
        slåttSammen.distinctBy { it.saksnummer }.map { it.saksnummer }.singleOrNullOrThrow()
    }

    val harEnEllerFlereÅpneBehandlinger: Boolean by lazy {
        rammebehandlinger.harÅpenBehandling || meldekortbehandlinger.harÅpenBehandling
    }

    fun leggTilSøknadsbehandling(behandling: Søknadsbehandling): Behandlinger {
        return copy(rammebehandlinger = rammebehandlinger.leggTilSøknadsbehandling(behandling))
    }

    fun leggTilRevurdering(revurdering: Revurdering): Behandlinger {
        return copy(rammebehandlinger = rammebehandlinger.leggTilRevurdering(revurdering))
    }

    fun leggTilMeldekortUnderBehandling(behandling: MeldekortUnderBehandling): Behandlinger {
        return copy(meldekortbehandlinger = meldekortbehandlinger.leggTil(behandling))
    }

    fun leggTilMeldekortBehandletAutomatisk(behandling: MeldekortBehandletAutomatisk): Behandlinger {
        return copy(meldekortbehandlinger = meldekortbehandlinger.leggTil(behandling))
    }

    fun oppdaterRammebehandling(avbruttBehandling: Rammebehandling): Behandlinger {
        return copy(rammebehandlinger = rammebehandlinger.oppdaterBehandling(avbruttBehandling))
    }

    fun oppdaterMeldekortbehandling(behandling: MeldekortBehandling): Behandlinger {
        return copy(meldekortbehandlinger = meldekortbehandlinger.oppdaterMeldekortbehandling(behandling))
    }

    init {
        require(slåttSammen.distinctBy { it.opprettet }.size == slåttSammen.size) {
            "Behandlingene kan ikke ha samme opprettet-tidspunkt."
        }
        require(slåttSammen.distinctBy { it.id }.size == slåttSammen.size) {
            "Behandlingene må ha unike IDer."
        }
    }
    companion object {
        fun empty() = Behandlinger(
            rammebehandlinger = Rammebehandlinger.empty(),
            meldekortbehandlinger = Meldekortbehandlinger.empty(),
        )
    }
}

private fun slåSammenBehandlingene(
    rammebehandlinger: Rammebehandlinger,
    meldekortbehandlinger: Meldekortbehandlinger,
): List<Behandling> {
    return (rammebehandlinger + meldekortbehandlinger).sortedBy { it.opprettet }
}
