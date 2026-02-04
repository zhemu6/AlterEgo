package org.zhemu.alterego.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.zhemu.alterego.model.dto.post.AgentPostGenerateRequest;
import org.zhemu.alterego.model.dto.post.AiPostGenerateResult;
import org.zhemu.alterego.model.dto.post.PostQueryRequest;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.PostTag;
import org.zhemu.alterego.model.entity.Species;
import org.zhemu.alterego.model.vo.PostVO;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private SpeciesService speciesService;

    @Autowired
    private PostTagService postTagService;

    @MockBean
    private AiPostGeneratorService aiPostGeneratorService;

    @Test
    void aiGeneratePost_shouldPersistTags() {
        Agent agent = createAgentWithSpecies();

        AiPostGenerateResult result = new AiPostGenerateResult();
        result.title = "title";
        result.content = "content";
        result.tags = Collections.singletonList("Tag");
        when(aiPostGeneratorService.generatePost(any(), any())).thenReturn(result);

        AgentPostGenerateRequest request = new AgentPostGenerateRequest();
        request.setAgentId(agent.getId());

        PostVO vo = postService.aiGeneratePost(request, agent.getUserId());
        List<PostTag> relations = postTagService.lambdaQuery().eq(PostTag::getPostId, vo.getId()).list();
        assertFalse(relations.isEmpty());
    }

    @Test
    void listPostByPage_shouldIncludeTags() {
        Agent agent = createAgentWithSpecies();

        AiPostGenerateResult result = new AiPostGenerateResult();
        result.title = "title";
        result.content = "content";
        result.tags = Collections.singletonList("Tag");
        when(aiPostGeneratorService.generatePost(any(), any())).thenReturn(result);

        AgentPostGenerateRequest request = new AgentPostGenerateRequest();
        request.setAgentId(agent.getId());
        PostVO created = postService.aiGeneratePost(request, agent.getUserId());

        PostQueryRequest queryRequest = new PostQueryRequest();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
        queryRequest.setAgentId(agent.getId());

        List<PostVO> records = postService.listPostByPage(queryRequest).getRecords();
        PostVO target = records.stream()
                .filter(item -> item.getId().equals(created.getId()))
                .findFirst()
                .orElse(records.get(0));
        assertNotNull(target.getTags());
    }

    private Agent createAgentWithSpecies() {
        Species species = new Species();
        String suffix = String.valueOf(System.currentTimeMillis());
        species.setName("test_" + suffix);
        species.setDescription("test");
        species.setIsDelete(0);
        speciesService.save(species);

        Agent agent = new Agent();
        agent.setUserId(Long.parseLong(suffix));
        agent.setSpeciesId(species.getId());
        agent.setAgentName("test_" + suffix);
        agent.setPersonality("test");
        agent.setEnergy(100);
        agent.setPostCount(0);
        agent.setCommentCount(0);
        agent.setLikeCount(0);
        agent.setDislikeCount(0);
        agent.setLastEnergyReset(java.time.LocalDate.now());
        agent.setCreateTime(java.time.LocalDateTime.now());
        agent.setUpdateTime(java.time.LocalDateTime.now());
        agent.setIsDelete(0);
        agentService.save(agent);
        return agent;
    }
}
