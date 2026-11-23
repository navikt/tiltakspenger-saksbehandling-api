package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettedeRevurderingGrunner
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Grunnene lagres som `{ "grunner": [...] }`, i rekkefølgen de ble lagt til.
 * Eldre rader er én grunn uten `lagtTil`, med feltene i [AutomatiskOpprettetRevurderingGrunnDbJson] direkte på rotnivå.
 * De er migrert til det nye formatet, men en gammel replika kan fortsatt skrive dem under utrulling, så de må kunne leses.
 */
private data class AutomatiskOpprettedeRevurderingGrunnerDbJson(
    val grunner: List<AutomatiskOpprettetRevurderingGrunnDbJson>? = null,
)

/**
 * Endringen lagres som en liste av enkeltendringer, slik den ble lagret før endringene ble tolket til ett utfall.
 * Formatet er beholdt for å kunne lese rader som allerede er lagret.
 * Eldre rader kan inneholde feltet `hendelseId`, som ignoreres ved lesing.
 * Eldre rader mangler feltet `tiltaksdeltakerId`.
 * @param lagtTil Mangler bare i rader på det eldre formatet.
 */
private data class AutomatiskOpprettetRevurderingGrunnDbJson(
    val endringer: List<TiltaksdeltakerEndringDbJson>,
    val tiltaksdeltakerId: String? = null,
    val lagtTil: String? = null,
)

private data class TiltaksdeltakerEndringDbJson(
    val type: TiltaksdeltakerEndringTypeDb,
    val nySluttdato: String? = null,
    val nyStartdato: String? = null,
    val nyDeltakelsesprosent: Float? = null,
    val nyDagerPerUke: Float? = null,
    val nyStatus: String? = null,
)

private enum class TiltaksdeltakerEndringTypeDb {
    AVSLUTTET_SOM_FORVENTET,
    AVBRUTT_DELTAKELSE,
    IKKE_AKTUELL_DELTAKELSE,
    FORLENGELSE,
    AVKORTET,
    ENDRET_SLUTTDATO,
    ENDRET_STARTDATO,
    ENDRET_DELTAKELSESMENGDE,
    ENDRET_STATUS,
}

fun AutomatiskOpprettedeRevurderingGrunner.toDbJson(): String {
    return serialize(
        AutomatiskOpprettedeRevurderingGrunnerDbJson(
            grunner = grunner.map {
                AutomatiskOpprettetRevurderingGrunnDbJson(
                    endringer = it.endring.tilEndringerDbJson(),
                    tiltaksdeltakerId = it.tiltaksdeltakerId?.toString(),
                    lagtTil = it.lagtTil.toString(),
                )
            },
        ),
    )
}

/**
 * @param opprettet Når revurderingen ble opprettet.
 * Brukes som [AutomatiskOpprettetRevurderingGrunn.lagtTil] for rader på det eldre formatet.
 */
fun String.toAutomatiskOpprettedeRevurderingGrunner(opprettet: LocalDateTime): AutomatiskOpprettedeRevurderingGrunner {
    return when (val grunner = deserialize<AutomatiskOpprettedeRevurderingGrunnerDbJson>(this).grunner) {
        null -> AutomatiskOpprettedeRevurderingGrunner(
            deserialize<AutomatiskOpprettetRevurderingGrunnDbJson>(this).toDomain(lagtTil = opprettet),
        )

        else -> AutomatiskOpprettedeRevurderingGrunner(
            grunner.map { it.toDomain(lagtTil = LocalDateTime.parse(it.lagtTil!!)) }.toNonEmptyListOrNull()!!,
        )
    }
}

private fun AutomatiskOpprettetRevurderingGrunnDbJson.toDomain(lagtTil: LocalDateTime): AutomatiskOpprettetRevurderingGrunn {
    return AutomatiskOpprettetRevurderingGrunn(
        endring = endringer.toDomain(),
        tiltaksdeltakerId = tiltaksdeltakerId?.let { TiltaksdeltakerId.fromString(it) },
        lagtTil = lagtTil,
    )
}

/**
 * Endringen som en liste av enkeltendringer, i samme format som i [AutomatiskOpprettetRevurderingGrunnDbJson].
 * Brukes i `tiltakshistorikk`.
 */
fun TiltaksdeltakerEndring.toDbJson(): String = serialize(tilEndringerDbJson())

fun String.tilTiltaksdeltakerEndring(): TiltaksdeltakerEndring = deserialize<List<TiltaksdeltakerEndringDbJson>>(this).toDomain()

