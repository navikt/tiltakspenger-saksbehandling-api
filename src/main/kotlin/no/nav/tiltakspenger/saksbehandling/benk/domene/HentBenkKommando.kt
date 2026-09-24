package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand

/**
 * Filtrene fanene tilbyr.
 * `null` betyr «ikke filtrert».
 *
 * [saksbehandler] er ett felt som treffer både saksbehandler og beslutter, fordi benken har én nedtrekksliste for de to.
 * Verdien [IKKE_TILDELT] betyr at saksbehandler eller beslutter (eller begge) ikke er tildelt.
 * [IKKE_TILDELT_SAKSBEHANDLER] treffer raden som ikke har saksbehandler.
 * [IKKE_TILDELT_BESLUTTER] treffer raden som har saksbehandler, men ikke beslutter.
 *
 * [skjulPåVent] tar bort behandlingene som er satt på vent, for saksbehandlere som vil se køen av det som faktisk kan jobbes med.
 *
 * [skjulVenterPåAnnenSaksbehandler] tar bort behandlingene som venter på en annen saksbehandler - enten de som kallende saksbehandler har sendt til beslutter, eller som kallende saksbehandler har underkjent
 *
 * Fanene uten et beslutningssteg (klage) har ikke [skjulVenterPåAnnenSaksbehandler]-filteret.
 */
sealed interface BenkFiltrering {
    val saksbehandler: String?
    val skjulPåVent: Boolean
    val skjulVenterPåAnnenSaksbehandler: Boolean

    companion object {
        const val IKKE_TILDELT: String = "IKKE_TILDELT"
        const val IKKE_TILDELT_SAKSBEHANDLER: String = "IKKE_TILDELT_SAKSBEHANDLER"
        const val IKKE_TILDELT_BESLUTTER: String = "IKKE_TILDELT_BESLUTTER"
    }
}

data class BenkSøknaderFiltrering(
    val status: BenkBehandlingsstatus?,
    val søknadstype: BenkSøknadstype?,
    val resultat: BenkSøknadsbehandlingResultat?,
    override val saksbehandler: String?,
    override val skjulPåVent: Boolean = false,
    override val skjulVenterPåAnnenSaksbehandler: Boolean = false,
) : BenkFiltrering

data class BenkRevurderingerFiltrering(
    val status: BenkBehandlingsstatus?,
    val resultat: BenkRevurderingResultat?,
    override val saksbehandler: String?,
    override val skjulPåVent: Boolean = false,
    override val skjulVenterPåAnnenSaksbehandler: Boolean = false,
) : BenkFiltrering

data class BenkMeldekortFiltrering(
    val status: BenkBehandlingsstatus?,
    val type: BenkMeldekortType?,
    override val saksbehandler: String?,
    override val skjulPåVent: Boolean = false,
    override val skjulVenterPåAnnenSaksbehandler: Boolean = false,
) : BenkFiltrering

data class BenkKlageFiltrering(
    val status: BenkKlagebehandlingStatus?,
    val resultat: BenkKlagebehandlingResultat?,
    override val saksbehandler: String?,
    override val skjulPåVent: Boolean = false,
) : BenkFiltrering {
    override val skjulVenterPåAnnenSaksbehandler: Boolean = false
}

data class BenkTilbakekrevingFiltrering(
    val status: BenkTilbakekrevingStatus?,
    val kilde: BenkTilbakekrevingKilde?,
    override val saksbehandler: String?,
    val minstebeløp: Long,
    override val skjulPåVent: Boolean = false,
    /** Tilbakekreving kaller beslutningssteget godkjenning, men filteret er det samme. */
    override val skjulVenterPåAnnenSaksbehandler: Boolean = false,
) : BenkFiltrering

/**
 * Ett kall henter én fane.
 * Kommandoen er derfor generisk over fanens filter og fanens sorteringskolonner, slik at feil kombinasjon ikke kompilerer.
 */
