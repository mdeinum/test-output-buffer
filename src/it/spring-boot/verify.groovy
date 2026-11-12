def log = new File(basedir, 'build.log').text

['BOOT-PASS-LOG', 'BOOT-PASS-OUT', 'Started PassingBootTest'].each {
    assert !log.contains(it): "Output of passing test leaked: $it"
}
// Spring Boot reconfigures Logback; output must still be captured and shown on failure
['BOOT-FAIL-LOG', 'BOOT-FAIL-OUT', 'Started FailingBootTest'].each {
    assert log.contains(it): "Missing output of failing test: $it"
}
assert log =~ /INFO .* com\.example\.it\.FailingBootTest\s+: BOOT-FAIL-OUT/: "System.out not routed through Boot's log format"
return true
