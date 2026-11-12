def log = new File(basedir, 'build.log').text

// passing tests: nothing
['J5-PASS-LOG', 'J5-PASS-OUT', 'J5-PASS-ERR', 'J4-PASS-LOG', 'J4-PASS-OUT'].each {
    assert !log.contains(it): "Output of passing test leaked: $it"
}

// failing tests: everything, System.out/err routed through SLF4J
[
    'LOG INFO com.example.it.Junit5Test - J5-BEFORE-ALL',
    'LOG INFO com.example.it.Junit5Test - J5-FAIL-LOG',
    'LOG INFO com.example.it.Junit5Test - J5-FAIL-OUT',
    'LOG ERROR com.example.it.Junit5Test - J5-FAIL-ERR',
    'LOG INFO com.example.it.Junit5Test - J5-FAIL-PARTIAL',
    'LOG INFO com.example.it.Junit5Test - J5-FAIL-BACKGROUND',
    'LOG INFO com.example.it.Junit4Test - J4-FAIL-LOG',
    'LOG INFO com.example.it.Junit4Test - J4-FAIL-OUT',
    'LOG ERROR com.example.it.Junit4Test - J4-FAIL-ERR',
].each {
    assert log.contains(it): "Missing output of failing test: $it"
}

// order preserved
assert log.indexOf('J5-FAIL-LOG') < log.indexOf('J5-FAIL-OUT')
assert log.indexOf('J5-FAIL-OUT') < log.indexOf('J5-FAIL-ERR')

// attached to the failing test in the Surefire XML report, not to another test
['Junit5Test': 'J5-FAIL-OUT', 'Junit4Test': 'J4-FAIL-OUT'].each { testClass, marker ->
    def report = new groovy.xml.XmlSlurper().parse(
            new File(basedir, "target/surefire-reports/TEST-com.example.it.${testClass}.xml"))
    def failing = report.testcase.find { it.@name == 'failing' }
    def passing = report.testcase.find { it.@name == 'passing' }
    assert failing.'system-out'.text().contains(marker): "$testClass: output not attached to failing test"
    assert passing.'system-out'.text().isEmpty(): "$testClass: output attached to passing test"
}
return true
