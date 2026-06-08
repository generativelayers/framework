/**
 * Generative Layers — Platform Adapters.
 *
 * <p>Bridges between the core framework and BDI agent platforms.
 *
 * <h2>Architecture</h2>
 * <pre>
 *   ResourceActions (interface)   — canonical 16-command contract
 *       ↑ implements
 *   DirectAdapter (concrete)      — all logic: provider lifecycle,
 *                                   body invocation, candidate deliberation
 *       ↑ held by
 *   ├── astra/AstraAdapter        — ASTRA Module binding
 *   ├── jacamo/JaCaMoAdapter      — JaCaMo/CArtAgO Artifact binding
 *   └── jason/JasonAdapter        — Jason singleton binding
 * </pre>
 *
 * <h2>Adapters</h2>
 * <ul>
 *   <li>{@link gl.adapter.ResourceActions} — 16-command interface</li>
 *   <li>{@link gl.adapter.DirectAdapter} — single implementation</li>
 *   <li>{@link gl.adapter.astra.AstraAdapter} — ASTRA wrapper</li>
 *   <li>{@link gl.adapter.jacamo.JaCaMoAdapter} — JaCaMo/CArtAgO artifact</li>
 *   <li>{@link gl.adapter.jason.JasonAdapter} — Jason singleton</li>
 * </ul>
 *
 * @see gl Core framework (governance engine, domain types)
 * @see gl.provider Provider SPI for LLM backends
 */
package gl.adapter;
