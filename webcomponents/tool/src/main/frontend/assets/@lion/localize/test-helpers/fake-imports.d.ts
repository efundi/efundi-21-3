/**
 * @param {string} path
 * @param {Object} data
 */
export function setupFakeImport(path: string, data: Object): void;
/**
 * @param {string[]} namespaces
 * @param {string[]} locales
 */
export function setupEmptyFakeImportsFor(namespaces: string[], locales: string[]): void;
export function resetFakeImport(): void;
/**
 * @param {string} path
 * @param {number} [ms=0]
 * @returns {Promise.<Object>}
 */
export function fakeImport(path: string, ms?: number | undefined): Promise<Object>;
