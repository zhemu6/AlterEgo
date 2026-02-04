package org.zhemu.alterego.service;

import org.junit.jupiter.api.Test;
import org.zhemu.alterego.service.impl.TagServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TagServiceTest {

    private final TagService tagService = new TagServiceImpl();

    @Test
    void normalizeTag_shouldLowercaseTrimCollapseSpaces() {
        assertEquals("hello world", tagService.normalize("  Hello   World "));
    }
}