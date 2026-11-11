# Test conventions

## Annotations and formatting

Place function annotations on their own lines directly above the declaration:

```kotlin
@Test
fun asAppException_coroutineCancellation_rethrowsCancellation() {
    assertThrows(CancellationException::class.java) {
        CancellationException().asAppException()
    }
}
```

Use the same layout for @Before and @After. Separate test functions with a blank
line. Use four-space indentation and expand dense argument lists and try/catch/
finally blocks for readability. The tests were formatted with ktfmt 0.64 using
--kotlinlang-style; this tool was used locally without adding a build dependency.
Property annotations such as @get:Rule may remain attached to the property.

The Android guide also allows a single annotation without arguments on the same
line as a declaration. The separate-line test-function layout is the stricter
project convention requested for JobTrack Pro, not a JUnit syntax requirement.

## Test names

Use English names in this form:

```text
operationOrBehavior_condition_expectedResult
```

Each component uses lowerCamelCase; underscores separate the three logical parts.
For integration/UI tests, the first component can describe the workflow rather
than a single production method. Describe observable behavior actually asserted
by the test. For example:

- saveLocal_staleRevision_throwsConflict
- validateDraft_requiredFieldsBlank_returnsInvalidFields
- homeContent_storageFailure_offersRetryAction

There is no universal mandatory test-name template. This is the project's choice,
consistent with the Android/Kotlin guides permitting underscores in test names.
Use the same convention for local JVM tests and device instrumentation tests.
Avoid spaces/backtick sentence names because the app supports Android API 26;
Kotlin documents Android runtime support for names with spaces from API 30.

Test classes should identify the component or behavior under test and end in Test.
This formatting/naming change preserves existing test cases and assertions.

## References

- [Android Kotlin style: annotations](https://developer.android.com/kotlin/style-guide#annotations)
- [Android Kotlin style: function names](https://developer.android.com/kotlin/style-guide#function_names)
- [Kotlin test method names](https://kotlinlang.org/docs/coding-conventions.html#names-for-test-methods)
- [ktfmt command-line usage](https://github.com/Kotlin/ktfmt#installation)
