def log = new File(basedir, 'build.log').text

['S-PASS-LOG', 'S-PASS-OUT'].each {
    assert !log.contains(it): "Output of passing test leaked: $it"
}
[
    'INFO com.example.it.SimpleTest - S-FAIL-LOG',
    'INFO com.example.it.SimpleTest - S-FAIL-OUT',
    'ERROR com.example.it.SimpleTest - S-FAIL-ERR',
].each {
    assert log.contains(it): "Missing output of failing test: $it"
}
return true
