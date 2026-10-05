package com.leagueplans.uicommon.facades.fusejs

import scala.scalajs.js

/** Options for Fuse.js 7.5. Anything left undefined takes Fuse's default, given in brackets.
  *
  * @see https://www.fusejs.io/api/options.html
  */
trait FuseOptions extends js.Object {
  // Basic options

  /** Whether case matters (false) */
  var isCaseSensitive: js.UndefOr[Boolean] = js.undefined
  /** Whether accented letters match their plain forms (false) */
  var ignoreDiacritics: js.UndefOr[Boolean] = js.undefined
  /** Whether results include their score, from 0 for a perfect match to 1 for none (false) */
  var includeScore: js.UndefOr[Boolean] = js.undefined
  /** Whether results include which characters matched (false) */
  var includeMatches: js.UndefOr[Boolean] = js.undefined
  /** Matches shorter than this are ignored (1) */
  var minMatchCharLength: js.UndefOr[Int] = js.undefined
  /** Whether results are sorted by score (true) */
  var shouldSort: js.UndefOr[Boolean] = js.undefined
  /** Whether to keep searching a text after a perfect match is found (false) */
  var findAllMatches: js.UndefOr[Boolean] = js.undefined
  /** The fields to search. Without any, the list must hold strings. */
  var keys: js.UndefOr[js.Array[FuseOptions.Key]] = js.undefined

  // Fuzzy matching options

  /** Where in the text a match is expected (0) */
  var location: js.UndefOr[Int] = js.undefined
  /** How strict a match must be, from 0 for exact to 1 for anything (0.6) */
  var threshold: js.UndefOr[Double] = js.undefined
  /** How far from [[location]] a match can be before it scores as no match. Each character of
    * distance costs `1 / distance` of score. (100)
    */
  var distance: js.UndefOr[Int] = js.undefined
  /** Whether a match far from the start of the text counts as much as one near it (false) */
  var ignoreLocation: js.UndefOr[Boolean] = js.undefined

  // Advanced options

  /** Enables the unix-like search syntax, such as `'exact`, `^prefix` and `!not` (false) */
  var useExtendedSearch: js.UndefOr[Boolean] = js.undefined
  /** Enables word-by-word matching, scored by how rare each word is (false) */
  var useTokenSearch: js.UndefOr[Boolean] = js.undefined
  /** With [[useTokenSearch]], whether a match needs every word of the search, or any ("any") */
  var tokenMatch: js.UndefOr[FuseOptions.TokenMatch] = js.undefined
  /** With [[useTokenSearch]], splits text into words */
  var tokenize: js.UndefOr[js.RegExp | js.Function1[String, js.Array[String]]] = js.undefined
  /** Reads a key's value from an element, given the key's path */
  var getFn: js.UndefOr[js.Function2[js.Any, String | js.Array[String], String | js.Array[String]]] = js.undefined
  /** Orders the results, when [[shouldSort]] is on */
  var sortFn: js.UndefOr[js.Function2[FuseOptions.SortArg, FuseOptions.SortArg, Double]] = js.undefined
  /** Whether short fields lose their scoring advantage over long ones (false) */
  var ignoreFieldNorm: js.UndefOr[Boolean] = js.undefined
  /** How much a field's length affects its score (1) */
  var fieldNormWeight: js.UndefOr[Double] = js.undefined
}

object FuseOptions {
  /** A field to search: its name, its path as a list of names, or a [[KeyObject]] */
  type Key = String | js.Array[String] | KeyObject

  trait KeyObject extends js.Object {
    /** The field's name, or its path as a list of names */
    val name: String | js.Array[String]
    /** How much the field counts relative to the others (1) */
    var weight: js.UndefOr[Double] = js.undefined
    /** Reads the field's value from an element */
    var getFn: js.UndefOr[js.Function1[js.Any, String | js.Array[String]]] = js.undefined
  }

  type TokenMatch = "all" | "any"

  /** A result as [[FuseOptions.sortFn]] sees it */
  @js.native
  trait SortArg extends js.Object {
    /** The element's position in the list */
    val idx: Int = js.native
    val score: Double = js.native
  }
}
