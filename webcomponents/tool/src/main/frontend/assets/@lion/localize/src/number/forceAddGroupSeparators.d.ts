/**
 * Add separators when they are not present
 *
 * @typedef {import('../../types/LocalizeMixinTypes').FormatNumberPart} FormatNumberPart
 * @param {FormatNumberPart[]} formattedParts
 * @param {string} groupSeparator
 * @returns {FormatNumberPart[]}
 */
export function forceAddGroupSeparators(formattedParts: FormatNumberPart[], groupSeparator: string): FormatNumberPart[];
/**
 * Add separators when they are not present
 */
export type FormatNumberPart = import("../../types/LocalizeMixinTypes").FormatNumberPart;
