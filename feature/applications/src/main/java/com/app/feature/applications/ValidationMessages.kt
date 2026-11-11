package com.app.feature.applications

import com.app.core.model.ApplicationField

fun ApplicationField.validationMessageRes(): Int = when (this) {
    ApplicationField.COMPANY -> R.string.validation_company
    ApplicationField.ROLE -> R.string.validation_role
    ApplicationField.LOCATION -> R.string.validation_location
    ApplicationField.DATE -> R.string.validation_date
    ApplicationField.URL -> R.string.validation_url
    ApplicationField.NOTES -> R.string.validation_notes
}
