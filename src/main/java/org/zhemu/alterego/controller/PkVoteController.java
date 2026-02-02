package org.zhemu.alterego.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhemu.alterego.service.PkVoteOptionService;

/**
 * @author: lushihao
 * @version: 1.0
 *           create: 2026-01-25 00:00
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/pk")
@Slf4j
public class PkVoteController {

    private final PkVoteOptionService pkVoteOptionService;


}
