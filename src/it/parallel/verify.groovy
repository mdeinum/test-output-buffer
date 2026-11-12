def log = new File(basedir, 'build.log').text

assert !(log =~ /PASS-[ABC]\d-/): "Output of passing tests leaked"
['A', 'B', 'C'].each { c ->
    [0, 5].each { i ->
        (0..4).each { line ->
            assert log.contains("FAIL-$c$i-LOG-$line"): "Missing FAIL-$c$i-LOG-$line"
            assert log.contains("FAIL-$c$i-OUT-$line"): "Missing FAIL-$c$i-OUT-$line"
        }
    }
}
// each failing test's output is contiguous (not interleaved with another test)
['A', 'B', 'C'].each { c ->
    [0, 5].each { i ->
        def lines = log.readLines().findAll { it.contains('FAIL-') && it.startsWith('LOG ') }
        def idx = lines.findIndexValues { it.contains("FAIL-$c$i-") }
        assert idx.last() - idx.first() == idx.size() - 1: "Output of FAIL-$c$i is interleaved"
    }
}
return true
