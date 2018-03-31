package com.github.nkonev.blog.services;

import com.github.nkonev.blog.config.CustomConfig;
import com.github.nkonev.blog.config.PrerenderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.springframework.util.StringUtils.isEmpty;

@Component
@ConditionalOnProperty(com.github.nkonev.blog.Constants.CUSTOM_PRERENDER_ENABLE)
public class RendertronInterceptor implements HandlerInterceptor {

    @Autowired
    private PrerenderConfig prerenderConfig;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CustomConfig customConfig;

    private RestTemplate restTemplate;

    private static final Logger LOGGER = LoggerFactory.getLogger(RendertronInterceptor.class);

    @PostConstruct
    public void pc() {
        restTemplate = new RestTemplate();
    }


    private List<String> getCrawlerUserAgents() {
        List<String> crawlerUserAgents = new ArrayList<String>(Arrays.asList("baiduspider",
                "facebookexternalhit", "twitterbot", "rogerbot", "linkedinbot", "embedly", "quora link preview",
                "showyoubo", "outbrain", "pinterest", "developers.google.com/+/web/snippet", "slackbot", "vkShare",
                "W3C_Validator", "redditbot", "Applebot", "yandex", "Googlebot"));
        final String crawlerUserAgentsFromConfig = prerenderConfig.getCrawlerUserAgents();
        if (!isEmpty(crawlerUserAgentsFromConfig)) {
            crawlerUserAgents.addAll(Arrays.asList(crawlerUserAgentsFromConfig.trim().split(",")));
        }

        return crawlerUserAgents;
    }

    private List<String> getExtensionsToIgnore() {
        List<String> extensionsToIgnore = new ArrayList<String>(Arrays.asList(".js", ".json", ".css", ".xml", ".less", ".png", ".jpg",
                ".jpeg", ".gif", ".pdf", ".doc", ".txt", ".ico", ".rss", ".zip", ".mp3", ".rar", ".exe", ".wmv",
                ".doc", ".avi", ".ppt", ".mpg", ".mpeg", ".tif", ".wav", ".mov", ".psd", ".ai", ".xls", ".mp4",
                ".m4a", ".swf", ".dat", ".dmg", ".iso", ".flv", ".m4v", ".torrent", ".woff", ".ttf"));
        final String extensionsToIgnoreFromConfig = prerenderConfig.getIgnoreExtensions();
        if (!isEmpty(extensionsToIgnoreFromConfig)) {
            extensionsToIgnore.addAll(Arrays.asList(extensionsToIgnoreFromConfig.trim().split(",")));
        }

        return extensionsToIgnore;
    }





    private boolean isInSearchUserAgent(final String userAgent) {
        if (userAgent == null){ return false;}
        for(String item: getCrawlerUserAgents()){
            if (userAgent.toLowerCase().contains(item.toLowerCase())){
                return true;
            }
        }
        return false;
    }


    private boolean isInResources(final String url) {
        for(String item: getExtensionsToIgnore()){
            if ((url.indexOf('?') >= 0 ? url.substring(0, url.indexOf('?')) : url)
                    .toLowerCase().endsWith(item)){
                return true;
            }
        }
        return false;
    }

    private String getRedisKeyHtml(HttpServletRequest clientRequest) {
        return "rendertron_html_" + performReplacements(clientRequest.getRequestURI()) + nullToEmpty(clientRequest.getQueryString());
    }

    private String nullToEmpty(String s){
        if (s == null) {
            return "";
        } else {
            return s;
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        final String userAgent = request.getHeader("User-Agent");
        final String url = request.getRequestURL().toString();

        if (isInSearchUserAgent(userAgent) && !isInResources(url)) {
            final String key = getRedisKeyHtml(request);

            String value = redisTemplate.opsForValue().get(key);
            if (value==null) {
                final String rendertronUrl = prerenderConfig.getPrerenderServiceUrl()
                        + customConfig.getBaseUrl() + performReplacements(request.getRequestURI()) + getQuery(request);

                LOGGER.info("Requesting {} from rendertron", rendertronUrl);
                final ResponseEntity<String> re = restTemplate.getForEntity(rendertronUrl, String.class);
                value = re.getBody();

                redisTemplate.opsForValue().set(key, value);
                redisTemplate.expire(key, prerenderConfig.getCacheExpire(), prerenderConfig.getCacheExpireTimeUnit());
            }
            response.setHeader("Content-Type", "text/html; charset=utf-8");
            response.getWriter().print(value);
            return false;
        }

        return true;
    }

    private String getQuery(HttpServletRequest request) {
        if (request.getQueryString() == null) {
            return "";
        } else {
            return "?" + request.getQueryString();
        }
    }

    private String performReplacements(String s) {
        return s.replaceFirst("/index.html", "/");
    }
}
