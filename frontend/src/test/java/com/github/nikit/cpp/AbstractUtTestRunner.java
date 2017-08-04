package com.github.nikit.cpp;

/**
 * Created by nik on 27.05.17.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nikit.cpp.controllers.CommentControllerTest;
import com.github.nikit.cpp.dto.PostDTO;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.AbstractConfigurableEmbeddedServletContainer;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        classes = {Launcher.class, SwaggerConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@AutoConfigureMockMvc(printOnlyOnFailure = false, print = MockMvcPrint.LOG_DEBUG)
@AutoConfigureRestDocs(outputDir = TestConstants.RESTDOCS_SNIPPETS_DIR)
@Transactional
public abstract class AbstractUtTestRunner {

    @Autowired
    protected MockMvc mockMvc;

//    @Value("${server.port}")
//    protected int serverPort;
//
//    @Value("${server.contextPath}")
//    protected String contextPath;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected AbstractConfigurableEmbeddedServletContainer abstractConfigurableEmbeddedServletContainer;

    public String urlWithContextPath(){
        return "http://127.0.0.1:"+abstractConfigurableEmbeddedServletContainer.getPort()+abstractConfigurableEmbeddedServletContainer.getContextPath();
    }

    @Value("${custom.it.user}")
    protected String username;

    @Value("${custom.it.password}")
    protected String password;

    @Autowired
    protected ObjectMapper objectMapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUtTestRunner.class);

    public PostDTO getPost(long postId) throws Exception {
        MvcResult getPostRequest = mockMvc.perform(
                get(Constants.Uls.API+Constants.Uls.POST+"/"+postId)
        )
                .andExpect(status().isOk())
                .andReturn();
        String getStr = getPostRequest.getResponse().getContentAsString();
        LOGGER.debug(getStr);
        return objectMapper.readValue(getStr, PostDTO.class);
    }
}
