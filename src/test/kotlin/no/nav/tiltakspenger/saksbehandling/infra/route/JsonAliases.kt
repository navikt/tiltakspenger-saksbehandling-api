package no.nav.tiltakspenger.saksbehandling.infra.route

import com.fasterxml.jackson.databind.JsonNode
import org.json.JSONObject

/**
 * Se også [no.nav.tiltakspenger.saksbehandling.sak.infra.routes.SakDTO]
 */
typealias SakDTOJson = JSONObject

/**
 * Se også [no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingDTO]
 */
typealias RammebehandlingDTOJson = JSONObject

/**
 * Se også [no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.RammevedtakDTO]
 */
typealias RammevedtakDTOJson = JSONObject

/**
 * Se også [no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagebehandlingDTO]
 */
typealias KlagebehandlingDTOJson = JsonNode

/**
 * Se også [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldeperiodeKjedeDTO]
 */
typealias MeldeperiodeKjedeDTOJson = JSONObject

/**
 * Se også [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortBehandlingDTO]
 */
typealias MeldekortBehandlingDTOJson = JSONObject
