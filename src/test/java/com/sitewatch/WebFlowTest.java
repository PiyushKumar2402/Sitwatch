package com.sitewatch;

import com.sitewatch.model.Website;
import com.sitewatch.repo.WebsiteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tests registration, login, security rules and the pages themselves (including template rendering). */
@SpringBootTest(properties = {
        "sitewatch.scheduler.enabled=false",
        "sitewatch.allow-private-hosts=false",
        "spring.datasource.url=jdbc:h2:mem:webtest;MODE=MySQL;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class WebFlowTest {

    @Autowired MockMvc mvc;
    @Autowired WebsiteRepository websites;

    @Test
    void anonymousVisitorsAreSentToLogin() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/api/websites/1/history")).andExpect(status().is3xxRedirection());
    }

    @Test
    void publicPagesRender() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(containsString("Sign in")));
        mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void registerLoginAddWebsiteAndIsolation() throws Exception {
        register("alice");
        register("bob");

        // duplicate username is refused
        mvc.perform(post("/register").with(csrf()).param("username", "alice").param("password", "Secret123!"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("already taken")));
        // weak password is refused
        mvc.perform(post("/register").with(csrf()).param("username", "carol").param("password", "short"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("8 to 72")));

        mvc.perform(formLogin("/login").user("alice").password("Secret123!")).andExpect(authenticated().withUsername("alice"));
        mvc.perform(formLogin("/login").user("alice").password("wrong-password")).andExpect(unauthenticated());

        // add a website (public IP literal, so no DNS is needed)
        mvc.perform(post("/websites").with(user("alice")).with(csrf())
                        .param("name", "Google DNS").param("url", "http://8.8.8.8").param("intervalSeconds", "60"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"));
        assertEquals(1, websites.count());

        // SSRF protection: local addresses are refused
        mvc.perform(post("/websites").with(user("alice")).with(csrf()).param("url", "http://127.0.0.1:8080"))
                .andExpect(flash().attributeExists("error"));
        // interval below the minimum is refused
        mvc.perform(post("/websites").with(user("alice")).with(csrf()).param("url", "http://8.8.4.4").param("intervalSeconds", "5"))
                .andExpect(flash().attributeExists("error"));
        assertEquals(1, websites.count());

        Website w = websites.findAll().get(0);
        mvc.perform(get("/dashboard").with(user("alice")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Google DNS")));
        mvc.perform(get("/websites/" + w.getId()).with(user("alice")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Daily report")));
        mvc.perform(get("/api/websites/" + w.getId() + "/history").with(user("alice")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));

        // another user cannot see or delete it
        mvc.perform(get("/websites/" + w.getId()).with(user("bob"))).andExpect(status().isNotFound());
        mvc.perform(post("/websites/" + w.getId() + "/delete").with(user("bob")).with(csrf())).andExpect(status().isNotFound());
        assertEquals(1, websites.count());

        // pause, then delete as the owner
        mvc.perform(post("/websites/" + w.getId() + "/toggle").with(user("alice")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/websites/" + w.getId() + "/delete").with(user("alice")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(0, websites.count());
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/websites").with(user("alice")).param("url", "http://8.8.8.8")).andExpect(status().isForbidden());
    }

    private void register(String name) throws Exception {
        mvc.perform(post("/register").with(csrf()).param("username", name).param("email", name + "@example.com")
                        .param("password", "Secret123!"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?registered"));
    }
}
