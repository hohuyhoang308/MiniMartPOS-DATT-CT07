package com.pos.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class CodeGeneratorTest {

    @Test
    void build_pads_sequence_and_prefixes_date() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        assertThat(CodeGenerator.build("HD", 1, 4)).isEqualTo("HD" + today + "-0001");
        assertThat(CodeGenerator.build("PN", 12, 3)).isEqualTo("PN" + today + "-012");
    }

    @Test
    void date_prefix_format() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        assertThat(CodeGenerator.datePrefix("HD")).isEqualTo("HD" + today + "-");
    }
}
