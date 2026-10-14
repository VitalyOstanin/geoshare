package io.github.vitalyostanin.geoshare.service

// Documents the project owner types by hand in the console, the false places
// and the group's settings, pass no rule on the way in. Their fields are read
// from the document's map: the typed getters throw on a value of another type,
// and one field typed wrong must cost that field, not the subscription.

/** The field as a number, or nothing where it is missing or is not one. */
internal fun numberIn(fields: Map<String, Any?>, field: String): Double? =
    (fields[field] as? Number)?.toDouble()

/** The field as text, or nothing where it is missing or is not text. */
internal fun textIn(fields: Map<String, Any?>, field: String): String? = fields[field] as? String
