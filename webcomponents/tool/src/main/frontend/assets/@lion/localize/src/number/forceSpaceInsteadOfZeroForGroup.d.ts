/**
 * @desc Intl uses 0 as group separator for bg-BG locale.
 * This should be a ' '
 *
 * @typedef {import('../../types/LocalizeMixinTypes').FormatNumberPart} FormatNumberPart
 * @param {FormatNumberPart[]} formattedParts
 * @returns {FormatNumberPart[]} corrected formatted parts
 */
export function forceSpaceInsteadOfZeroForGroup(formattedParts: FormatNumberPart[]): FormatNumberPart[];
export type FormatNumberPart = import("../../types/LocalizeMixinTypes").FormatNumberPart;