data class HentBenkKommando<F : BenkFiltrering, K : BenkSorteringKolonne>(
    val filtrering: F,
    val sortering: BenkSortering<K>,
    val paginering: BenkPaginering = BenkPaginering(),
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand

/**
 * Mine-fanen er behandlingene den innloggede er tildelt, som saksbehandler eller beslutter, vist som én seksjon per fane.
 * Hver seksjon er fanens egen spørring avgrenset til den innloggede, så radene, kolonnene og sorteringen er de samme som i fanen.
 *
 * [fane] avgrenser til én seksjon; `null` viser alle.
 * Fanens egne filtre (status, resultat osv.) tilbys ikke — de er gjort for å finne arbeid i køen, ikke i egen liste.
 * Seksjonene pagineres ikke og har ingen øvre grense: listen er avgrenset til én saksbehandler, og saksbehandler skal se alt hen er tildelt uten å bla.
 */
data class HentMineKommando(
    val fane: BenkFane?,
    val skjulPåVent: Boolean = false,
    val skjulVenterPåAnnenSaksbehandler: Boolean = false,
    val sortering: BenkMineSortering = BenkMineSortering(),
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand {
    init {
        require(fane != BenkFane.MINE) { "Mine-fanen kan ikke være en seksjon i seg selv" }
    }

    /** Seksjonene som skal hentes, i rekkefølgen benken viser fanene. */
    val seksjoner: List<BenkFane> = BenkFane.entries.filter { it != BenkFane.MINE && (fane == null || it == fane) }

    fun søknader(): HentBenkKommando<BenkSøknaderFiltrering, BenkSøknaderKolonne> = kommando(
        BenkSøknaderFiltrering(
            status = null,
            søknadstype = null,
            resultat = null,
            saksbehandler = null,
            skjulPåVent = skjulPåVent,
            skjulVenterPåAnnenSaksbehandler = skjulVenterPåAnnenSaksbehandler,
        ),
        sortering.søknader,
    )

    fun revurderinger(): HentBenkKommando<BenkRevurderingerFiltrering, BenkRevurderingerKolonne> = kommando(
        BenkRevurderingerFiltrering(
            status = null,
            resultat = null,
            saksbehandler = null,
            skjulPåVent = skjulPåVent,
            skjulVenterPåAnnenSaksbehandler = skjulVenterPåAnnenSaksbehandler,
        ),
        sortering.revurderinger,
    )

    fun meldekort(): HentBenkKommando<BenkMeldekortFiltrering, BenkMeldekortKolonne> = kommando(
        BenkMeldekortFiltrering(
            status = null,
            type = null,
            saksbehandler = null,
            skjulPåVent = skjulPåVent,
            skjulVenterPåAnnenSaksbehandler = skjulVenterPåAnnenSaksbehandler,
        ),
        sortering.meldekort,
    )

    fun klager(): HentBenkKommando<BenkKlageFiltrering, BenkKlageKolonne> = kommando(
        BenkKlageFiltrering(
            status = null,
            resultat = null,
            saksbehandler = null,
            skjulPåVent = skjulPåVent,
        ),
        sortering.klage,
    )

    /** Minstebeløpet er 0: en tilbakekreving den innloggede er tildelt, er «min» uansett beløp. */
    fun tilbakekrevinger(): HentBenkKommando<BenkTilbakekrevingFiltrering, BenkTilbakekrevingKolonne> = kommando(
        BenkTilbakekrevingFiltrering(
            status = null,
            kilde = null,
            saksbehandler = null,
            minstebeløp = 0,
            skjulPåVent = skjulPåVent,
            skjulVenterPåAnnenSaksbehandler = skjulVenterPåAnnenSaksbehandler,
        ),
        sortering.tilbakekreving,
    )

    private fun <F : BenkFiltrering, K : BenkSorteringKolonne> kommando(
        filtrering: F,
        sortering: BenkSortering<K>,
    ): HentBenkKommando<F, K> = HentBenkKommando(
        filtrering = filtrering,
        sortering = sortering,
        saksbehandler = saksbehandler,
        correlationId = correlationId,
    )
}

/** Sorteringen per seksjon i mine-fanen, med samme standard som i fanene. */
data class BenkMineSortering(
    val søknader: BenkSortering<BenkSøknaderKolonne> = BenkSortering(BenkSøknaderKolonne.STANDARD, BenkSorteringRetning.ASC),
    val revurderinger: BenkSortering<BenkRevurderingerKolonne> = BenkSortering(BenkRevurderingerKolonne.STANDARD, BenkSorteringRetning.ASC),
    val meldekort: BenkSortering<BenkMeldekortKolonne> = BenkSortering(BenkMeldekortKolonne.STANDARD, BenkSorteringRetning.ASC),
    val klage: BenkSortering<BenkKlageKolonne> = BenkSortering(BenkKlageKolonne.STANDARD, BenkSorteringRetning.ASC),
    val tilbakekreving: BenkSortering<BenkTilbakekrevingKolonne> = BenkSortering(BenkTilbakekrevingKolonne.STANDARD, BenkSorteringRetning.ASC),
)
