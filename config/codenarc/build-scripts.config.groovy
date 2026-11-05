ruleset {
    ruleset('rulesets/basic.xml') {
        DeadCode(priority: 1)
    }
    ruleset('rulesets/braces.xml')
    ruleset('rulesets/comments.xml')
    ruleset('rulesets/concurrency.xml')
    ruleset('rulesets/convention.xml') {
        CompileStatic(priority: 3)
        FieldTypeRequired(priority: 3)
        IfStatementCouldBeTernary(priority: 3)
        NoDef(priority: 3)
        TrailingComma(enabled: false)
        VariableTypeRequired(priority: 3)
    }
    ruleset('rulesets/design.xml')
    ruleset('rulesets/dry.xml') {
        DuplicateListLiteral(priority: 3)
        DuplicateMapLiteral(priority: 2)
        DuplicateNumberLiteral(priority: 3, ignoreNumbers: '0,1,2,3,-1')
        DuplicateStringLiteral(priority: 3)
    }
    ruleset('rulesets/exceptions.xml') {
        ThrowException(priority: 3)
        ThrowNullPointerException(priority: 3)
        ThrowRuntimeException(priority: 3)
        ThrowThrowable(priority: 3)
    }
    ruleset('rulesets/formatting.xml') {
        BracesForClass(priority: 3)
        BracesForForLoop(priority: 3)
        BracesForIfElse(priority: 3)
        BracesForMethod(priority: 3)
        BracesForTryCatchFinally(priority: 3)
        Indentation(priority: 3, spacesPerIndentLevel: 4)
        SpaceAroundMapEntryColon(enabled: false)
    }
    ruleset('rulesets/generic.xml')
    ruleset('rulesets/grails.xml')
    ruleset('rulesets/groovyism.xml') {
        ExplicitCallToEqualsMethod(priority: 3)
    }
    ruleset('rulesets/imports.xml')
    ruleset('rulesets/jdbc.xml')
    ruleset('rulesets/junit.xml')
    ruleset('rulesets/logging.xml') {
        Println(enabled: false)
    }
    ruleset('rulesets/naming.xml')
    ruleset('rulesets/security.xml')
    ruleset('rulesets/serialization.xml')
    ruleset('rulesets/size.xml') {
        AbcMetric(priority: 3)
        CrapMetric(enabled: false)
    }
    ruleset('rulesets/unnecessary.xml') {
        UnnecessaryReturnKeyword(enabled: false)
    }
    ruleset('rulesets/unused.xml') {
        UnusedArray(priority: 1)
        UnusedObject(priority: 1)
        UnusedPrivateField(priority: 1)
        UnusedPrivateMethod(priority: 1)
        UnusedPrivateMethodParameter(priority: 1)
        UnusedVariable(priority: 1)
    }
}
