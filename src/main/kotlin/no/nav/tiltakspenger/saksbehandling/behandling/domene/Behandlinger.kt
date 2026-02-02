package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

data class Behandlinger(
    val rammebehandlinger: Rammebehandlinger,
    val meldekortbehandlinger: Meldekortbehandlinger,
    val klagebehandlinger: Klagebehandlinger,
) {

    val slåttSammen: List<Behandling> by lazy {
        (rammebehandlinger + meldekortbehandlinger + klagebehandlinger).sortedBy { it.opprettet }
    }

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

    fun oppdaterRammebehandling(oppdatertRammebehandling: Rammebehandling): Behandlinger {
        return copy(
            klagebehandlinger = if (oppdatertRammebehandling.klagebehandling != null) {
                klagebehandlinger.oppdaterKlagebehandling(
                    oppdatertRammebehandling.klagebehandling!!,
                )
            } else {
                klagebehandlinger
            },
            rammebehandlinger = rammebehandlinger.oppdaterBehandling(oppdatertRammebehandling),
        )
    }

    fun oppdaterMeldekortbehandling(behandling: MeldekortBehandling): Behandlinger {
        return copy(meldekortbehandlinger = meldekortbehandlinger.oppdaterMeldekortbehandling(behandling))
    }

    fun leggTilKlagebehandling(klagebehandling: Klagebehandling): Behandlinger {
        return copy(klagebehandlinger = klagebehandlinger.leggTilKlagebehandling(klagebehandling))
    }

    fun oppdaterKlagebehandling(klagebehandling: Klagebehandling): Behandlinger {
        return copy(klagebehandlinger = klagebehandlinger.oppdaterKlagebehandling(klagebehandling))
    }

    fun hentKlagebehandling(klagebehandlingId: KlagebehandlingId): Klagebehandling {
        return klagebehandlinger.hentKlagebehandling(klagebehandlingId)
    }

    fun hentÅpneRammebehandlingerMedKlagebehandlingId(klagebehandlingId: KlagebehandlingId): List<Rammebehandling> {
        return rammebehandlinger.åpneRammebehandlingerMedKlagebehandlingId(klagebehandlingId)
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
            klagebehandlinger = Klagebehandlinger.empty(),
        )
    }
}
