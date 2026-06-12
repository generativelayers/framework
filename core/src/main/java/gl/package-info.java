/**
 * Generative Layers (GL) -- Core Framework.
 *
 * <p>This package contains the governance engine, SPI interfaces,
 * and utility classes.
 *
 * <h2>Package Structure</h2>
 * <pre>
 *   gl/           -- Governance engine (this package)
 *   gl/model/     -- Domain records and enums
 *   gl/body/      -- Affordance model (BodyAffordance, GenerativeBody)
 *   gl/provider/  -- LLM backends (Gemini, ChatCompletions)
 *   gl/adapter/   -- Platform bindings (ASTRA, Jason, JaCaMo)
 * </pre>
 *
 * <h2>GL v2 Lifecycle</h2>
 * <pre>
 *   see > bind > call > result > candidate > check > get
 *       > judge > decide > accept/reject > knowledge > explain
 * </pre>
 *
 * <h2>Governance Engine</h2>
 * <ul>
 *   <li>{@link gl.GovernanceKernel} -- Central pipeline:
 *       policy check > generate > validate > create candidate</li>
 *   <li>{@link gl.GovernanceKernelFactory} -- Factory with configurable ports</li>
 *   <li>{@link gl.KernelPorts} -- SPI interfaces</li>
 *   <li>{@link gl.KernelDefaults} -- Default implementations</li>
 *   <li>{@link gl.InMemoryKernelStores} -- In-memory stores</li>
 *   <li>{@link gl.Outcomes} -- Enum container for outcomes and verdicts</li>
 *   <li>{@link gl.Ids} -- UUID-based ID generation</li>
 * </ul>
 *
 * @see gl.model Domain model (records and enums)
 * @see gl.body Affordance model
 * @see gl.provider Provider SPI for LLM backends
 * @see gl.adapter Platform adapters (ASTRA, Jason, JaCaMo)
 */
package gl;
