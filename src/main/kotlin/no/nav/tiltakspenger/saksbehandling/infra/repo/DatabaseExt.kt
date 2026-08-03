package no.nav.tiltakspenger.saksbehandling.infra.repo

import kotliquery.Row

// TODO jah: Denne bør flyttes til: no.nav.tiltakspenger.infra.repo  eller libs.
fun Row.booleanOrNull(name: String): Boolean? = this.anyOrNull(name)?.let { this.boolean(name) }
