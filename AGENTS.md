# AGENTS.md — tiltakspenger-saksbehandling-api

Dette repoet følger monorepo-konvensjonene i [`../AGENTS.md`](../AGENTS.md) og Kotlin/JVM-backendkonvensjonene i [`../AGENTS-backend.md`](../AGENTS-backend.md). Les disse først.

## Kodestil

- **KDoc og kommentarer: én setning per linje.** Hver setning i KDoc (`/** ... */`) og vanlige kommentarer skal stå på sin egen linje, med linjeskift etter punktum, i stedet for flere setninger pakket sammen på én lang linje. Dette gir renere diffs og bedre lesbarhet. Gjelder også fler-setnings `//`-kommentarer. Se den utfyllende regelen i [`../AGENTS-backend.md`](../AGENTS-backend.md#språk-og-stil) — den gjentas her fordi agenter glipper på den gang på gang.
