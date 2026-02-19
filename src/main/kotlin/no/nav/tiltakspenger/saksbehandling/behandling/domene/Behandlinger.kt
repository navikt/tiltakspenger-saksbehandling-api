package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlinger
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
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

    val sisteNavkontor: Navkontor? by lazy {
        slåttSammen.asReversed().firstNotNullOfOrNull {
            when (it) {
                is Rammebehandling -> it.utbetaling?.navkontor
                is MeldekortBehandling -> it.navkontor
                else -> null
            }
        }
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
            rammebehandlinger = rammebehandlinger.oppdaterRammebehandling(oppdatertRammebehandling),
        )
    }

    fun oppdaterMeldekortbehandling(behandling: MeldekortBehandling): Behandlinger {
        return copy(meldekortbehandlinger = meldekortbehandlinger.oppdaterMeldekortbehandling(behandling))
    }

    fun leggTilKlagebehandling(klagebehandling: Klagebehandling): Behandlinger {
        return copy(klagebehandlinger = klagebehandlinger.leggTilKlagebehandling(klagebehandling))
    }

    fun oppdaterKlagebehandling(klagebehandling: Klagebehandling): Behandlinger {
        return copy(
            klagebehandlinger = klagebehandlinger.oppdaterKlagebehandling(klagebehandling),
            rammebehandlinger = rammebehandlinger.oppdaterKlagebehandling(klagebehandling),
        )
    }

    fun hentKlagebehandling(klagebehandlingId: KlagebehandlingId): Klagebehandling {
        return klagebehandlinger.hentKlagebehandling(klagebehandlingId)
    }

    fun hentÅpneRammebehandlingerMedKlagebehandlingId(klagebehandlingId: KlagebehandlingId): List<Rammebehandling> {
        return rammebehandlinger.åpneRammebehandlingerMedKlagebehandlingId(klagebehandlingId)
    }

    fun hentKlagebehandlingerSomSkalOversendesKlageinstansen(): List<Klagebehandling> {
        return klagebehandlinger.hentKlagebehandlingerSomSkalOversendesKlageinstansen()
    }

    init {
        require(slåttSammen.distinctBy { it.opprettet }.size == slåttSammen.size) {
            "Behandlingene kan ikke ha samme opprettet-tidspunkt."
        }
        require(slåttSammen.distinctBy { it.id }.size == slåttSammen.size) {
            "Behandlingene må ha unike IDer."
        }
        klagebehandlinger.filter { it.rammebehandlingId != null }.forEach { klagebehandling ->
            val rammebehandling = klagebehandling.rammebehandlingId!!.let { klagensRammebehandlingId ->
                rammebehandlinger.single { it.id == klagensRammebehandlingId }
            }
            require(rammebehandling.klagebehandling == klagebehandling) {
                "Klagebehandling ${klagebehandling.id} er tilknyttet rammebehandling ${rammebehandling.id}, men objektene er ikke identiske."
            }
            when (klagebehandling.status) {
                Klagebehandlingsstatus.KLAR_TIL_BEHANDLING -> require(rammebehandling.status == Rammebehandlingsstatus.KLAR_TIL_BEHANDLING) {
                    "Forventet at rammebehandling ${rammebehandling.id} er KLAR_TIL_BEHANDLING når klagebehandling ${klagebehandling.id} er KLAR_TIL_BEHANDLING, men var ${rammebehandling.status}. sakId =${klagebehandling.sakId}, saksnummer=${klagebehandling.saksnummer}"
                }

                Klagebehandlingsstatus.UNDER_BEHANDLING -> require(
                    rammebehandling.status in listOf(
                        Rammebehandlingsstatus.UNDER_BEHANDLING,
                        Rammebehandlingsstatus.KLAR_TIL_BESLUTNING,
                        Rammebehandlingsstatus.UNDER_BESLUTNING,
                    ),
                ) {
                    "Forventet at rammebehandling ${rammebehandling.id} er [UNDER_BEHANDLING, KLAR_TIL_BESLUTNING, UNDER_BESLUTNING] når klagebehandling ${klagebehandling.id} er UNDER_BEHANDLING, men var ${rammebehandling.status}. sakId =${klagebehandling.sakId}, saksnummer=${klagebehandling.saksnummer}"
                }

                Klagebehandlingsstatus.VEDTATT -> require(rammebehandling.status == Rammebehandlingsstatus.VEDTATT)

                Klagebehandlingsstatus.AVBRUTT, Klagebehandlingsstatus.OPPRETTHOLDT, Klagebehandlingsstatus.OVERSENDT -> throw IllegalStateException(
                    "En klagebehandling med status ${klagebehandling.status} skal ikke være tilknyttet en rammebehandling",
                )
            }
        }
        // Siden [Rammebehandling] er "eieren" av relasjonen til [Klagebehandling], sjekker vi statusen i initen til implementasjonene av [Rammebehandling].
    }

    companion object {
        fun empty() = Behandlinger(
            rammebehandlinger = Rammebehandlinger.empty(),
            meldekortbehandlinger = Meldekortbehandlinger.empty(),
            klagebehandlinger = Klagebehandlinger.empty(),
        )
    }
}
