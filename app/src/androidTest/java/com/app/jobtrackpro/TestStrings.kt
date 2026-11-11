package com.app.jobtrackpro

import androidx.test.platform.app.InstrumentationRegistry

internal fun testString(resourceId: Int, vararg arguments: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *arguments)