private fun TiltaksdeltakerEndring.tilEndringerDbJson(): List<TiltaksdeltakerEndringDbJson> = when (this) {
    is TiltaksdeltakerEndring.AvsluttetSomForventet -> listOf(TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.AVSLUTTET_SOM_FORVENTET))

    is TiltaksdeltakerEndring.AvbruttDeltakelse -> listOf(TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.AVBRUTT_DELTAKELSE))

    is TiltaksdeltakerEndring.IkkeAktuellDeltakelse -> listOf(TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.IKKE_AKTUELL_DELTAKELSE))

    is TiltaksdeltakerEndring.Forlengelse -> listOfNotNull(
        endretDeltakelsesmengde?.toDbJson(),
        TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.FORLENGELSE, nySluttdato = nySluttdato.toString()),
    )

    is TiltaksdeltakerEndring.Avkortet -> listOf(TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.AVKORTET, nySluttdato = nySluttdato.toString()))

    is TiltaksdeltakerEndring.AndreEndringer -> endringer.map { it.toDbJson() }
}

private fun TiltaksdeltakerEndring.Endringsdetalj.toDbJson(): TiltaksdeltakerEndringDbJson = when (this) {
    is TiltaksdeltakerEndring.EndretDeltakelsesmengde -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.ENDRET_DELTAKELSESMENGDE, nyDeltakelsesprosent = nyDeltakelsesprosent, nyDagerPerUke = nyDagerPerUke)
    is TiltaksdeltakerEndring.EndretStartdato -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.ENDRET_STARTDATO, nyStartdato = nyStartdato?.toString())
    is TiltaksdeltakerEndring.EndretSluttdato -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.ENDRET_SLUTTDATO, nySluttdato = nySluttdato?.toString())
    is TiltaksdeltakerEndring.EndretStatus -> TiltaksdeltakerEndringDbJson(type = TiltaksdeltakerEndringTypeDb.ENDRET_STATUS, nyStatus = nyStatus.name)
}

/**
 * Utfallene prioriteres i samme rekkefølge som når endringene tolkes, slik at også eldre rader med flere enkeltendringer gir ett utfall.
 */
private fun List<TiltaksdeltakerEndringDbJson>.toDomain(): TiltaksdeltakerEndring {
    fun finn(type: TiltaksdeltakerEndringTypeDb) = firstOrNull { it.type == type }

    val endretDeltakelsesmengde = finn(TiltaksdeltakerEndringTypeDb.ENDRET_DELTAKELSESMENGDE)?.let {
        TiltaksdeltakerEndring.EndretDeltakelsesmengde(nyDeltakelsesprosent = it.nyDeltakelsesprosent, nyDagerPerUke = it.nyDagerPerUke)
    }
    val forlengelse = finn(TiltaksdeltakerEndringTypeDb.FORLENGELSE)
    val avkortet = finn(TiltaksdeltakerEndringTypeDb.AVKORTET)

    return when {
        finn(TiltaksdeltakerEndringTypeDb.AVSLUTTET_SOM_FORVENTET) != null -> TiltaksdeltakerEndring.AvsluttetSomForventet

        finn(TiltaksdeltakerEndringTypeDb.AVBRUTT_DELTAKELSE) != null -> TiltaksdeltakerEndring.AvbruttDeltakelse

        finn(TiltaksdeltakerEndringTypeDb.IKKE_AKTUELL_DELTAKELSE) != null -> TiltaksdeltakerEndring.IkkeAktuellDeltakelse

        forlengelse != null -> TiltaksdeltakerEndring.Forlengelse(
            nySluttdato = LocalDate.parse(forlengelse.nySluttdato!!),
            endretDeltakelsesmengde = endretDeltakelsesmengde,
        )

        avkortet != null -> TiltaksdeltakerEndring.Avkortet(nySluttdato = LocalDate.parse(avkortet.nySluttdato!!))

        else -> TiltaksdeltakerEndring.AndreEndringer(
            endretDeltakelsesmengde = endretDeltakelsesmengde,
            endretStartdato = finn(TiltaksdeltakerEndringTypeDb.ENDRET_STARTDATO)?.let {
                TiltaksdeltakerEndring.EndretStartdato(nyStartdato = it.nyStartdato?.let(LocalDate::parse))
            },
            endretSluttdato = finn(TiltaksdeltakerEndringTypeDb.ENDRET_SLUTTDATO)?.let {
                TiltaksdeltakerEndring.EndretSluttdato(nySluttdato = it.nySluttdato?.let(LocalDate::parse))
            },
            endretStatus = finn(TiltaksdeltakerEndringTypeDb.ENDRET_STATUS)?.let {
                TiltaksdeltakerEndring.EndretStatus(nyStatus = TiltakDeltakerstatus.valueOf(it.nyStatus!!))
            },
        )
    }
}
