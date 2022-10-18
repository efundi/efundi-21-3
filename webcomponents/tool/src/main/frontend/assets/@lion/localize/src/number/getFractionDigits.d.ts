/**
 * @example
 * getFractionDigits('JOD'); // return 3
 *
 * @typedef {import('../../types/LocalizeMixinTypes').FormatNumberPart} FormatNumberPart
 * @param {string} [currency="EUR"] Currency code e.g. EUR
 * @returns {number} fraction for the given currency
 */
export function getFractionDigits(currency?: string | undefined): number;
export type FormatNumberPart = import("../../types/LocalizeMixinTypes.js").FormatNumberPart;
