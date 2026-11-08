package com.tripfactory.nomad.service.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class SearchSpellTest {

    private static final Set<String> VOCAB = Set.of("lalbagh", "cubbon", "park", "bengaluru", "mumbai", "marina", "beach", "charminar");

    @Test
    void knowsCityNicknames() {
        assertThat(SearchSpell.correct("Bangalore", VOCAB)).isEqualTo("bengaluru");
        assertThat(SearchSpell.correct("vizag", VOCAB)).isEqualTo("vishakapatnam");
    }

    @Test
    void fixesSmallTypos() {
        assertThat(SearchSpell.correct("lalbgah", VOCAB)).isEqualTo("lalbagh");
        assertThat(SearchSpell.correct("cubon park", VOCAB)).isEqualTo("cubbon park");
        assertThat(SearchSpell.correct("charminr", VOCAB)).isEqualTo("charminar");
    }

    @Test
    void leavesGoodOrHopelessTextAlone() {
        assertThat(SearchSpell.correct("marina beach", VOCAB)).isNull();
        assertThat(SearchSpell.correct("xqzwvk", VOCAB)).isNull();
        assertThat(SearchSpell.correct("  ", VOCAB)).isNull();
    }

    @Test
    void swappedLettersCountAsOneMistake() {
        assertThat(SearchSpell.distance("mumabi", "mumbai")).isEqualTo(1);
    }
}
